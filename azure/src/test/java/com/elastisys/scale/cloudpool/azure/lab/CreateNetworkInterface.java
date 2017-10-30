package com.elastisys.scale.cloudpool.azure.lab;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateNetworkInterfaceRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

/**
 * Lab program for creating a network interface in Azure.
 */
public class CreateNetworkInterface extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateNetworkInterface.class);

    /** TODO: set to region to create NIC in */
    private static final Region region = Region.US_EAST;
    /** TODO: set to resource group to create NIC under */
    private static final String resourceGroup = "itest";

    /** TODO: set to name of network interface card to create */
    private static final String nicName = "my-nic";
    /** TODO: set to name of the virtual network that NIC is to be created in */
    private static final String virtualNetwork = "itestnet";
    /** TODO: set to the virtual network subnet that NIC is to be created in */
    private static final String subnetName = "default";
    /**
     * TODO: set to indicate if a public IP should be associated with the
     * created NIC
     */
    private static final boolean assignPublicIp = true;
    /** TODO: set to name of network that NIC is to be created in */
    private static final List<String> networkSecurityGroups = Arrays.asList("itest-webserver");

    private static final NetworkSettings networkSettings = new NetworkSettings(virtualNetwork, subnetName,
            assignPublicIp, networkSecurityGroups);

    public static void main(String[] args) throws CloudException, IOException {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        NetworkInterface nic = new CreateNetworkInterfaceRequest(apiAccess, resourceGroup, region, nicName,
                networkSettings).call();
        LOG.info("NIC created: {}", nic.id());
    }

}
