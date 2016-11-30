package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;

/**
 * An Azure request that, when called, fetches available network security groups
 * in a given resource group.
 */
public class GetNetworkSecurityGroupsRequest extends AzureRequest<List<NetworkSecurityGroup>> {

    /** The Azure resource group that contains the security groups. */
    private final String resourceGroup;

    /**
     * Creates a {@link GetNetworkSecurityGroupsRequest} for a particular
     * region.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The Azure resource group that contains the security groups.
     */
    public GetNetworkSecurityGroupsRequest(AzureApiAccess apiAccess, String resourceGroup) {
        super(apiAccess);
        this.resourceGroup = resourceGroup;
    }

    @Override
    public List<NetworkSecurityGroup> doRequest(Azure api) throws RuntimeException {
        return new ArrayList<>(api.networkSecurityGroups().listByGroup(this.resourceGroup));
    }

}
