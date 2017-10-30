package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkInterfaceRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given network interface.
 */
public class GetNetworkInterface extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetNetworkInterface.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the network within the resource group to get. */
    private static final String networkInterfaceName = "my-nic";

    public static void main(String[] args) {
        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String nicId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s", SUBSCRIPTION_ID,
                resourceGroup, networkInterfaceName);

        NetworkInterface nic = new GetNetworkInterfaceRequest(apiAccess, nicId).call();
        LOG.info("got network interface: {}", nic.id());
    }
}
