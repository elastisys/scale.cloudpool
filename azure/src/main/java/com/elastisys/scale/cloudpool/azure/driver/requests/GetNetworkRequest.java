package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;

/**
 * An Azure request that, when executed, retrieves metadata about a virtual
 * network.
 */
public class GetNetworkRequest extends AzureRequest<Network> {
    /** The name of the virtual network to get. */
    private final String networkName;

    /** The resource group under which the network is assumed to exist. */
    private final String resourceGroup;

    /**
     * Creates a {@link GetNetworkRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param networkName
     *            The name of the virtual network to get.
     * @param resourceGroup
     *            The resource group under which the network is assumed to
     *            exist.
     */
    public GetNetworkRequest(AzureApiAccess apiAccess, String networkName, String resourceGroup) {
        super(apiAccess);
        this.networkName = networkName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public Network doRequest(Azure api) throws NotFoundException, CloudException {
        try {
            LOG.debug("retrieving network {} ...", this.networkName);
            return api.networks().getByGroup(this.resourceGroup, this.networkName);
        } catch (CloudException e) {
            if (e.body().code().equals("ResourceNotFound")) {
                throw new NotFoundException("no such network: " + this.networkName, e);
            }
            throw e;
        }
    }

}
