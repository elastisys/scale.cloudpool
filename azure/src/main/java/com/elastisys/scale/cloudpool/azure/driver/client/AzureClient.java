package com.elastisys.scale.cloudpool.azure.driver.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.AzurePoolDriver;
import com.elastisys.scale.cloudpool.azure.driver.config.CloudApiSettings;
import com.microsoft.azure.CloudException;
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
     * @throws CloudException
     */
    List<VirtualMachine> launchVms(List<VmSpec> vmSpecs) throws CloudException;

    /**
     * Deletes a VM and any associated network interface and public IP address.
     * <p/>
     * Note that the specified VM needs to be located in the resource group and
     * region that this client has been configured to access, or else the
     * operation will fail.
     *
     * @param vmId
     *            A VM identifier. Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     * @throws NotFoundException
     * @throws CloudException
     */
    void deleteVm(String vmId) throws NotFoundException, CloudException;

    /**
     * Lists all VMs, with a given set of tags, that are located in the resource
     * group and region that this client has been configured to access.
     *
     * @param withTags
     * @return
     * @throws CloudException
     */
    List<VirtualMachine> listVms(Map<String, String> withTags) throws CloudException;

    /**
     * Retrieve metadata about a particular VM.
     *
     * @param vmId
     *            A VM id, typically of form
     *            {@code /subscriptions/123...abc/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<vm-name>}.
     * @return
     * @throws NotFoundException
     * @throws CloudException
     */
    VirtualMachine getVm(String vmId) throws NotFoundException, CloudException;

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
     * @throws CloudException
     */
    void tagVm(VirtualMachine vm, Map<String, String> tags) throws NotFoundException, CloudException;

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
     * @throws CloudException
     */
    void untagVm(VirtualMachine vm, Collection<String> tagKeys) throws NotFoundException, CloudException;
}
