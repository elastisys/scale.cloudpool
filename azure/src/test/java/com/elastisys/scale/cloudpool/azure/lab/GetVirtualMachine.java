package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that deletes a given VM and any associated public IP and network
 * interface.
 */
public class GetVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";
    private static final String region = "northeurope";

    /** TODO: set the name of the VM within the resource group to get. */
    private static final String vmName = "testvm-1490866859894";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        VirtualMachine vm = api.virtualMachines().getByResourceGroup(resourceGroup, vmName);
        LOG.debug("found vm {}: {}", vm.name(), vm.id());

        LOG.debug("is managed disk enabled: {}", vm.isManagedDiskEnabled());
        LOG.debug("os disk id: {} ", vm.osDiskId());
        LOG.debug("os disk vhd uri: {} ", vm.osUnmanagedDiskVhdUri());
        LOG.debug("storage account type: {} ", vm.osDiskStorageAccountType());
        LOG.debug("data disks: {}", vm.dataDisks());
        LOG.debug("data disks: {}", vm.storageProfile().dataDisks());
        LOG.debug("unmanaged data disks: {}", vm.unmanagedDataDisks());
    }
}
