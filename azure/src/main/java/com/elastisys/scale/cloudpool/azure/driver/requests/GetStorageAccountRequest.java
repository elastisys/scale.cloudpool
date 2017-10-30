package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
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
     * @param resourceGroup
     *            The resource group under which the storage account is assumed
     *            to exist.
     * @param storageAccountName
     *            The name of the storage account to get.
     */
    public GetStorageAccountRequest(AzureApiAccess apiAccess, String resourceGroup, String storageAccountName) {
        super(apiAccess);
        this.storageAccountName = storageAccountName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public StorageAccount doRequest(Azure api) throws NotFoundException, AzureException {
        StorageAccount storageAccount;

        try {
            storageAccount = api.storageAccounts().getByResourceGroup(this.resourceGroup, this.storageAccountName);
        } catch (Exception e) {
            throw new AzureException("failed to get storage account: " + e.getMessage(), e);
        }

        return Optional.ofNullable(storageAccount).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("storage account not found: resourceGroup: %s, name: %s",
                this.resourceGroup, this.storageAccountName));
    }
}
