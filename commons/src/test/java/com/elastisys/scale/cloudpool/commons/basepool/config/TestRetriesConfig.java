package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercise {@link RetriesConfig}.
 */
public class TestRetriesConfig {

    @Test
    public void basicSanity() {
        int maxRetries = 2;
        TimeInterval initialBackoffDelay = new TimeInterval(10L, TimeUnit.SECONDS);
        RetriesConfig config = new RetriesConfig(maxRetries, initialBackoffDelay);

        config.validate();

        assertThat(config.getMaxRetries(), is(maxRetries));
        assertThat(config.getInitialBackoffDelay(), is(initialBackoffDelay));
    }

    /**
     * It should be possible to give an initial backoff delay of 0, which should
     * result in no delay between retries.
     */
    @Test
    public void withNoExponentialBackoff() {
        int maxRetries = 3;
        TimeInterval initialBackoffDelay = new TimeInterval(0L, TimeUnit.SECONDS);
        RetriesConfig config = new RetriesConfig(maxRetries, initialBackoffDelay);
        assertThat(config.getInitialBackoffDelay(), is(new TimeInterval(0L, TimeUnit.SECONDS)));
    }

    /**
     * Should be possible to have no retries.
     */
    @Test
    public void zeroRetries() {
        new RetriesConfig(0, new TimeInterval(0L, TimeUnit.SECONDS)).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeMaxRetries() {
        new RetriesConfig(-1, new TimeInterval(0L, TimeUnit.SECONDS)).validate();
    }

}
