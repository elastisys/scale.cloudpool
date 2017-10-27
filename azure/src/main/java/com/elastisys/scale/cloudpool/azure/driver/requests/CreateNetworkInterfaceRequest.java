package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterface.DefinitionStages.WithCreate;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * An Azure request that, when called, creates a {@link NetworkInterface} in a
 * given region and resource group, possibly with an associated public IP
 * address and network security groups.
 * <p/>
 * Such a {@link NetworkInterface} can later be associated with a VM.
 */
public class CreateNetworkInterfaceRequest extends AzureRequest<NetworkInterface> {

    /** The resource group under which to create the network interface. */
    private final String resourceGroup;
    /** The region in which to create the network interface. */
    private final Region region;
    /** The name of the network interface to create. */
    private final String nicName;

    /**
     * Specifies how to create the network interface: which virtual network and
     * subnet it belongs to, if it should have an associated public IP and
     * network security groups.
     */
    private final NetworkSettings networkSettings;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The resource group under which to create the network
     *            interface.
     * @param region
     *            The region in which to create the network interface.
     * @param nicName
     *            The name of the network interface to create.
     * @param networkSettings
     *            Specifies how to create the network interface: which virtual
     *            network and subnet it belongs to, if it should have an
     *            associated public IP and network security groups.
     */
    public CreateNetworkInterfaceRequest(AzureApiAccess apiAccess, String resourceGroup, Region region, String nicName,
            NetworkSettings networkSettings) {
        super(apiAccess);

        this.resourceGroup = resourceGroup;
        this.region = region;
        this.nicName = nicName;
        this.networkSettings = networkSettings;
    }

    @Override
    public NetworkInterface doRequest(Azure api) throws RuntimeException {
        LOG.debug("creating network interface {} ...", this.nicName);

        String networkName = this.networkSettings.getVirtualNetwork();
        Network network = new GetNetworkRequest(apiAccess(), networkName, this.resourceGroup).call();

        WithCreate nicDef = api.networkInterfaces().define(this.nicName) //
                .withRegion(this.region) //
                .withExistingResourceGroup(this.resourceGroup) //
                .withExistingPrimaryNetwork(network) //
                .withSubnet(this.networkSettings.getSubnetName()) //
                .withPrimaryPrivateIPAddressDynamic();
        if (this.networkSettings.getAssignPublicIp()) {
            nicDef.withNewPrimaryPublicIPAddress(this.nicName);
        }
        for (String securityGroup : this.networkSettings.getNetworkSecurityGroups()) {
            NetworkSecurityGroup nsg = new GetNetworkSecurityGroupRequest(apiAccess(), this.resourceGroup,
                    securityGroup).call();
            nicDef.withExistingNetworkSecurityGroup(nsg);
        }

        NetworkInterface networkInterface = nicDef.create();
        return networkInterface;
    }

}
