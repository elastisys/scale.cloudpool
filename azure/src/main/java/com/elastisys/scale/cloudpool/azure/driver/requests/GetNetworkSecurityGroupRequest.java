package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.CloudException;
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
    public NetworkSecurityGroup doRequest(Azure api) throws RuntimeException {

        try {
            LOG.debug("retrieving security group {} ...", this.securityGroup);
            return api.networkSecurityGroups().getByResourceGroup(this.resourceGroup, this.securityGroup);
        } catch (CloudException e) {
            if (e.body().code().equals("ResourceNotFound")) {
                throw new NotFoundException("no such security group: " + this.securityGroup, e);
            }
            throw e;
        }
    }

}
