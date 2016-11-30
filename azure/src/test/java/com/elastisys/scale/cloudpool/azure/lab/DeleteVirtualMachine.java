package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIpAddress;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Lab program that deletes a given VM and any associated public IP and network
 * interface.
 */
public class DeleteVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "testpool";
    private static final String region = "northeurope";

    /** TODO: set the name of the VM within the resource group to delete. */
    private static final String vmName = "AzureTestPool-1481264393858-0";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS),
                HttpLoggingInterceptor.Level.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        VirtualMachine vm = api.virtualMachines().getByGroup(resourceGroup, vmName);
        LOG.debug("found vm {}: {}", vm.name(), vm.id());

        NetworkInterface nic = vm.getPrimaryNetworkInterface();
        PublicIpAddress publicIp = nic.primaryIpConfiguration().getPublicIpAddress();
        if (publicIp != null) {
            LOG.debug("disassociating public ip {} ({}) from network interface {}", publicIp.name(),
                    publicIp.ipAddress(), nic.name());
            nic.update().withoutPrimaryPublicIpAddress().apply();
            LOG.debug("deleting public ip {} ...", publicIp.id());
            api.publicIpAddresses().delete(publicIp.id());
        }

        LOG.debug("deleting vm {}", vm.id());
        api.virtualMachines().delete(vm.id());
        LOG.debug("vm deleted.");

        try {
            LOG.debug("deleting network interface {} ...", nic.name());
            api.networkInterfaces().delete(nic.id());
            LOG.debug("network interface deleted.", nic.name());
        } catch (Exception e) {
            LOG.error("failed to delete primary network interface {}: {}", nic.name(), e);
        }
    }
}
