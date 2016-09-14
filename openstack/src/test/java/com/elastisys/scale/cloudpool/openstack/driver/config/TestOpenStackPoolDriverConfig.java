package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV3Credentials;
import com.elastisys.scale.commons.openstack.Scope;

public class TestOpenStackPoolDriverConfig {

    @Test
    public void creation() {
        // explicit floating IP assignment
        AuthConfig auth = new AuthConfig("https://keystone.host:5000/v3/", null,
                new AuthV3Credentials(new Scope("domain_id", null), "user_id", "pass"));
        String region = "RegionOne";
        boolean assignFloatingIp = false;
        OpenStackPoolDriverConfig config = new OpenStackPoolDriverConfig(auth, region, null, assignFloatingIp);
        assertThat(config.getAuth(), is(auth));
        assertThat(config.getRegion(), is(region));
        assertThat(config.isAssignFloatingIp(), is(assignFloatingIp));

        // explicit connection timeouts
        int connectionTimeout = 5000;
        int socketTimeout = 7000;
        config = new OpenStackPoolDriverConfig(auth, region, null, assignFloatingIp, connectionTimeout, socketTimeout);
        assertThat(config.getConnectionTimeout(), is(connectionTimeout));
        assertThat(config.getSocketTimeout(), is(socketTimeout));

        // default floating IP assignment (true)
        config = new OpenStackPoolDriverConfig(auth, region, null, null);
        assertThat(config.getAuth(), is(auth));
        assertThat(config.getRegion(), is(region));
        assertThat(config.isAssignFloatingIp(), is(true));
        // default connection timeouts
        assertThat(config.getConnectionTimeout(), is(OpenStackPoolDriverConfig.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(OpenStackPoolDriverConfig.DEFAULT_SOCKET_TIMEOUT));

        // explicit networks
        config = new OpenStackPoolDriverConfig(auth, region, Arrays.asList("private"), null);
        assertThat(config.getNetworks(), is(Arrays.asList("private")));
    }

    /** Config must specify authentication details. */
    @Test(expected = IllegalArgumentException.class)
    public void missingAuth() {
        AuthConfig auth = null;
        String region = "RegionOne";
        new OpenStackPoolDriverConfig(auth, region, null, null);
    }

    /** Config must specify region to operate against. */
    @Test(expected = IllegalArgumentException.class)
    public void missingRegion() {
        AuthConfig auth = new AuthConfig("https://keystone.host:5000/v3/", null,
                new AuthV3Credentials(new Scope("domain_id", null), "user_id", "pass"));
        String region = null;
        new OpenStackPoolDriverConfig(auth, region, null, null);
    }

    /**
     * Connection timeout must be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalConnectionTimeout() {
        AuthConfig authConfig = new AuthConfig("https://keystone.host:5000/v3/", null,
                new AuthV3Credentials(new Scope("domain_id", null), "user_id", "pass"));
        new OpenStackPoolDriverConfig(authConfig, "region", null, true, 0, 10000).validate();
    }

    /**
     * Socket timeout must be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalSocketTimeout() {
        AuthConfig authConfig = new AuthConfig("https://keystone.host:5000/v3/", null,
                new AuthV3Credentials(new Scope("domain_id", null), "user_id", "pass"));
        new OpenStackPoolDriverConfig(authConfig, "region", null, true, 10000, -1).validate();
    }

}
