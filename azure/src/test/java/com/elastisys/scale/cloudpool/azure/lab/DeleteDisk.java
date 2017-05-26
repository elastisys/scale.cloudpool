package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.rest.LogLevel;

public class DeleteDisk extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteDisk.class);

    /** TODO: set resource group name to operate on */
    private static final String resourceGroup = "itest";

    /** TODO: set the name of the disk within the resource group to delete. */
    private static final String diskName = "testvm-1490866859894_OsDisk_1_6fbabf3fc2094686a84c5b04f11e21f4";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        Disk disk = api.disks().getByResourceGroup(resourceGroup, diskName);
        LOG.debug("found disk {}", disk.id());

        api.disks().deleteById(disk.id());
        LOG.debug("disk deleted", disk.id());
    }
}
