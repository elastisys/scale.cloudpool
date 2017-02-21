package com.elastisys.scale.cloudpool.multipool.api;

import com.elastisys.scale.cloudpool.api.CloudPool;

/**
 * Represents a {@link CloudPool} instance in a {@link MultiCloudPool}.
 */
public interface CloudPoolInstance extends CloudPool {

    /**
     * Returns the name that was assigned on creation to the
     * {@link CloudPoolInstance}. This is the name under which the
     * {@link CloudPoolInstance} will have its base URL:
     * {@code /cloudpools/<name>}.
     *
     * @return
     */
    String name();

}
