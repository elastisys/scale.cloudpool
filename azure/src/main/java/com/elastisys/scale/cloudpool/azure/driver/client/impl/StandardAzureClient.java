package com.elastisys.scale.cloudpool.azure.driver.client.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureClient;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetVmRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.PurgeVmRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.TagVmRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.UntagVmRequest;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * Standard {@link AzureClient} which makes actual calls to the Azure API.
 */
public class StandardAzureClient implements AzureClient {
    static final Logger LOG = LoggerFactory.getLogger(StandardAzureClient.class);
    static {
        // Install slf4j logging bridge to capture java.util.logging output from
        // the Azure SDK
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /** Azure API credentials. */
    private CloudApiSettings config;

    @Override
    public void configure(CloudApiSettings config) {
        checkArgument(config != null, "azure client config cannot be null");
        this.config = config;
    }

    @Override
    public List<VirtualMachine> launchVms(List<VmSpec> vmSpecs) throws AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        validateVmSpecs(vmSpecs);

        VmLauncher vmLauncher = new VmLauncher(apiAccess(), this.config.getResourceGroup(), this.config.getRegion());
        List<VirtualMachine> createdVms = vmLauncher.createVms(vmSpecs);
        LOG.debug("{} VMs launched.", createdVms.size());

        return createdVms;
    }

    @Override
    public void deleteVm(String vmId) throws NotFoundException, AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        new PurgeVmRequest(apiAccess(), vmId).call();
    }

    @Override
    public List<VirtualMachine> listVms(Map<String, String> withTags) throws AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        Azure api = ApiUtils.acquireApiClient(apiAccess());
        List<VirtualMachine> vms = api.virtualMachines().listByResourceGroup(this.config.getResourceGroup());

        // filter out VMs in wrong region and with wrong tag set
        List<VirtualMachine> filteredVms = vms.stream()//
                .filter(vm -> vm.regionName().equals(this.config.getRegion())) //
                .filter(vm -> vm.tags().entrySet().containsAll(withTags.entrySet()))//
                .collect(Collectors.toList());
        return filteredVms;
    }

    @Override
    public VirtualMachine getVm(String vmId) throws NotFoundException, AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        return new GetVmRequest(apiAccess(), vmId).call();
    }

    @Override
    public void tagVm(VirtualMachine vm, Map<String, String> tags) throws NotFoundException, AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        new TagVmRequest(apiAccess(), vm.id(), tags).call();
    }

    @Override
    public void untagVm(VirtualMachine vm, Collection<String> tagKeys) throws NotFoundException, AzureException {
        checkState(isConfigured(), "attempt to use unconfigured azure client");

        new UntagVmRequest(apiAccess(), vm.id(), tagKeys).call();
    }

    private AzureApiAccess apiAccess() {
        return this.config.getApiAccess();
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    /**
     * Validate a collection VM provisioning specs to ensure that referenced
     * assets exist.
     *
     * @param vmSpecs
     */
    private void validateVmSpecs(List<VmSpec> vmSpecs) {

        for (VmSpec vmSpec : vmSpecs) {
            try {
                new VmSpecValidator(apiAccess(), this.config.getRegion(), this.config.getResourceGroup())
                        .validateVmSpec(vmSpec);
            } catch (Exception e) {
                throw new IllegalArgumentException("failed to validate provisioning spec for a VM: " + e.getMessage(),
                        e);
            }
        }
    }
}
