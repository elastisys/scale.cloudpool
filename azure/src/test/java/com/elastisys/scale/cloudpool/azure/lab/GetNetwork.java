package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.network.Network;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given network.
 */
public class GetNetwork extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetNetwork.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the network within the resource group to get. */
    private static final String networkName = "itestnet";

    public static void main(String[] args) {
        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        Network network = new GetNetworkRequest(apiAccess, resourceGroup, networkName).call();
        LOG.info("got network: {}", network.id());
    }
}
