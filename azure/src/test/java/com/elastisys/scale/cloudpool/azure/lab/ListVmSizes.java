package com.elastisys.scale.cloudpool.azure.lab;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.ListVmSizesRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that lists VM sizes.
 */
public class ListVmSizes extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ListVmSizes.class);

    /** TODO: set to region of interest */
    private static final String region = "northeurope";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        List<VirtualMachineSize> sizes = new ListVmSizesRequest(apiAccess, Region.findByLabelOrName(region)).call();
        for (VirtualMachineSize size : sizes) {
            LOG.info("size: {}: {} cores, {} MB memory", size.name(), size.numberOfCores(), size.memoryInMB());
        }
    }
}
