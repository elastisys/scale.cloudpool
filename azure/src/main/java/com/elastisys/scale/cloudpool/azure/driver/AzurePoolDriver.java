package com.elastisys.scale.cloudpool.azure.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureClient;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.azure.driver.functions.VmToMachine;
import com.elastisys.scale.cloudpool.azure.driver.functions.VmToMachineState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.retryable.DelayStrategies;
import com.elastisys.scale.commons.net.retryable.GaveUpException;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.StopStrategies;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * The {@link AzurePoolDriver} is a management interface towards the Azure API.
 * It supports provisioning VMs according to the <a href=
 * "https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-manager-deployment-model">Resource
 * Manager deployment model</a>.
 *
 * @see BaseCloudPool
 */
public class AzurePoolDriver implements CloudPoolDriver {
    /**
     * The maximum amount of time after VM submission to wait for a set of VMs
     * to appear started before considering the start attempt a failure.
     */
    private static final TimeInterval VM_START_TIMEOUT = new TimeInterval(5L, TimeUnit.MINUTES);
    /**
     * The maximum amount of time after VM deletion to wait for a set of VMs to
     * disappear or appear in state deleting before timing outs.
     */
    private static final TimeInterval VM_DELETE_TIMEOUT = new TimeInterval(5L, TimeUnit.MINUTES);

    private static final Logger LOG = LoggerFactory.getLogger(AzurePoolDriver.class);

    /** Used to run concurrent tasks. */
    private final ScheduledExecutorService executor;

    /** Client for performing actions against the Azure REST API. */
    private final AzureClient client;

    /** The current driver configuration. */
    private DriverConfig config;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    public AzurePoolDriver(AzureClient client, ScheduledExecutorService executor) {
        this.client = client;
        this.executor = executor;
        this.config = null;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {
        synchronized (this.lock) {
            // parse and validate openstack-specific cloudApiSettings
            CloudApiSettings cloudApiSettings = configuration.parseCloudApiSettings(CloudApiSettings.class);
            cloudApiSettings.validate();

            // parse and validate openstack-specific provisioningTemplate
            ProvisioningTemplate provisioningTemplate = configuration
                    .parseProvisioningTemplate(ProvisioningTemplate.class);
            provisioningTemplate.validate();

            this.config = configuration;
            this.client.configure(cloudApiSettings);
        }
    }

    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        return getPoolVms().stream().map(new VmToMachine()).collect(Collectors.toList());
    }

