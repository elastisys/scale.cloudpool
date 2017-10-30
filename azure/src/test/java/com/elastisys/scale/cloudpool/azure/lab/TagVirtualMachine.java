package com.elastisys.scale.cloudpool.azure.lab;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetVmRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.TagVmRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that tags a given VM.
 */
public class TagVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(UntagVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the VM within the resource group to get. */
    private static final String vmName = "vm-123";

    /** TODO: set tags to apply. */
    private static final Map<String, String> tags = ImmutableMap.of("tag1", "value1");

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String vmId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", SUBSCRIPTION_ID,
                resourceGroup, vmName);

        VirtualMachine vm = new GetVmRequest(apiAccess, vmId).call();
        LOG.info("pre: vm tags: {}", vm.tags());

        new TagVmRequest(apiAccess, vmId, tags).call();
        LOG.info("vm tagged.");

        vm = new GetVmRequest(apiAccess, vmId).call();
        LOG.info("post: vm tags: {}", vm.tags());
    }
}
