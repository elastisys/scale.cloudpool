package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;

/**
 * An Azure request that, when executed, retrieves metadata about a storage
 * account.
 */
public class GetStorageAccountRequest extends AzureRequest<StorageAccount> {
    /** The name of the storage account to get. */
    private final String storageAccountName;

    /**
     * The resource group under which the storage account is assumed to exist.
     */
    private final String resourceGroup;

    /**
     * Creates a {@link GetStorageAccountRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param storageAccountName
     *            The name of the storage account to get.
     * @param resourceGroup
     *            The resource group under which the storage account is assumed
     *            to exist.
     */
    public GetStorageAccountRequest(AzureApiAccess apiAccess, String storageAccountName, String resourceGroup) {
        super(apiAccess);
        this.storageAccountName = storageAccountName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public StorageAccount doRequest(Azure api) throws NotFoundException, CloudException {
        try {
            LOG.debug("retrieving storage account {} ...", this.storageAccountName);
            return api.storageAccounts().getByResourceGroup(this.resourceGroup, this.storageAccountName);
        } catch (CloudException e) {
            if (e.body().code().equals("ResourceNotFound")) {
                throw new NotFoundException("no such storage account: " + this.storageAccountName, e);
            }
            throw e;
        }
    }

}
