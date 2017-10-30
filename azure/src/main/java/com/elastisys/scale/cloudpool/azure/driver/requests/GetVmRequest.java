package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
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
    public VirtualMachine doRequest(Azure api) throws NotFoundException, AzureException {
        VirtualMachine vm;
        try {
            LOG.debug("retrieving vm {} ...", this.vmId);
            vm = api.virtualMachines().getById(this.vmId);
        } catch (Exception e) {
            throw new AzureException("failed to get vm: " + e.getMessage(), e);
        }

        return Optional.ofNullable(vm).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("vm not found: %s", this.vmId));
    }
}
