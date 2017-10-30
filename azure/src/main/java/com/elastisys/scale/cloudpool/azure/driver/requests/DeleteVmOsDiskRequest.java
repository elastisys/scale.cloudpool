package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.net.URI;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * An Azure request that, when called, deletes the OS disk associated with a
 * given VM. If the OS disk is an unmanaged disk, all blobs found in the storage
 * account container with the VM name as prefix will be deleted.
 *
 * @see PurgeVmsRequest
 */
public class DeleteVmOsDiskRequest extends AzureRequest<Void> {

    /** A VM whose OS disk is to be deleted. */
    private final VirtualMachine vm;

    /**
     * Creates a {@link DeleteVmOsDiskRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vm
     *            A VM whose OS disk is to be deleted.
     */
    public DeleteVmOsDiskRequest(AzureApiAccess apiAccess, VirtualMachine vm) {
        super(apiAccess);

        this.vm = vm;
    }

    @Override
    public Void doRequest(Azure api) throws AzureException {

        try {
            LOG.debug("deleting OS disk for VM {} ...", this.vm.name());
            if (this.vm.isManagedDiskEnabled()) {
                deleteManagedOsDisk(api);
            } else {
                deleteUnmanagedOsDisk(api);
            }
            LOG.debug("done deleting OS disk for VM {}.", this.vm.name());
        } catch (Exception e) {
            throw new AzureException(
                    String.format("failed to delete OS disk for VM %s: %s", this.vm.name(), e.getMessage()), e);
        }

        return null;
    }

    private void deleteManagedOsDisk(Azure api) {
        LOG.debug("deleting managed OS disk for VM {} ...", this.vm.name());
        String osDiskId = this.vm.osDiskId();
        if (osDiskId == null) {
            LOG.debug("no managed OS disk to delete found for VM {}", this.vm.name());
            return;
        }
        LOG.debug("deleting managed OS disk {}", osDiskId);
        api.disks().deleteById(osDiskId);
    }

    private void deleteUnmanagedOsDisk(Azure api) {
        LOG.debug("deleting unmanaged OS disk for VM {} ...", this.vm.name());

        String osDiskUrl = this.vm.osUnmanagedDiskVhdUri();
        if (osDiskUrl == null) {
            LOG.warn("no unmanaged OS disk to delete found for VM {}.", this.vm.name());
            return;
        }

        LOG.debug("deleting unmanaged OS disk {}", osDiskUrl);
        URI osDiskUri = URI.create(osDiskUrl);
        String storageAccountName = extractStorageAccountName(osDiskUri);
        StorageAccount storageAccount = api.storageAccounts().getByResourceGroup(this.vm.resourceGroupName(),
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
