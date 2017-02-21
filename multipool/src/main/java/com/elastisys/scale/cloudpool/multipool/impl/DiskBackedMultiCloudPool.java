package com.elastisys.scale.cloudpool.multipool.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolCreateException;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolDeleteException;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A {@link MultiCloudPool} that stores the state of its
 * {@link CloudPoolInstance}s to disk, to allow recovery on restart.
 */
public class DiskBackedMultiCloudPool implements MultiCloudPool {
    static final Logger LOG = LoggerFactory.getLogger(DiskBackedMultiCloudPool.class);

    public static final Pattern VALID_CLOUDPOOL_NAME = Pattern.compile("[A-Za-z0-9_\\-\\.]+");

    /**
     * Maximum time (in seconds) to wait for all {@link CloudPoolInstance}
     * restore tasks to complete.
     */
    private static final long MAX_RESTORE_WAIT = 180;

    /**
     * Storage directory under which a separate directory is created for each
     * {@link CloudPoolInstance} to store its state.
     */
    private final File storageDir;
    /** {@link CloudPool} instance factory. */
    private final CloudPoolFactory factory;

    /**
     * Registry of all {@link DiskBackedCloudPoolInstance}s. Keys are
     * {@link CloudPool} names, values are {@link DiskBackedCloudPoolInstance}s.
     */
    private final Map<String, CloudPoolInstance> instances;

    /**
     * Creates a {@link DiskBackedCloudPoolInstance} and restore any
     * {@link CloudPoolInstance}s from the storage diectory.
     *
     * @param storageDir
     *            Storage directory where {@link CloudPool} instance state is
     *            stored.
     * @param factory
     *            {@link CloudPool} instance factory.
     * @throws IOException
     * @throws InterruptedException
     */
    public DiskBackedMultiCloudPool(File storageDir, CloudPoolFactory factory)
            throws IOException, InstanceRestoreException {
        checkArgument(storageDir != null, "storageDir cannot be null");
        checkArgument(factory != null, "cloudpool factory cannot be null");
        prepareStorageDir(storageDir);

        this.storageDir = storageDir;
        this.factory = factory;
        this.instances = new ConcurrentHashMap<>();

        restoreInstances();
    }

    @Override
    public CloudPoolInstance create(String cloudPoolName) throws IllegalArgumentException, CloudPoolCreateException {
        checkArgument(VALID_CLOUDPOOL_NAME.matcher(cloudPoolName).matches(),
                "invalid cloudpool name '%s': may only contain characters [A-Za-z_0-9-.]", cloudPoolName);
        checkArgument(!this.instances.containsKey(cloudPoolName), "cloud pool instance already exists: %s",
                cloudPoolName);

        try {
            File stateDir = new File(this.storageDir, cloudPoolName);
            CloudPool cloudPool = this.factory.create(new CloudPoolThreadFactory(cloudPoolName), stateDir);
            DiskBackedCloudPoolInstance instance = new DiskBackedCloudPoolInstance(cloudPool, stateDir);
            this.instances.put(cloudPoolName, instance);
            return instance;
        } catch (Exception e) {
            throw new CloudPoolCreateException("failed to create cloudpool instance: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String cloudPoolName) throws NotFoundException, CloudPoolDeleteException {
        // make sure it exists
        get(cloudPoolName);

        this.instances.remove(cloudPoolName);

        // delete instance directory
        try {
            FileUtils.deleteRecursively(new File(this.storageDir, cloudPoolName));
        } catch (IOException e) {
            throw new CloudPoolDeleteException("failed to delete cloudpool instance: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> list() {
        List<String> names = this.instances.values().stream().map(CloudPoolInstance::name).collect(Collectors.toList());
        Collections.sort(names);
        return names;
    }

    @Override
    public CloudPoolInstance get(String cloudPoolName) throws NotFoundException {
        if (!this.instances.containsKey(cloudPoolName)) {
            throw new NotFoundException("no such cloudpool instance: " + cloudPoolName);
        }
        return this.instances.get(cloudPoolName);
    }

    private void prepareStorageDir(File storageDir) throws IOException {
        try {
            // it is okay for directory to already exist
            Files.createDirectories(storageDir.toPath());
            checkArgument(FileUtils.canWriteTo(storageDir),
                    "not permitted to write to storageDir: " + storageDir.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(String.format("failed to prepare storageDir %s: %s: %s", storageDir.getAbsolutePath(),
                    e.getClass().getName(), e.getMessage()), e);
        }
    }

    private void restoreInstances() throws InstanceRestoreException {
        LOG.debug("restoring cloudpool instances from {} ...", this.storageDir.getAbsolutePath());

        // restore instances in parallel to speed up the process in case there
        // are many instances to recover
        ExecutorService executor = Executors.newFixedThreadPool(20,
                new ThreadFactoryBuilder().setNameFormat("restore-instances-%d").build());
        try {
            List<File> instanceDirs = FileUtils.listDirectories(this.storageDir);

            List<Future<CloudPoolInstance>> restoreTasks = new ArrayList<>();
            for (File instanceDir : instanceDirs) {
                restoreTasks.add(executor.submit(new InstanceRestoreTask(this.factory, instanceDir)));
            }

            for (Future<CloudPoolInstance> restoreTask : restoreTasks) {
                CloudPoolInstance instance = restoreTask.get();
                this.instances.put(instance.name(), instance);
            }
        } catch (Exception e) {
            throw new InstanceRestoreException(String.format("failed to restore all cloudpool instances under %s: %s",
                    this.storageDir.getAbsolutePath(), e.getMessage()), e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * A {@link Callable} that, when called, restores a
     * {@link CloudPoolInstance} from a given instance directory.
     */
    private static class InstanceRestoreTask implements Callable<CloudPoolInstance> {

        private final CloudPoolFactory factory;
        private final File instanceDir;

        public InstanceRestoreTask(CloudPoolFactory factory, File instanceDir) {
            this.factory = factory;
            this.instanceDir = instanceDir;
        }

        @Override
        public CloudPoolInstance call() throws InstanceRestoreException {
            try {
                CloudPool cloudPool = this.factory.create(new CloudPoolThreadFactory(this.instanceDir.getName()),
                        this.instanceDir);
                DiskBackedCloudPoolInstance instance = new DiskBackedCloudPoolInstance(cloudPool, this.instanceDir);
                instance.restore();
                return instance;
            } catch (Exception e) {
                throw new InstanceRestoreException("failed to restore cloudpool instance from " + this.instanceDir, e);
            }
        }

    }
}
