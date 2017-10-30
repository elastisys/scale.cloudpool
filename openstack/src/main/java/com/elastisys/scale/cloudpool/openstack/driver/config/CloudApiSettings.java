package com.elastisys.scale.cloudpool.openstack.driver.config;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.commons.openstack.ApiAccessConfig;
import com.elastisys.scale.commons.openstack.AuthConfig;

/**
 * Cloud API settings for an {@link OpenStackPoolDriver}.
 *
 * @see BaseCloudPoolConfig#getCloudApiSettings()
 */
public class CloudApiSettings extends ApiAccessConfig {

    public CloudApiSettings(AuthConfig auth, String region) {
        super(auth, region);
    }

    public CloudApiSettings(AuthConfig auth, String region, Integer connectionTimeout, Integer socketTimeout,
            boolean logHttpRequests) {
        super(auth, region, connectionTimeout, socketTimeout, logHttpRequests);
    }

    public CloudApiSettings(AuthConfig auth, String region, Integer connectionTimeout, Integer socketTimeout) {
        super(auth, region, connectionTimeout, socketTimeout);
    }

}
