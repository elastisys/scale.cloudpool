package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.commons.openstack.ApiAccessConfig;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;
import com.elastisys.scale.commons.openstack.AuthV3Credentials;

/**
 * Exercise {@link CloudApiSettings}.
 */
public class TestCloudApiSettings {

    /**
     * Should be possible to set v2 auth credentials.
     */
    @Test
    public void withAuthV2Credentials() {
        AuthV2Credentials v2Credentials = authV2Credentials();
        AuthV3Credentials v3Credentials = null;

        AuthConfig auth = new AuthConfig(keystoneUrl(), v2Credentials, v3Credentials);
        String region = "RegionOne";
        CloudApiSettings config = new CloudApiSettings(auth, region);
        config.validate();

        assertThat(config.getAuth(), is(auth));
        assertThat(config.getRegion(), is(region));
    }

    /**
     * Should be possible to set v3 auth credentials.
     */
    @Test
    public void withAuthV3Credentials() {
        AuthV2Credentials v2Credentials = null;
        AuthV3Credentials v3Credentials = authV3Credentials();

        AuthConfig auth = new AuthConfig(keystoneUrl(), v2Credentials, v3Credentials);
        CloudApiSettings config = new CloudApiSettings(auth, "RegionOne");
        config.validate();

        assertThat(config.getAuth(), is(auth));
    }

    /**
     * If not given, default values are to be provided for connectionTimeout and
     * socketTimeout.
     */
    @Test
    public void withDefaults() {

        AuthConfig auth = new AuthConfig(keystoneUrl(), null, authV3Credentials());
        CloudApiSettings config = new CloudApiSettings(auth, "RegionOne");
        config.validate();

        assertThat(config.getConnectionTimeout(), is(ApiAccessConfig.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(ApiAccessConfig.DEFAULT_SOCKET_TIMEOUT));
        assertThat(config.shouldLogHttpRequests(), is(false));
    }

    @Test
    public void withExplicitTimeouts() {
        int connectionTimeout = 5000;
        int socketTimeout = 7000;
        CloudApiSettings config = new CloudApiSettings(new AuthConfig(keystoneUrl(), null, authV3Credentials()),
                "RegionOne", connectionTimeout, socketTimeout);
        config.validate();
        assertThat(config.getConnectionTimeout(), is(connectionTimeout));
        assertThat(config.getSocketTimeout(), is(socketTimeout));
    }

    @Test
    public void withRequestLogging() {
        int connectionTimeout = 5000;
        int socketTimeout = 7000;
        boolean logHttpRequests = true;
        CloudApiSettings config = new CloudApiSettings(new AuthConfig(keystoneUrl(), null, authV3Credentials()),
                "RegionOne", connectionTimeout, socketTimeout, logHttpRequests);
        config.validate();
        assertThat(config.getConnectionTimeout(), is(connectionTimeout));
        assertThat(config.getSocketTimeout(), is(socketTimeout));
        assertThat(config.shouldLogHttpRequests(), is(true));
    }

    /** Config must specify authentication details. */
    @Test(expected = IllegalArgumentException.class)
    public void withMissingAuth() {
        AuthConfig auth = null;
        String region = "RegionOne";
        new CloudApiSettings(auth, region, null, null).validate();
    }

    /** Config must specify region to operate against. */
    @Test(expected = IllegalArgumentException.class)
    public void withMissingRegion() {
        AuthConfig auth = new AuthConfig(keystoneUrl(), null, authV3Credentials());
        String region = null;
        new CloudApiSettings(auth, region, null, null).validate();
    }

    /**
     * Connection timeout must be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalConnectionTimeout() {
        AuthConfig authConfig = new AuthConfig(keystoneUrl(), null, authV3Credentials());
        new CloudApiSettings(authConfig, "region", 0, 10000).validate();
    }

    /**
     * Socket timeout must be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalSocketTimeout() {
        AuthConfig authConfig = new AuthConfig(keystoneUrl(), null, authV3Credentials());
        new CloudApiSettings(authConfig, "region", 10000, -1).validate();
    }

    private String keystoneUrl() {
        return "https://keystone.host:5000/v2/";
    }

    private AuthV2Credentials authV2Credentials() {
        return new AuthV2Credentials("tenantName", "userName", "password");
    }

    private AuthV3Credentials authV3Credentials() {
        String userId = null;
        String userName = "foo";
        String userDomainId = null;
        String userDomainName = "default";
        String password = "secret";
        String projectId = null;
        String projectName = "admin";
        String projectDomainName = "default";
        String projectDomainId = null;
        return new AuthV3Credentials(userId, userName, userDomainId, userDomainName, password, projectId, projectName,
                projectDomainName, projectDomainId);
    }
}
