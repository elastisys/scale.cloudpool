package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.net.URI;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * An Azure request that, when called, deletes the VM disk VHD associated with a
 * given VM its the storage account container.
 *
 * @see PurgeVmRequest
 */
public class DeleteVmOsDiskRequest extends AzureRequest<Void> {

    /**
     * A VM. All blobs found in the storage account container with the VM name
     * as prefix will be deleted.
     */
    private final VirtualMachine vm;

    /**
     * Creates a {@link DeleteVmOsDiskRequest}. All blobs found in the storage
     * account container with the VM name as prefix will be deleted.
     *
     * @param apiAccess
     *            https://testpooldisks.blob.core.windows.net/vhds
     * @param vm
     *            A VM. All blobs found in the storage account container with
     *            the VM name as prefix will be deleted.
     */
    public DeleteVmOsDiskRequest(AzureApiAccess apiAccess, VirtualMachine vm) {
        super(apiAccess);

        this.vm = vm;
    }

    @Override
    public Void doRequest(Azure api) throws RuntimeException {
        LOG.debug("deleting OS disk for VM {} ...", this.vm.name());

        URI osDiskUri = URI.create(this.vm.osDiskVhdUri());
        String storageAccountName = extractStorageAccountName(osDiskUri);
        StorageAccount storageAccount = api.storageAccounts().getByGroup(this.vm.resourceGroupName(),
                storageAccountName);

        // extract API access key to allow us to access the blob container
        StorageAccountKey accessKey = storageAccount.getKeys().get(0);
        StorageCredentialsAccountAndKey storageCredentials = new StorageCredentialsAccountAndKey(storageAccountName,
                accessKey.value());

        try {
            LOG.debug("deleting disk blob {} ...", osDiskUri);
            new CloudBlockBlob(osDiskUri, storageCredentials).delete();
        } catch (StorageException e) {
            throw new RuntimeException(String.format("failed to delete storage account blobs for VM %s: %s",
                    this.vm.name(), e.getMessage()), e);
        }

        LOG.debug("done deleting OS disk for VM {}.", this.vm.name());
        return null;
    }

    /**
     * Extracts the name of a storage account from a storage account URI. It is
     * the first part of the host name:
     * {@code <storage-account-name>.blob.core.windows.net}.
     *
     * @param storageAccountContainerUri
     *            An absolute storage account container URI. For example,
     *            {@code https://<storage-account-name>.blob.core.windows.net/vhds}
     * @return
     */
    public static String extractStorageAccountName(URI storageAccountContainerUri) {
        return storageAccountContainerUri.getHost().split("\\.")[0];
    }

}
