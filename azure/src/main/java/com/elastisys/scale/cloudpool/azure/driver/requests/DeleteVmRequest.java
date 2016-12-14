package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;

/**
 * An Azure request that, when called, deletes a VM. Note: any network
 * interface, public IP address, and storage account blobs (such as OS disk)
 * associated with the VM will not be removed by this request. See
 * {@link PurgeVmRequest}.
 */
public class DeleteVmRequest extends AzureRequest<Void> {
    /**
     * The fully qualified id of the VM to delete. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final String vmId;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmId
     *            The fully qualified id of the VM to delete. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    public DeleteVmRequest(AzureApiAccess apiAccess, String vmId) {
        super(apiAccess);
        this.vmId = vmId;
    }

    @Override
    public Void doRequest(Azure api) throws RuntimeException {
        LOG.debug("deleting vm {} ...", this.vmId);
        api.virtualMachines().delete(this.vmId);
        LOG.debug("vm {} deleted.", this.vmId);
        return null;
    }

}
