package com.elastisys.scale.cloudpool.azure.lab;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that deletes a given VM and any associated public IP and network
 * interface.
 */
public class ListVirtualMachines extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ListVirtualMachines.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "testpool";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        PagedList<VirtualMachine> vmsInGroup = api.virtualMachines().listByGroup(resourceGroup);
        for (VirtualMachine vm : vmsInGroup) {
            LOG.debug("vm: {}", vm.name());
            LOG.debug("* tags: {}", vm.tags());
            LOG.debug("* private ip: {}", vm.getPrimaryNetworkInterface().primaryPrivateIp());
            if (vm.getPrimaryPublicIpAddress() != null) {
                LOG.debug("* public ip:  {}", vm.getPrimaryPublicIpAddress().ipAddress());
            }
            LOG.debug("* power state:        {}", vm.powerState());
            LOG.debug("* provisioning state: {}", vm.provisioningState());
            LOG.debug("* region: {}", vm.region());
            List<InstanceViewStatus> statuses = vm.instanceView().statuses();
            DateTime creationTime = null;
            for (InstanceViewStatus status : statuses) {
                if (status.code().toLowerCase().contains("provisioningstate")) {
                    creationTime = status.time();
                }
                LOG.debug("* status: {}", status.displayStatus());
                LOG.debug("  code:   {}", status.code());
                LOG.debug("  level:  {}", status.level());
                LOG.debug("  time:   {}", status.time());
                LOG.debug("  message:{}", status.message());
            }
            LOG.debug("* creation time: {}", creationTime);
        }
    }
}
