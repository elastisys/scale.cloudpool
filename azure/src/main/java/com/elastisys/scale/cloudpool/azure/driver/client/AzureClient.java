package com.elastisys.scale.cloudpool.azure.driver.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.AzurePoolDriver;
import com.elastisys.scale.cloudpool.azure.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * Azure client interface that captures only the subset of API calls required by
 * the {@link AzurePoolDriver}.
 */
public interface AzureClient {

    /**
     * Configures the client by specifying how to authenticate with the Azure
     * API and what resource group and region to limit its operations to.
     *
     * @param driverConfig
     *            Azure API driver configuration. Declares how to authenticate
     *            with the Azure API and what resource group and region contains
     *            pool assets.
     */
    void configure(CloudApiSettings driverConfig);

    /**
     * Launches a number of VMs, each with a given provisioning template.
     *
     * @param vmSpecs
     *            A number of VM provisioning templates.
     * @return
     * @throws AzureException
     */
    List<VirtualMachine> launchVms(List<VmSpec> vmSpecs) throws AzureException;

    /**
     * Deletes a collection of VMs and any associated network interfaces and
     * public IP addresses.
     * <p/>
     * Note that the specified VMs needs to be located in the resource group and
     * region that this client has been configured to access, or else the
     * operation will fail.
     *
     * @param vmIds
     *            A VM identifier. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     * @throws TerminateMachinesException
     *             If one or more vms could not be terminated.
     * @throws AzureException
     */
    void deleteVms(List<String> vmIds) throws TerminateMachinesException, AzureException;

    /**
     * Lists all VMs, with a given set of tags, that are located in the resource
     * group and region that this client has been configured to access. Note
     * that this may include vms in terminated states.
     *
     * @param withTags
     * @return
     * @throws AzureException
     */
    List<VirtualMachine> listVms(Map<String, String> withTags) throws AzureException;

    /**
     * Retrieve metadata about a particular VM.
     *
     * @param vmId
     *            A VM id, typically of form
     *            {@code /subscriptions/123...abc/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<vm-name>}.
     * @return
     * @throws NotFoundException
     * @throws AzureException
     */
    VirtualMachine getVm(String vmId) throws NotFoundException, AzureException;

    /**
     * Sets a collection of tags on a given VM.
     * <p/>
     * Note that the specified VM needs to be located in the resource group and
     * region that this client has been configured to access, or else the
     * operation will fail.
     *
     * @param vm
     * @param tags
     * @throws NotFoundException
     * @throws AzureException
     */
    void tagVm(VirtualMachine vm, Map<String, String> tags) throws NotFoundException, AzureException;

    /**
     * Removes a collection of tags from a given VM.
     * <p/>
     * Note that the specified VM needs to be located in the resource group and
     * region that this client has been configured to access, or else the
     * operation will fail.
     *
     * @param vm
     * @param tagKeys
     * @throws NotFoundException
     * @throws AzureException
     */
    void untagVm(VirtualMachine vm, Collection<String> tagKeys) throws NotFoundException, AzureException;
}
