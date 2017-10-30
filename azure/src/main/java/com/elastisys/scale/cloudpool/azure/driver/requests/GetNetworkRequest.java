package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
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
     * @param resourceGroup
     *            The resource group under which the network is assumed to
     *            exist.
     * @param networkName
     *            The name of the virtual network to get.
     */
    public GetNetworkRequest(AzureApiAccess apiAccess, String resourceGroup, String networkName) {
        super(apiAccess);
        this.networkName = networkName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public Network doRequest(Azure api) throws NotFoundException, AzureException {
        Network network;
        try {
            LOG.debug("retrieving network {} ...", this.networkName);
            network = api.networks().getByResourceGroup(this.resourceGroup, this.networkName);
        } catch (Exception e) {
            throw new AzureException("failed to get network: " + e.getMessage(), e);
        }

        return Optional.ofNullable(network).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("virtual network not found: resource group: %s, name: %s",
                this.resourceGroup, this.networkName));
    }

}
