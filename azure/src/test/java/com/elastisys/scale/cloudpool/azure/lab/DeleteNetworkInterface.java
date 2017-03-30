package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.DeleteNetworkInterfaceRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that deletes a given network interface and any associated public
 * IP.
 */
public class DeleteNetworkInterface extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteNetworkInterface.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /**
     * TODO: set the name of the network interface within the resource group to
     * delete.
     */
    private static final String networkInterfaceName = "AzureTestPool-1481267299236-0";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        NetworkInterface nic = api.networkInterfaces().getByGroup(resourceGroup, networkInterfaceName);
        LOG.debug("found network interface {}", nic.id());

        new DeleteNetworkInterfaceRequest(apiAccess, nic.id()).call();
    }
}
