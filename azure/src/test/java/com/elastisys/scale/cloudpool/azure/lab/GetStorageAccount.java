package com.elastisys.scale.cloudpool.azure.lab;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetStorageAccountRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.rest.LogLevel;

/**
 * Lab program that gets a given storage account.
 *
 */
public class GetStorageAccount extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetStorageAccount.class);

    /** TODO: set to resource group of interest */
    private static final String resourceGroup = "itest";
    /** TODO: set to storage account of interest */
    private static final String storageAccountName = "itestdisks";

    public static void main(String[] args) {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        StorageAccount storageAccount = new GetStorageAccountRequest(apiAccess, resourceGroup, storageAccountName)
                .call();
        LOG.info("storage account: {}", storageAccount.id());
    }
}
