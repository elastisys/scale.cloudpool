package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.List;
import java.util.stream.Collectors;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.microsoft.azure.management.Azure;

/**
 * An Azure request that, when called, deletes a group of VMs. Note: any network
 * interface, public IP address, and storage account blobs (such as OS disk)
 * associated with the VM will <b>not</b> be removed by this request. See
 * {@link PurgeVmRequest}.
 */
public class DeleteVmsRequest extends AzureRequest<Void> {
    /**
     * The fully qualified ids of the VMs to delete. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final List<String> vmIds;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmIds
     *            The fully qualified ids of the VMs to delete. Typically of
     *            form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    public DeleteVmsRequest(AzureApiAccess apiAccess, List<String> vmIds) {
        super(apiAccess);
        this.vmIds = vmIds;
    }

    @Override
    public Void doRequest(Azure api) throws AzureException {
        try {
            List<String> vmNames = this.vmIds.stream().map(UrlUtils::basename).collect(Collectors.toList());
            LOG.debug("deleting vms {} ...", vmNames);
            api.virtualMachines().deleteByIds(this.vmIds);
            LOG.debug("vms deleted: {}", vmNames);
        } catch (Exception e) {
            throw new AzureException(String.format("failed to delete vms %s: %s", this.vmIds, e.getMessage()), e);
        }

        return null;
    }

}
