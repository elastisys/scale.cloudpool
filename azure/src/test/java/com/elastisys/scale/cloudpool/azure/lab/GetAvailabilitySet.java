package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetAvailabilitySetRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given availability set.
 */
public class GetAvailabilitySet extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetAvailabilitySet.class);

    /** TODO: set to resource group of interest */
    private static final String resourceGroup = "pkube";
    /** TODO: set to availability set of interest */
    private static final String availabilitySetName = "worker-sets";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        AvailabilitySet as = new GetAvailabilitySetRequest(apiAccess, resourceGroup, availabilitySetName).call();
        LOG.info("availability set: {}", as.id());
    }
}
