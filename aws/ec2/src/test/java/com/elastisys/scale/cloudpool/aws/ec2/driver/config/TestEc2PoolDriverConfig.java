package com.elastisys.scale.cloudpool.aws.ec2.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.aws.ec2.driver.config.Ec2PoolDriverConfig;

/**
 * Exercises the {@link Ec2PoolDriverConfig} class.
 */
public class TestEc2PoolDriverConfig {

    /**
     * Creation that sets all config parameters.
     */
    @Test
    public void fullConfig() {
        int connectionTimeout = 7000;
        int socketTimeout = 5000;
        Ec2PoolDriverConfig config = new Ec2PoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", "us-east-1",
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
        Ec2PoolDriverConfig config = new Ec2PoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", "us-east-1");
        config.validate();

        // verify default values
        assertThat(config.getConnectionTimeout(), is(Ec2PoolDriverConfig.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(Ec2PoolDriverConfig.DEFAULT_SOCKET_TIMEOUT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingAwsAccessKeyId() {
        new Ec2PoolDriverConfig(null, "awsSecretAccessKey", "us-east-1").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingAwsSecretAccessKey() {
        new Ec2PoolDriverConfig("awsAccessKeyId", null, "us-east-1").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingRegion() {
        new Ec2PoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", null).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalConnectionTimeout() {
        new Ec2PoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", "us-east-1", 0, 5000).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalSocketTimeout() {
        new Ec2PoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", "us-east-1", 5000, 0).validate();
    }

}
