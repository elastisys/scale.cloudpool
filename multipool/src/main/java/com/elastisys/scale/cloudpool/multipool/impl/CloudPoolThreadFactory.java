package com.elastisys.scale.cloudpool.multipool.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.MDC;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.multipool.logging.LogConstants;

/**
 * A {@link ThreadFactory} intended to create {@link Thread}s for a particular
 * {@link CloudPool}. Every created thread will be given a name of
 * {@code <cloudPoolName>-<counter>} and will have a
 * <a href="https://logback.qos.ch/manual/mdc.html">logging context</a> property
 * set to distinguish its log output from that of other {@link CloudPool}s.
 * <p/>
 * More specifically, a {@code cloudpool} MDC property is set which, for
 * example, can be used in a layout pattern via <code>%X{cloudpool}</code> to
 * show which {@link CloudPool} instance produced a given log entry.
 *
 */
class CloudPoolThreadFactory implements ThreadFactory {
    private final String cloudPoolName;
    private final AtomicLong counter = new AtomicLong(0);

    public CloudPoolThreadFactory(String cloudPoolName) {
        this.cloudPoolName = cloudPoolName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        final String poolName = this.cloudPoolName;
        Thread thread = new Thread(runnable, poolName + "-" + this.counter.getAndIncrement()) {
            @Override
            public void run() {
                MDC.put(LogConstants.POOL_INSTANCE_MDC_PROPERTY, poolName);
                super.run();
            }
        };
        thread.setDaemon(true);
        return thread;
    }
}