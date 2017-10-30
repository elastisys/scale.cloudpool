package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.List;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * An Azure request that, when executed, lists all VMs in a given resource
 * group.
 */
public class ListVmsRequest extends AzureRequest<List<VirtualMachine>> {
    /** The Azure resource group of interest. */
    private final String resourceGroup;

    /**
     * Creates a {@link ListVmsRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The Azure resource group of interest.
     */
    public ListVmsRequest(AzureApiAccess apiAccess, String resourceGroup) {
        super(apiAccess);
        this.resourceGroup = resourceGroup;
    }

    @Override
    public List<VirtualMachine> doRequest(Azure api) throws AzureException {
        List<VirtualMachine> vms;
        try {
            vms = api.virtualMachines().listByResourceGroup(this.resourceGroup);
        } catch (Exception e) {
            throw new AzureException("failed to list vm: " + e.getMessage(), e);
        }

        return vms;
    }
}
