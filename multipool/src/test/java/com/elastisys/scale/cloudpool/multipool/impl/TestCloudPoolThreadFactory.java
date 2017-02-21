package com.elastisys.scale.cloudpool.multipool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;
import org.slf4j.MDC;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.multipool.logging.LogConstants;

/**
 * Exercises {@link CloudPoolThreadFactory}.
 */
public class TestCloudPoolThreadFactory {

    /**
     * Threads created by the {@link CloudPoolThreadFactory} should have a MDC
     * logging property set to the name of the {@link CloudPool} to allow log
     * entries from different cloud pool instances to be distinguished from one
     * another.
     */
    @Test
    public void shouldScopeLoggingToCloudpool() throws InterruptedException {
        ThreadFactory factory = new CloudPoolThreadFactory("my-pool");

        assertThat(MDC.get(LogConstants.POOL_INSTANCE_MDC_PROPERTY), is(nullValue()));

        Thread thread = factory.newThread(() -> {
            // the created thread should have a logging property set for the
            // cloud pool name
            assertThat(MDC.get(LogConstants.POOL_INSTANCE_MDC_PROPERTY), is("my-pool"));
        });
        thread.start();
        thread.join();
    }

    /**
     * Threads created by a {@link CloudPoolThreadFactory} should be named
     * {@code <cloudPoolName>-<sequencenum>}.
     */
    @Test
    public void shouldCreateThreadsNamedAfterCloudPool() {
        ThreadFactory factory = new CloudPoolThreadFactory("my-pool");
        Thread thread1 = factory.newThread(() -> {
        });
        Thread thread2 = factory.newThread(() -> {
        });
        Thread thread3 = factory.newThread(() -> {
        });

        assertThat(thread1.getName(), is("my-pool-0"));
        assertThat(thread2.getName(), is("my-pool-1"));
        assertThat(thread3.getName(), is("my-pool-2"));
    }

    /**
     * Threads created by a {@link CloudPoolThreadFactory} should be daemon
     * threads.
     */
    @Test
    public void shouldCreateDaemonThreads() {
        ThreadFactory factory = new CloudPoolThreadFactory("my-pool");
        Thread thread1 = factory.newThread(() -> {
        });

        assertThat(thread1.isDaemon(), is(true));
    }
}
