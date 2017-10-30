
package com.elastisys.scale.cloudpool.azure.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetVmRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.UntagVmRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that removes tags from a given VM.
 */
public class UntagVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(UntagVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the VM within the resource group to get. */
    private static final String vmName = "vm-123";

    /** TODO: set tags to remove. */
    private static final List<String> tagKeys = Arrays.asList("tag1");

    public static void main(String[] args) {
        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String vmId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", SUBSCRIPTION_ID,
                resourceGroup, vmName);

        VirtualMachine vm = new GetVmRequest(apiAccess, vmId).call();
        LOG.info("pre: vm tags: {}", vm.tags());

        new UntagVmRequest(apiAccess, vmId, tagKeys).call();
        LOG.info("vm untagged.");

        vm = new GetVmRequest(apiAccess, vmId).call();
        LOG.info("post: vm tags: {}", vm.tags());
    }
}
