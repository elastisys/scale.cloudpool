package com.elastisys.scale.cloudpool.commons.basepool;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl.CachingPoolFetcher;
import com.google.common.collect.ImmutableSet;

/**
 * Declares where runtime state is stored during execution, and where runtime
 * state is recovered from when the {@link BaseCloudPool} process is restarted.
 */
public class StateStorage {
    public static final String DEFAULT_CACHED_MACHINE_POOL_FILENAME = "cached_machine_pool.json";

    /**
     * File where the {@link CachingPoolFetcher} stores its machine pool cache.
     */
    private final File cachedMachinePoolFile;

    /**
     * Creates a {@link StateStorage} instance.
     *
     * @param cachedMachinePoolFile
     *            File where the {@link CachingPoolFetcher} stores its machine
     *            pool cache.
     */
    private StateStorage(File cachedMachinePoolFile) {
        this.cachedMachinePoolFile = cachedMachinePoolFile;
    }

    /**
     * File where the {@link CachingPoolFetcher} stores its machine pool cache.
     *
     * @return
     */
    public File getCachedMachinePoolFile() {
        return this.cachedMachinePoolFile;
    }

    public static StateStorageBuilder builder(String storageDir) {
        return builder(new File(storageDir));
    }

    public static StateStorageBuilder builder(File storageDir) {
        return new StateStorageBuilder(storageDir);
    }

    /**
     * Builds {@link StateStorage} instances with files relative to a given
     * storage directory.
     */
    public static class StateStorageBuilder {
        private final File storageDir;

        private String cachedMachinePoolFileName = DEFAULT_CACHED_MACHINE_POOL_FILENAME;

        public StateStorageBuilder(File storageDir) {
            checkArgument(storageDir != null, "storageDir cannot be null");
            this.storageDir = storageDir;
        }

        public StateStorage build() {
            List<String> allFileNames = Arrays.asList(this.cachedMachinePoolFileName);
            Set<String> uniqueFileNames = ImmutableSet.copyOf(allFileNames);
            checkArgument(allFileNames.size() == uniqueFileNames.size(),
                    "all StateStorage file names need to be unique");

            File cachedMachinePoolFile = new File(this.storageDir, this.cachedMachinePoolFileName);
            return new StateStorage(cachedMachinePoolFile);
        }

        /**
         * The name of the tile where the {@link CachingPoolFetcher} stores its
         * machine pool cache.
         *
         * @param fileName
         * @return
         */
        public StateStorageBuilder withCachedMachinePoolFileName(String fileName) {
            checkArgument(fileName != null, "fileName cannot be null");
            this.cachedMachinePoolFileName = fileName;
            return this;
        }
    }
}
