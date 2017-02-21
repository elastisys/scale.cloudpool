package com.elastisys.scale.cloudpool.multipool.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;

/**
 * Exercises {@link DiskBackedMultiCloudPool}.
 */
public class TestDiskBackedMultiCloudPool {

    private static final File storageDir = new File("target/multipool/cloudpools");
    private static final File instance1Dir = new File(storageDir, "pool1");
    private static final File instance2Dir = new File(storageDir, "pool2");

    private CloudPoolFactory mockedFactory = mock(CloudPoolFactory.class);

    /** Object under test. */
    private DiskBackedMultiCloudPool multiCloudPool;

    @Before
    public void beforeTestMethod() throws IOException {
        FileUtils.deleteRecursively(storageDir);
        this.multiCloudPool = new DiskBackedMultiCloudPool(storageDir, this.mockedFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullStorageDir() throws IOException {
        File nullStorageDir = null;
        new DiskBackedMultiCloudPool(nullStorageDir, this.mockedFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullFactory() throws IOException {
        CloudPoolFactory nullFactory = null;
        new DiskBackedMultiCloudPool(storageDir, nullFactory);
    }

    /**
     * On creation, the {@link DiskBackedMultiCloudPool} should create its
     * storage directory.
     */
    @Test
    public void createStorageDirectoryOnInit() throws IOException {
        FileUtils.deleteRecursively(storageDir);
        assertThat(storageDir.exists(), is(false));
        new DiskBackedMultiCloudPool(storageDir, this.mockedFactory);
        assertThat(storageDir.isDirectory(), is(true));
    }

    /**
     * Must fail if the storage directory cannot be written.
     */
    @Test(expected = IOException.class)
    public void createWithUnwritableStorageDirectory() throws IOException {
        File notWritableDir = new File("/root/mypool");
        assertThat(notWritableDir.exists(), is(false));
        new DiskBackedMultiCloudPool(notWritableDir, this.mockedFactory);
    }

    /**
     * Any {@link CloudPool} instances with saved state should be restored on
     * creation of a {@link DiskBackedMultiCloudPool}.
     */
    @Test
    public void restoreInstancesOnInit() throws Exception {
        createInstanceDir(instance1Dir, asJson("{\"pool\": 1}"), true);
        createInstanceDir(instance2Dir, asJson("{\"pool\": 2}"), false);

        // prepare factory mock to be asked to create instances
        CloudPool pool1 = mock(CloudPool.class);
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(instance1Dir)))).thenReturn(pool1);
        CloudPool pool2 = mock(CloudPool.class);
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(instance2Dir)))).thenReturn(pool2);

        DiskBackedMultiCloudPool multiCloudPool = new DiskBackedMultiCloudPool(storageDir, this.mockedFactory);

        // verify that pool1 had its config set and was started
        verify(pool1).configure(asJson("{\"pool\": 1}"));
        verify(pool1).start();
        // verify that pool2 had its config set and was not started
        verify(pool2).configure(asJson("{\"pool\": 2}"));
        verify(pool2).stop();

        assertTrue(multiCloudPool.list().containsAll(asList("pool1", "pool2")));
        assertThat(multiCloudPool.get("pool1").name(), is("pool1"));
        assertThat(multiCloudPool.get("pool2").name(), is("pool2"));
    }

    /**
     * On create, the {@link CloudPoolFactory} should be called and the created
     * instance should be added to the collection of instances.
     */
    @Test
    public void create() {
        // prepare factory mock to be asked to create an instance
        CloudPool mockedCloudPool = mock(CloudPool.class);
        File expectedStateDir = new File(storageDir, "my-pool-1");
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(expectedStateDir)))).thenReturn(mockedCloudPool);

        CloudPoolInstance instance = this.multiCloudPool.create("my-pool-1");
        assertThat(instance, is(notNullValue()));
        assertThat(instance.name(), is("my-pool-1"));

        // verify that factory was called
        verify(this.mockedFactory).create(anyThreadFactory(), argThat(is(expectedStateDir)));

        // verify that instance was added to collection
        assertTrue(this.multiCloudPool.list().contains("my-pool-1"));
        assertThat(this.multiCloudPool.get("my-pool-1"), is(instance));
    }

    /**
     * Only [A-Za-z0-9_.] are valid pool name characters.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithIllegalName() {
        this.multiCloudPool.create("my-pool/1");
    }

    /**
     * It should not be possible to create a {@link CloudPool} instance with a
     * name that already exists.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithTakenCloudPoolName() throws IOException {
        // prepare a multi cloud pool with an existing instance
        createInstanceDir(instance1Dir, asJson("{\"pool\": 1}"), true);
        CloudPool pool1 = mock(CloudPool.class);
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(instance1Dir)))).thenReturn(pool1);
        DiskBackedMultiCloudPool multiCloudPool = new DiskBackedMultiCloudPool(storageDir, this.mockedFactory);

        multiCloudPool.create("pool1");
    }

    /**
     * Deleting a {@link CloudPool} instance should remove the instance state
     * directory as well.
     */
    @Test
    public void delete() throws IOException {
        // prepare a couple of instances
        createInstanceDir(instance1Dir, asJson("{\"pool\": 1}"), true);
        createInstanceDir(instance2Dir, asJson("{\"pool\": 2}"), false);
        CloudPool pool1 = mock(CloudPool.class);
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(instance1Dir)))).thenReturn(pool1);
        CloudPool pool2 = mock(CloudPool.class);
        when(this.mockedFactory.create(anyThreadFactory(), argThat(is(instance2Dir)))).thenReturn(pool2);

        DiskBackedMultiCloudPool multiCloudPool = new DiskBackedMultiCloudPool(storageDir, this.mockedFactory);

        assertTrue(instance1Dir.exists());
        assertTrue(instance2Dir.exists());
        assertTrue(multiCloudPool.list().containsAll(asList("pool1", "pool2")));
        assertThat(multiCloudPool.get("pool1"), is(notNullValue()));

        multiCloudPool.delete("pool1");
        // should remove instance dir
        assertFalse(instance1Dir.exists());
        assertFalse(multiCloudPool.list().contains("pool1"));

        multiCloudPool.delete("pool2");
        assertFalse(instance2Dir.exists());
        assertFalse(multiCloudPool.list().contains("pool2"));
    }

    @Test(expected = NotFoundException.class)
    public void deleteNonExistentPool() {
        this.multiCloudPool.delete("pool1");
    }

    private static void createInstanceDir(File instanceDir, JsonObject config, boolean started) throws IOException {
        Files.createDirectories(instanceDir.toPath());
        Files.write(new File(instanceDir, DiskBackedCloudPoolInstance.CONFIG_FILE).toPath(),
                JsonUtils.toString(config).getBytes());
        Files.write(new File(instanceDir, DiskBackedCloudPoolInstance.STATUS_FILE).toPath(),
                JsonUtils.toString(JsonUtils.toJson(new CloudPoolStatus(started, true))).getBytes());
    }

    private static ThreadFactory anyThreadFactory() {
        return argThat(is(any(ThreadFactory.class)));
    }

    private static JsonObject asJson(String jsonString) {
        return JsonUtils.parseJsonString(jsonString).getAsJsonObject();
    }

}