    @Override
    public List<Machine> startMachines(int count) throws StartMachinesException {
        checkState(isConfigured(), "cannot use driver before being configured");

        ProvisioningTemplate provisioningTemplate = provisioningTemplate();
        // add a cloud pool membership tag
        Map<String, String> tags = provisioningTemplate.getTags();
        tags.put(Constants.CLOUD_POOL_TAG, getPoolName());

        // generate unique name for each VM
        long timeMillis = UtcTime.now().getMillis();
        String vmNamePrefix = getPoolName();
        if (provisioningTemplate.getVmNamePrefix().isPresent()) {
            vmNamePrefix = provisioningTemplate.getVmNamePrefix().get();
        }

        List<VmSpec> vmSpecs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String vmName = String.format("%s-%d-%d", vmNamePrefix, timeMillis, i);
            VmSpec vmSpec = new VmSpec(provisioningTemplate.getVmSize(), provisioningTemplate.getVmImage(),
                    provisioningTemplate.getOsDiskType(), vmName, provisioningTemplate.getLinuxSettings(),
                    provisioningTemplate.getWindowsSettings(), provisioningTemplate.getNetwork(),
                    provisioningTemplate.getAvailabilitySet().orElse(null), tags);
            vmSpecs.add(vmSpec);
        }
        LOG.info("launching VMs: {}", Joiner.on("\n").join(vmSpecs));
        try {
            // wait either for launch to complete (typically takes several
            // minutes) or vms to appear as started in api
            List<String> vmNames = vmSpecs.stream().map(VmSpec::getVmName).collect(Collectors.toList());
            Future<List<VirtualMachine>> launchTask = this.executor.submit(() -> this.client.launchVms(vmSpecs));
            Future<List<VirtualMachine>> awaitTask = this.executor
                    .submit(new AwaitVmsStarted(vmNames, VM_START_TIMEOUT));

            List<VirtualMachine> launchedVms;
            while (true) {
                if (launchTask.isDone()) {
                    LOG.debug("launch task done");
                    launchedVms = launchTask.get();
                    break;
                }
                if (awaitTask.isDone()) {
                    LOG.debug("await task done");
                    launchedVms = awaitTask.get();
                    break;
                }
            }

            LOG.debug("done launching VMs: {}",
                    Joiner.on(", ").join(launchedVms.stream().map(VirtualMachine::name).iterator()));
            List<Machine> machines = launchedVms.stream().map(new VmToMachine()).collect(Collectors.toList());
            return machines;
        } catch (Exception e) {
            throw new StartMachinesException(count, Collections.emptyList(), e);
        }
    }

    @Override
    public void terminateMachines(List<String> vmIds)
            throws IllegalStateException, TerminateMachinesException, CloudPoolDriverException {
        checkState(isConfigured(), "cannot use driver before being configured");

        List<String> victimIds = new ArrayList<>(vmIds);
        LOG.info("request to terminate instances: {}", vmIds);

        List<VirtualMachine> vms = getPoolVms();

        // track termination failures
        Map<String, Throwable> failures = new HashMap<>();

        // only terminate pool members (error mark other requests)
        nonPoolMembers(vmIds, vms).stream().forEach(vmId -> {
            failures.put(vmId, new NotFoundException(String.format("vm %s is not a member of the pool", vmId)));
            victimIds.remove(vmId);
        });

        // none of the machine ids were pool members
        if (victimIds.isEmpty()) {
            throw new TerminateMachinesException(Collections.emptyList(), failures);
        }

        LOG.info("terminating pool member instances: {}", victimIds);
        try {
            // wait either for delete task to finish (time-consuming) or until
            // vm are observed as deleted/deleting, whichever comes first.
            Future<Void> deleteTask = this.executor.submit(() -> {
                this.client.deleteVms(victimIds);
                return null;
            });
            Future<Void> awaitTask = this.executor.submit(new AwaitVmsDeleted(victimIds, VM_DELETE_TIMEOUT));
            while (true) {
                if (deleteTask.isDone()) {
                    LOG.debug("delete task done");
                    deleteTask.get();
                    break;
                }
                if (awaitTask.isDone()) {
                    LOG.debug("await task done");
                    awaitTask.get();
                    break;
                }
            }
        } catch (ExecutionException e) {
            if (e.getCause().getClass().equals(TerminateMachinesException.class)) {
                TerminateMachinesException err = TerminateMachinesException.class.cast(e.getCause());
                failures.putAll(err.getTerminationErrors());
                throw new TerminateMachinesException(err.getTerminatedMachines(), failures);
            } else {
                throw new CloudPoolDriverException("failed to terminate machines: " + e.getCause().getMessage(),
                        e.getCause());
            }
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to terminate machines: " + e.getMessage(), e);
        }

        if (!failures.isEmpty()) {
            throw new TerminateMachinesException(victimIds, failures);
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

        return this.config.getPoolName();

    }

    private void ensurePoolMember(VirtualMachine vm) throws NotFoundException {
        if (!vm.tags().containsKey(Constants.CLOUD_POOL_TAG)) {
            throw new NotFoundException(
                    String.format("the specified vm is not a pool member, it does not have the right %s tag",
                            vm.regionName(), Constants.CLOUD_POOL_TAG));
        }
    }

    private Map<String, String> cloudPoolTag() {
        return ImmutableMap.of(Constants.CLOUD_POOL_TAG, getPoolName());
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
        Region cloudpoolRegion = cloudApiSettings().getRegion();
        if (!vm.region().equals(cloudpoolRegion)) {
            throw new NotFoundException(String.format("the specified vm is located region %s, cloudpool uses region %s",
                    vm.regionName(), cloudpoolRegion));
        }
        String cloudpoolResourceGroup = cloudApiSettings().getResourceGroup();
        if (!vm.resourceGroupName().equalsIgnoreCase(cloudpoolResourceGroup)) {
            throw new NotFoundException(
                    String.format("the specified vm is in resource group %s, cloudpool uses resource group %s",
                            vm.resourceGroupName(), cloudpoolResourceGroup));
        }
    }

    /**
     * Lists all {@link VirtualMachine}s that are members of the cloudpool. Note
     * that this may include vms in terminated states.
     *
     * @return
     * @throws CloudPoolDriverException
     */
    private List<VirtualMachine> getPoolVms() throws CloudPoolDriverException {
        try {
            List<VirtualMachine> vms = this.client.listVms(cloudPoolTag());
            LOG.debug("pool vms: {}",
                    Joiner.on(", ").join(vms.stream().map(VirtualMachine::name).collect(Collectors.toList())));
            return vms;
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to list VMs: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the list of vm ids (from a given list of vm ids) that are *not*
     * members of the given pool.
     *
     * @param vmIds
     * @param pool
     * @return
     */
    private static List<String> nonPoolMembers(List<String> vmIds, List<VirtualMachine> pool) {
        return vmIds.stream().filter(vmId -> !member(vmId, pool)).collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if the given vm id is found in the given vm
     * pool.
     *
     * @param vmId
     * @param pool
     * @return
     */
    private static boolean member(String vmId, List<VirtualMachine> pool) {
        return pool.stream().anyMatch(vm -> vm.id().equals(vmId));
    }

    CloudApiSettings cloudApiSettings() {
        return config().parseCloudApiSettings(CloudApiSettings.class);
    }

    ProvisioningTemplate provisioningTemplate() {
        return config().parseProvisioningTemplate(ProvisioningTemplate.class);
    }

    DriverConfig config() {
        return this.config;
    }

    private boolean isConfigured() {
        return config() != null;
    }

    /**
     * A callable task that, when called, waits for a collection of VMs to
     * appear in a started state in the cloudpool. When the VMs are all started,
     * the list of {@link VirtualMachine}s are returned. If the specified set of
     * VMs have not appeared started, before a timeout is reached, a
     * {@link GaveUpException} is thrown.
     */
    private class AwaitVmsStarted implements Callable<List<VirtualMachine>> {
        private final List<String> vmNames;
        private final TimeInterval timeout;
        /** Delay between cloudpool polls. */
        private final TimeInterval delay = new TimeInterval(5L, TimeUnit.SECONDS);

        public AwaitVmsStarted(List<String> vmNames, TimeInterval timeout) {
            this.vmNames = vmNames;
            this.timeout = timeout;
        }

        @Override
        public List<VirtualMachine> call() throws Exception {
            String taskName = String.format("await-vms-started[%s]", String.join(",", this.vmNames));
            Callable<List<VirtualMachine>> task = () -> getPoolVms().stream()
                    .filter(vm -> this.vmNames.contains(vm.name())).collect(Collectors.toList());
            Retryable<List<VirtualMachine>> retryable = new Retryable<>(task) //
                    .name(taskName) //
                    .retryOnException() //
                    .delay(DelayStrategies.fixed(this.delay.getTime(), this.delay.getUnit())) //
                    .stop(StopStrategies.afterTime(this.timeout.getTime(), this.timeout.getUnit()))//
                    .retryUntilResponse(vms -> allStarted(this.vmNames, vms));
            return retryable.call();
        }

        private boolean allStarted(List<String> expectedVmNames, List<VirtualMachine> actualVms) {
            Map<String, VirtualMachine> actualVmsByName = actualVms.stream()
                    .collect(Collectors.toMap(VirtualMachine::name, vm -> vm));

            for (String expectedVmName : expectedVmNames) {
                if (!actualVmsByName.containsKey(expectedVmName)) {
                    return false;
                }
                VirtualMachine actualVm = actualVmsByName.get(expectedVmName);
                MachineState vmState = new VmToMachineState().apply(actualVm);
                if (vmState != MachineState.PENDING && vmState != MachineState.RUNNING) {
                    return false;
                }
            }
            // all vms were found to be started
            return true;
        }
    }

    /**
     * A callable task that, when called, waits for a collection of VMs to
     * either be gone or to appear in a deleting state. If the specified set of
     * VMs have not been observed as deleted before a timeout is reached, a
     * {@link GaveUpException} is thrown.
     */
    private class AwaitVmsDeleted implements Callable<Void> {
        private final List<String> vmIds;
        private final TimeInterval timeout;
        /** Delay between cloudpool polls. */
        private final TimeInterval delay = new TimeInterval(5L, TimeUnit.SECONDS);

        public AwaitVmsDeleted(List<String> vmIds, TimeInterval timeout) {
            this.vmIds = vmIds;
            this.timeout = timeout;
        }

        @Override
        public Void call() throws Exception {
            List<String> vmNames = this.vmIds.stream().map(UrlUtils::basename).collect(Collectors.toList());
            String taskName = String.format("await-deletion[%s]", String.join(",", vmNames));
            Callable<List<VirtualMachine>> task = () -> getPoolVms();
            Retryable<List<VirtualMachine>> retryable = new Retryable<>(task) //
                    .name(taskName) //
                    .retryOnException() //
                    .delay(DelayStrategies.fixed(this.delay.getTime(), this.delay.getUnit())) //
                    .stop(StopStrategies.afterTime(this.timeout.getTime(), this.timeout.getUnit()))//
                    .retryUntilResponse(vms -> allDeleted(this.vmIds, vms));
            retryable.call();

            return null;
        }

        /**
         * Returns <code>true</code> if all {@code #victimIds} have been
         * deleted, meaning that they are either no longer in the current list
         * of pool vms, or in a {@code TERMINATING}/{@code TERMINATED} state.
         *
         * @param victimIds
         *            The vm ids that we want to see deleted.
         * @param actualVms
         *            the current list of pool vms.
         * @return
         */
        private boolean allDeleted(List<String> victimIds, List<VirtualMachine> actualVms) {
            Map<String, VirtualMachine> actualVmsById = actualVms.stream()
                    .collect(Collectors.toMap(VirtualMachine::id, vm -> vm));

            for (String victimId : victimIds) {
                if (actualVmsById.keySet().contains(victimId)) {
                    VirtualMachine vm = actualVmsById.get(victimId);
                    MachineState vmState = new VmToMachineState().apply(vm);
                    if (vmState != MachineState.TERMINATING && vmState != MachineState.TERMINATED) {
                        return false;
                    }
                }
            }
            // all vms are either gone from pool or in a TERMINATING/TERMINATED
            // state
            return true;
        }
    }
}
