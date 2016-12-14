package com.elastisys.scale.cloudpool.azure.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureClient;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.AzurePoolDriverConfig;
import com.elastisys.scale.cloudpool.azure.driver.config.ScaleOutExtConfig;
import com.elastisys.scale.cloudpool.azure.driver.functions.VmToMachine;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * The {@link AzurePoolDriver} is a management interface towards the Azure API.
 * It supports provisioning VMs according to the <a href=
 * "https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-manager-deployment-model">Resource
 * Manager deployment model</a>.
 *
 * @see BaseCloudPool
 */
public class AzurePoolDriver implements CloudPoolDriver {
    private static final Logger LOG = LoggerFactory.getLogger(AzurePoolDriver.class);

    /** Supported API versions by this implementation. */
    private final static List<String> supportedApiVersions = Arrays.asList(ApiVersion.LATEST);
    /** Cloud pool metadata for this implementation. */
    private final static CloudPoolMetadata cloudPoolMetadata = new CloudPoolMetadata(CloudProviders.AZURE,
            supportedApiVersions);

    /** Logical name of the managed machine pool. */
    private String poolName;

    private final AzureClient client;
    private AzurePoolDriverConfig driverConfig;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    public AzurePoolDriver(AzureClient client) {
        this.poolName = null;
        this.client = client;
        this.driverConfig = null;
    }

    @Override
    public void configure(BaseCloudPoolConfig config) throws IllegalArgumentException, CloudPoolDriverException {
        checkArgument(config != null, "null configuration received");
        config.validate();

        synchronized (this.lock) {
            AzurePoolDriverConfig driverConfig = validateDriverConfig(config.getCloudPool().getDriverConfig());

            // check azure-specific extensions
            validateScaleOutAzureExtensions(config.getScaleOutConfig());

            this.poolName = config.getCloudPool().getName();
            this.client.configure(driverConfig);
            this.driverConfig = driverConfig;
        }
    }

    private void validateScaleOutAzureExtensions(ScaleOutConfig scaleOutConfig) throws IllegalArgumentException {
        JsonObject extensions = scaleOutConfig.getExtensions();
        checkArgument(extensions != null, "scaleOutConfig: missing extensions field for Azure-specific functionality");

        try {
            ScaleOutExtConfig azureExt = JsonUtils.toObject(extensions, ScaleOutExtConfig.class);
            azureExt.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to validate Azure-specific scaleOutConfig extensions: " + e.getMessage(), e);
        }
    }

