package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;

/**
 * An Azure request that, when executed, deletes a given storage account.
 */
public class DeleteStorageAccountRequest extends AzureRequest<Void> {

    /** Name of the storage account to delete. */
    private final String storageAccountName;
    /**
     * The name of the resource group under which the storage account exists.
     */
    private final String resourceGroup;

    /**
     * Creates a {@link DeleteStorageAccountRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The name of the resource group under which the storage account
     *            exists.
     * @param storageAccountName
     *            Name of the storage account to delete.
     */
    public DeleteStorageAccountRequest(AzureApiAccess apiAccess, String resourceGroup, String storageAccountName) {
        super(apiAccess);
        this.resourceGroup = resourceGroup;
        this.storageAccountName = storageAccountName;
    }

    @Override
    public Void doRequest(Azure api) throws RuntimeException {
        LOG.debug("deleting storage account {} in resource group ...", this.storageAccountName, this.resourceGroup);
        api.storageAccounts().delete(this.resourceGroup, this.storageAccountName);
        LOG.debug("storage account {} deleted.", this.storageAccountName);

        return null;
    }

}
