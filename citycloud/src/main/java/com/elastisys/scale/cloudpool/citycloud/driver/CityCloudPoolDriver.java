package com.elastisys.scale.cloudpool.citycloud.driver;

import java.util.Arrays;
import java.util.List;

import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;

/**
 * A {@link CloudPoolDriver} implementation that operates against CityCloud's
 * OpenStack API.
 *
 * @see BaseCloudPool
 */
public class CityCloudPoolDriver extends OpenStackPoolDriver {

    /**
     * Supported API versions by this implementation.
     */
    private final static List<String> supportedApiVersions = Arrays.asList(ApiVersion.LATEST);
    /**
     * Cloud pool metadata for this implementation.
     */
    private final static CloudPoolMetadata cloudPoolMetadata = new CloudPoolMetadata(CloudProviders.CITYCLOUD,
            supportedApiVersions);

    /**
     * Creates a new {@link CityCloudPoolDriver}. Needs to be configured before
     * use.
     *
     * @param client
     *            The client to be used to communicate with the OpenStack API.
     */
    public CityCloudPoolDriver(OpenstackClient client) {
        super(client);
    }

    @Override
    public CloudPoolMetadata getMetadata() {
        return cloudPoolMetadata;
    }

}
