package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetVmRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given VM.
 */
public class GetVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "pkube";

    /** TODO: set the name of the VM within the resource group to get. */
    private static final String vmName = "kubemaster";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String vmId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", SUBSCRIPTION_ID,
                resourceGroup, vmName);
        VirtualMachine vm = new GetVmRequest(apiAccess, vmId).call();
        LOG.debug("found vm {}", vm.name());
        LOG.debug("vm powerstate: {}", vm.powerState());
        LOG.debug("vm proivsioningState: {}", vm.provisioningState());
    }
}
