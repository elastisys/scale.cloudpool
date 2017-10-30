package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;

/**
 * An Azure request that, when called, fetches meta data about a particular
 * network security group in a given resource group.
 */
public class GetNetworkSecurityGroupRequest extends AzureRequest<NetworkSecurityGroup> {

    /** The Azure resource group that contains the security group. */
    private final String resourceGroup;
    /** The name of the security group to retrieve. */
    private final String securityGroup;

    /**
     * Creates a {@link GetNetworkSecurityGroupRequest} for a particular region.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The Azure resource group that contains the security group.
     * @param securityGroup
     *            The name of the security group to retrieve.
     */
    public GetNetworkSecurityGroupRequest(AzureApiAccess apiAccess, String resourceGroup, String securityGroup) {
        super(apiAccess);
        this.resourceGroup = resourceGroup;
        this.securityGroup = securityGroup;
    }

    @Override
    public NetworkSecurityGroup doRequest(Azure api) throws NotFoundException, AzureException {
        NetworkSecurityGroup nsg;
        try {
            LOG.debug("retrieving security group {} ...", this.securityGroup);
            nsg = api.networkSecurityGroups().getByResourceGroup(this.resourceGroup, this.securityGroup);
        } catch (Exception e) {
            throw new AzureException("failed to get network security group: " + e.getMessage(), e);
        }

        return Optional.ofNullable(nsg).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("security group not found: resourceGroup: %s, name: %s",
                this.resourceGroup, this.securityGroup));
    }

}
