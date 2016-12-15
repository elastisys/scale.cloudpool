package com.elastisys.scale.cloudpool.aws.autoscaling.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link CloudApiSettings} class.
 *
 */
public class TestCloudApiSettings {

    /**
     * Creation that sets all config parameters.
     */
    @Test
    public void fullConfig() {
        int connectionTimeout = 7000;
        int socketTimeout = 5000;
        CloudApiSettings config = new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", "us-east-1",
                connectionTimeout, socketTimeout);
        config.validate();

        // verify
        assertThat(config.getAwsAccessKeyId(), is("awsAccessKeyId"));
        assertThat(config.getAwsSecretAccessKey(), is("awsSecretAccessKey"));
        assertThat(config.getRegion(), is("us-east-1"));
        assertThat(config.getConnectionTimeout(), is(connectionTimeout));
        assertThat(config.getSocketTimeout(), is(socketTimeout));
    }

    /**
     * Creation that only sets mandatory config parameters.
     */
    @Test
    public void withDefaults() {
        CloudApiSettings config = new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", "us-east-1");
        config.validate();

        // verify default values
        assertThat(config.getConnectionTimeout(), is(CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(CloudApiSettings.DEFAULT_SOCKET_TIMEOUT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingAwsAccessKeyId() {
        new CloudApiSettings(null, "awsSecretAccessKey", "us-east-1").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingAwsSecretAccessKey() {
        new CloudApiSettings("awsAccessKeyId", null, "us-east-1").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingRegion() {
        new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", null).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalConnectionTimeout() {
        new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", "us-east-1", 0, 5000).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalSocketTimeout() {
        new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", "us-east-1", 5000, 0).validate();
    }

}
