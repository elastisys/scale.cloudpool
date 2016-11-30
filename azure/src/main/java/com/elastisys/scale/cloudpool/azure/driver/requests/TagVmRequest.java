package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.HashMap;
import java.util.Map;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * An Azure request that, when called, tags a VM.
 */
public class TagVmRequest extends AzureRequest<Void> {
    /**
     * The fully qualified id of the VM to tag. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final String vmId;

    /** Tags to assign to VM. */
    private final Map<String, String> tags;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmId
     *            The fully qualified id of the VM to delete. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     * @param tags
     *            Tags to assign to VM.
     */
    public TagVmRequest(AzureApiAccess apiAccess, String vmId, Map<String, String> tags) {
        super(apiAccess);
        this.vmId = vmId;
        this.tags = tags;
    }

    @Override
    public Void doRequest(Azure api) throws RuntimeException {
        LOG.debug("tagging vm {}: {} ...", this.vmId, this.tags);
        VirtualMachine vm = new GetVmRequest(apiAccess(), this.vmId).call();

        // append tags to existing vm tags
        Map<String, String> newTags = new HashMap<>(vm.tags());
        newTags.putAll(this.tags);

        vm.update().withTags(newTags).apply();
        LOG.debug("vm {} tagged.", this.vmId);
        return null;
    }

}
