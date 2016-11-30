package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Lab program that deletes a given VM and any associated public IP and network
 * interface.
 */
public class GetVirtualMachine extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetVirtualMachine.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "testpool";
    private static final String region = "northeurope";

    /** TODO: set the name of the VM within the resource group to get. */
    private static final String vmName = "AzureTestPool-1481267548482-01";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS),
                HttpLoggingInterceptor.Level.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        VirtualMachine vm = api.virtualMachines().getByGroup(resourceGroup, vmName);
        LOG.debug("found vm {}: {}", vm.name(), vm.id());
    }
}
