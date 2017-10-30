package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkSecurityGroupRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given network security group.
 *
 */
public class GetNetworkSecurityGroup extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetNetworkSecurityGroup.class);

    /** TODO: set to resource group of interest */
    private static final String resourceGroup = "itest";
    /** TODO: set to network security group of interest */
    private static final String securityGroupName = "itest-webserver";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        NetworkSecurityGroup nsg = new GetNetworkSecurityGroupRequest(apiAccess, resourceGroup, securityGroupName)
                .call();
        LOG.info("network security group: {}", nsg.id());
    }
}