    private AzurePoolDriverConfig validateDriverConfig(JsonObject rawDriverConfig) {
        try {
            AzurePoolDriverConfig driverConfig = JsonUtils.toObject(rawDriverConfig, AzurePoolDriverConfig.class);
            driverConfig.validate();
            return driverConfig;
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to apply driver configuration: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        try {
            List<VirtualMachine> vms = this.client.listVms(cloudPoolTag());
            LOG.debug("pool vms: {}",
                    Joiner.on(", ").join(vms.stream().map(VirtualMachine::name).collect(Collectors.toList())));
            List<Machine> machines = vms.stream().map(new VmToMachine()).collect(Collectors.toList());
            return machines;
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to list VMs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Machine> startMachines(int count, ScaleOutConfig vmTemplate) throws StartMachinesException {
        checkState(isConfigured(), "cannot use driver before being configured");

        // azure-specific provisioning details located under extensions field
        ScaleOutExtConfig vmTemplateAzureExt = JsonUtils.toObject(vmTemplate.getExtensions(), ScaleOutExtConfig.class);

        String vmSize = vmTemplate.getSize();
        String vmImage = vmTemplate.getImage();
        Map<String, String> tags = vmTemplateAzureExt.getTags();
        tags.put(Constants.CLOUD_POOL_TAG, this.poolName);

        long timeMillis = System.currentTimeMillis();
        String vmNamePrefix = this.poolName;
        if (vmTemplateAzureExt.getVmNamePrefix().isPresent()) {
            vmNamePrefix = vmTemplateAzureExt.getVmNamePrefix().get();
        }

        List<VmSpec> vmSpecs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String vmName = String.format("%s-%d-%d", vmNamePrefix, timeMillis, i);
            VmSpec vmSpec = new VmSpec(vmSize, vmImage, vmName, vmTemplateAzureExt.getLinuxSettings(),
                    vmTemplateAzureExt.getWindowsSettings(), vmTemplateAzureExt.getStorageAccountName(),
                    vmTemplateAzureExt.getNetwork(), tags);
            vmSpecs.add(vmSpec);
        }
        LOG.info("launching VMs: {}", Joiner.on("\n").join(vmSpecs));
        try {
            List<VirtualMachine> launchedVms = this.client.launchVms(vmSpecs);
            LOG.debug("done launching VMs: {}",
                    Joiner.on(", ").join(launchedVms.stream().map(VirtualMachine::name).iterator()));

            List<Machine> machines = launchedVms.stream().map(new VmToMachine()).collect(Collectors.toList());
            return machines;
        } catch (Exception e) {
            throw new StartMachinesException(count, Collections.emptyList(), e);
        }
    }

    @Override
    public void terminateMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        try {
            LOG.info("deleting VM {} ...", machineId);
            this.client.deleteVm(machineId);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException("failed to terminate vm " + machineId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        // verifies that VM exists
        VirtualMachine vm = this.client.getVm(machineId);
        // only attach VM if it belongs to the region and resource group that
        // the cloudpool is configured to use
        ensureRightRegionAndResouceGroup(vm);

        try {
            LOG.info("attaching VM {} ...", machineId);
            this.client.tagVm(vm, cloudPoolTag());
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to attach vm " + machineId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void detachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        // verifies that VM exists
        VirtualMachine vm = this.client.getVm(machineId);
        // only detach VM if it is a pool member
        ensurePoolMember(vm);

        try {
            LOG.info("detaching VM {} ...", machineId);
            this.client.untagVm(vm, Arrays.asList(Constants.CLOUD_POOL_TAG));
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to detach vm " + machineId + ": " + e.getMessage(), e);
        }

    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        // verifies that VM exists
        VirtualMachine vm = this.client.getVm(machineId);
        // only set status VM if it is a pool member
        ensurePoolMember(vm);

        try {
            LOG.info("setting service state for VM {} ...", machineId);
            Map<String, String> tags = ImmutableMap.of(Constants.SERVICE_STATE_TAG, serviceState.name());
            this.client.tagVm(vm, tags);
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    "failed to set service state for vm " + machineId + ": " + e.getMessage(), e);
        }

    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        // verifies that VM exists
        VirtualMachine vm = this.client.getVm(machineId);
        // only set status VM if it is a pool member
        ensurePoolMember(vm);

        try {
            LOG.info("setting membership status for VM {} ...", machineId);
            Map<String, String> tags = ImmutableMap.of(Constants.MEMBERSHIP_STATUS_TAG,
                    JsonUtils.toString(toJson(membershipStatus)));
            this.client.tagVm(vm, tags);
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    "failed to set membership status for vm " + machineId + ": " + e.getMessage(), e);
        }

    }

    @Override
    public String getPoolName() {
        checkState(isConfigured(), "cannot use driver before being configured");

        return this.poolName;

    }

    @Override
    public CloudPoolMetadata getMetadata() {
        return cloudPoolMetadata;
    }

    private void ensurePoolMember(VirtualMachine vm) throws NotFoundException {
        if (!vm.tags().containsKey(Constants.CLOUD_POOL_TAG)) {
            throw new NotFoundException(
                    String.format("the specified vm is not a pool member, it does not have the right %s tag",
                            vm.regionName(), Constants.CLOUD_POOL_TAG));
        }
    }

    private boolean isConfigured() {
        return this.driverConfig != null;
    }

    private Map<String, String> cloudPoolTag() {
        return ImmutableMap.of(Constants.CLOUD_POOL_TAG, this.poolName);
    }

    /**
     * Throws a {@link NotFoundException} if the given VM is either located in a
     * different region or is a member of a different resource group than the
     * cloudpool.
     *
     * @param vm
     * @throws NotFoundException
     */
    private void ensureRightRegionAndResouceGroup(VirtualMachine vm) throws NotFoundException {
        String cloudpoolRegion = this.driverConfig.getRegion();
        if (!vm.regionName().equalsIgnoreCase(cloudpoolRegion)) {
            throw new NotFoundException(String.format("the specified vm is located region %s, cloudpool uses region %s",
                    vm.regionName(), cloudpoolRegion));
        }
        String cloudpoolResourceGroup = this.driverConfig.getResourceGroup();
        if (!vm.resourceGroupName().equalsIgnoreCase(cloudpoolResourceGroup)) {
            throw new NotFoundException(
                    String.format("the specified vm is in resource group %s, cloudpool uses resource group %s",
                            vm.resourceGroupName(), cloudpoolResourceGroup));
        }
    }

    /**
     * Returns the currently set config, if any. Otherwise, <code>null</code> is
     * returned.
     *
     * @return
     */
    AzurePoolDriverConfig config() {
        return this.driverConfig;
    }

}
