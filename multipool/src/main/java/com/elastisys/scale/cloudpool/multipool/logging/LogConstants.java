package com.elastisys.scale.cloudpool.multipool.logging;

import com.elastisys.scale.cloudpool.api.CloudPool;

public class LogConstants {
    /**
     * <a href="https://logback.qos.ch/manual/mdc.html">MDC</a> property key
     * used to separate log output from different {@link CloudPool} instances.
     * This MDC property can be set for any thread serving a particular
     * {@link CloudPool} instance. The property can then be referenced in a
     * layout pattern via <code>%X{cloudpool}</code> to show which
     * {@link CloudPool} instance produced a given log entry.
     */
    public static final String POOL_INSTANCE_MDC_PROPERTY = "cloudpool";
}
