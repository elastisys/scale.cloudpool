package com.elastisys.scale.cloudpool.azure.lab;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.PurgeVmsRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that deletes a given VM and any associated public IP and network
 * interface.
 */
public class DeleteVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the VM within the resource group to delete. */
    private static final String vmName = "mv-vm";

    public static void main(String[] args) {
        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String vmId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", SUBSCRIPTION_ID,
                resourceGroup, vmName);

        LOG.debug("deleting vm {}", vmName);
        new PurgeVmsRequest(apiAccess, Arrays.asList(vmId)).call();
        LOG.debug("vm deleted.");
    }
}
