package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * An Azure request that, when executed, retrieves metadata about a VM.
 */
public class GetVmRequest extends AzureRequest<VirtualMachine> {
    /**
     * The fully qualified id of the VM to get. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final String vmId;

    /**
     * Creates a {@link GetVmRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmId
     *            The fully qualified id of the VM to get. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    public GetVmRequest(AzureApiAccess apiAccess, String vmId) {
        super(apiAccess);
        this.vmId = vmId;
    }

    @Override
    public VirtualMachine doRequest(Azure api) throws NotFoundException, CloudException {
        try {
            LOG.debug("retrieving vm {} ...", this.vmId);
            return api.virtualMachines().getById(this.vmId);
        } catch (CloudException e) {
            if (e.getBody().getCode().equals("ResourceNotFound")) {
                throw new NotFoundException("no such vm: " + this.vmId, e);
            }
            throw e;
        }
    }

}
