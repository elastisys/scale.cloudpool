package com.elastisys.scale.cloudpool.aws.ec2.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercises the {@link CloudApiSettings} class.
 */
public class TestCloudApiSettings {
    /** Sample AWS access key id. */
    private static final String ACCESS_KEY_ID = "awsAccessKeyId";
    /** Sample AWS secret access key. */
    private static final String SECRET_ACCESS_KEY = "awsSecretAccessKey";
    /** Sample region */
    private static final String REGION = "us-east-1";

    /** Sample conneciton timeout. */
    private static final int CONN_TIMEOUT = 7000;
    /** Sample socket timeout. */
    private static final int SOCK_TIMEOUT = 7000;

    /**
     * Creation that sets all config parameters.
     */
    @Test
    public void completeConfig() {
        CloudApiSettings config = new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, CONN_TIMEOUT,
                SOCK_TIMEOUT);
        config.validate();

        // verify
        assertThat(config.getAwsAccessKeyId(), is(ACCESS_KEY_ID));
        assertThat(config.getAwsSecretAccessKey(), is(SECRET_ACCESS_KEY));
        assertThat(config.getRegion(), is(REGION));
        assertThat(config.getConnectionTimeout(), is(CONN_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(SOCK_TIMEOUT));
    }

    /**
     * Connection timeout and socket timeout are optional.
     */
    @Test
    public void withDefaults() {
        CloudApiSettings config = new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION);

        // verify default values
        assertThat(config.getConnectionTimeout(), is(CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(CloudApiSettings.DEFAULT_SOCKET_TIMEOUT));
    }

    @Test
    public void missingAwsAccessKeyId() {
        try {
            new CloudApiSettings(null, SECRET_ACCESS_KEY, REGION).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsAccessKeyId"));
        }
    }

    @Test
    public void missingAwsSecretAccessKey() {
        try {
            new CloudApiSettings(ACCESS_KEY_ID, null, REGION).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsSecretAccessKey"));
        }

    }

    @Test
    public void missingRegion() {
        try {
            new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("region"));
        }

    }

    @Test
    public void illegalConnectionTimeout() {
        try {
            new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, 0, SOCK_TIMEOUT).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("connectionTimeout"));
        }

    }

    @Test
    public void illegalSocketTimeout() {
        try {
            new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, CONN_TIMEOUT, 0).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("socketTimeout"));
        }

    }

}
