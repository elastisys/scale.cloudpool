package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIpAddress;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Lab program that deletes a given network interface and any associated public
 * IP.
 */
public class DeleteNetworkInterface extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteNetworkInterface.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "testpool";
    private static final String region = "northeurope";

    /** TODO: set the name of the VM within the resource group to delete. */
    private static final String networkInterfaceName = "AzureTestPool-1481267299236-0";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS),
                HttpLoggingInterceptor.Level.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        NetworkInterface nic = api.networkInterfaces().getByGroup(resourceGroup, networkInterfaceName);
        LOG.debug("found network interface {}", nic.id());
        PublicIpAddress publicIp = nic.primaryIpConfiguration().getPublicIpAddress();
        if (publicIp != null) {
            LOG.debug("disassociating public ip {} ({}) from network interface {}", publicIp.name(),
                    publicIp.ipAddress(), nic.name());
            nic.update().withoutPrimaryPublicIpAddress().apply();
            LOG.debug("deleting public ip {} ...", publicIp.id());
            api.publicIpAddresses().delete(publicIp.id());
        }

        try {
            LOG.debug("deleting network interface {} ...", nic.name());
            api.networkInterfaces().delete(nic.id());
            LOG.debug("network interface deleted.", nic.name());
        } catch (Exception e) {
            LOG.error("failed to delete primary network interface {}: {}", nic.name(), e);
        }
    }
}
