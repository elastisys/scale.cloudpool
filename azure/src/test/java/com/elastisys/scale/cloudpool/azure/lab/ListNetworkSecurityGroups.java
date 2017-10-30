package com.elastisys.scale.cloudpool.azure.lab;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.ListNetworkSecurityGroupsRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that lists network security groups.
 */
public class ListNetworkSecurityGroups extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ListNetworkSecurityGroups.class);

    /** TODO: set to resource group of interest */
    private static final String resourceGroup = "itest";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        List<NetworkSecurityGroup> nsgs = new ListNetworkSecurityGroupsRequest(apiAccess, resourceGroup).call();
        for (NetworkSecurityGroup nsg : nsgs) {
            LOG.info("network security group: {}", nsg.name());
        }
    }
}
