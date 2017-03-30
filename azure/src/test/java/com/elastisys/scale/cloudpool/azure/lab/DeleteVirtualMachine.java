package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.PurgeVmRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
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
    private static final String vmName = "testvm-1490870352889";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        VirtualMachine vm = api.virtualMachines().getByGroup(resourceGroup, vmName);
        LOG.debug("found vm {}: {}", vm.name(), vm.id());

        LOG.debug("deleting vm {}", vm.id());
        new PurgeVmRequest(apiAccess, vm.id()).call();
        LOG.debug("vm deleted.");
    }
}
