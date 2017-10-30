package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Collection;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.Update;

/**
 * An Azure request that, when called, untags a VM.
 */
public class UntagVmRequest extends AzureRequest<Void> {
    /**
     * The fully qualified id of the VM to tag. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final String vmId;

    /** Tag keys to remove from VM. */
    private final Collection<String> tagKeys;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmId
     *            The fully qualified id of the VM to delete. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     * @param tagKeys
     *            Tag keys to remove from VM.
     */
    public UntagVmRequest(AzureApiAccess apiAccess, String vmId, Collection<String> tagKeys) {
        super(apiAccess);
        this.vmId = vmId;
        this.tagKeys = tagKeys;
    }

    @Override
    public Void doRequest(Azure api) throws AzureException {
        LOG.debug("untagging vm {}: {} ...", this.vmId, this.tagKeys);

        VirtualMachine vm = new GetVmRequest(apiAccess(), this.vmId).call();

        try {
            Update update = vm.update();
            for (String tagKey : this.tagKeys) {
                update.withoutTag(tagKey);
            }
            update.apply();
        } catch (Exception e) {
            throw new AzureException("failed to update vm tags: " + e.getMessage(), e);
        }

        LOG.debug("vm {} untagged.", this.vmId);
        return null;
    }

}
