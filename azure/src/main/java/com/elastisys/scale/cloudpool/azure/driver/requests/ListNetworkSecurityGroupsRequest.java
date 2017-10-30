package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;

/**
 * An Azure request that, when called, fetches available network security groups
 * in a given resource group.
 */
public class ListNetworkSecurityGroupsRequest extends AzureRequest<List<NetworkSecurityGroup>> {

    /** The Azure resource group that contains the security groups. */
    private final String resourceGroup;

    /**
     * Creates a {@link ListNetworkSecurityGroupsRequest} for a particular
     * region.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The Azure resource group that contains the security groups.
     */
    public ListNetworkSecurityGroupsRequest(AzureApiAccess apiAccess, String resourceGroup) {
        super(apiAccess);
        this.resourceGroup = resourceGroup;
    }

    @Override
    public List<NetworkSecurityGroup> doRequest(Azure api) throws AzureException {
        List<NetworkSecurityGroup> securityGroups;

        try {
            securityGroups = api.networkSecurityGroups().listByResourceGroup(this.resourceGroup);
        } catch (Exception e) {
            throw new AzureException("failed to list network security groups: " + e.getMessage());
        }

        return new ArrayList<>(securityGroups);
    }

}
