package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * An Azure request that, when called, deletes a VM together with its network
 * interface, any associated public IP address, and storage account blobs (such
 * as OS disk, which are assumed to be named with the VM name as prefix).
 */
public class PurgeVmRequest extends AzureRequest<Void> {
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
    public PurgeVmRequest(AzureApiAccess apiAccess, String vmId) {
        super(apiAccess);
        this.vmId = vmId;
    }

    @Override
    public Void doRequest(Azure api) throws RuntimeException {
        LOG.debug("purging vm {} ...", this.vmId);
        VirtualMachine vm = new GetVmRequest(apiAccess(), this.vmId).call();
        // need to delete VM before its network interface
        new DeleteVmRequest(apiAccess(), this.vmId).call();
        new DeleteNetworkInterfaceRequest(apiAccess(), vm.primaryNetworkInterfaceId()).call();
        new DeleteVmOsDiskRequest(apiAccess(), vm).call();
        LOG.debug("vm {} purged.", this.vmId);
        return null;
    }

}
