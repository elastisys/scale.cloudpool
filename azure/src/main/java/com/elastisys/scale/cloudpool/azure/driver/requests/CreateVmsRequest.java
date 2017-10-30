package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.List;
import java.util.stream.Collectors;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.CreatedResources;

/**
 * An Azure request that, when called, creates a number of VMs from a collection
 * of VM definitions.
 */
public class CreateVmsRequest extends AzureRequest<CreatedResources<VirtualMachine>> {

    /** The VMs to create. */
    private final List<Creatable<VirtualMachine>> vmDefinitions;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmDefinitions
     *            The VMs to create.
     */
    public CreateVmsRequest(AzureApiAccess apiAccess, List<Creatable<VirtualMachine>> vmDefinitions) {
        super(apiAccess);
        this.vmDefinitions = vmDefinitions;
    }

    @Override
    public CreatedResources<VirtualMachine> doRequest(Azure api) throws AzureException {
        List<String> vmNames = this.vmDefinitions.stream().map(Creatable::name).collect(Collectors.toList());
        LOG.debug("creating VMs {} ...", vmNames);

        CreatedResources<VirtualMachine> created;
        try {
            created = api.virtualMachines().create(this.vmDefinitions);
        } catch (Exception e) {
            throw new AzureException(String.format("failed to create vms: %s: %s", vmNames, e.getMessage()), e);
        }

        LOG.debug("done creating VMs {}.", vmNames);
        return created;
    }

}
