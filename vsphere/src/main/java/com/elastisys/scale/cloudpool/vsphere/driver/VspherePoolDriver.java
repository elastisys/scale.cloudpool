package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.functions.VirtualMachineToMachine;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.mo.VirtualMachine;
import jersey.repackaged.com.google.common.collect.Lists;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * A {@link CloudPoolDriver} implementation that operates against Vsphere.
 */
public class VspherePoolDriver implements CloudPoolDriver {

    private VsphereClient vsphereClient;
    private DriverConfig driverConfig;

    /**
     * Create a new {@link VspherePoolDriver} that needs to be configured before use.
     *
     * @param vsphereClient The client that communicates with the Vsphere API.
     */
    public VspherePoolDriver(VsphereClient vsphereClient) {
        this.vsphereClient = vsphereClient;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {
        VsphereApiSettings vsphereApiSettings;
        VsphereProvisioningTemplate vsphereProvisioningTemplate;

        vsphereApiSettings = configuration.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereApiSettings.validate();
        vsphereProvisioningTemplate = configuration.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
        vsphereProvisioningTemplate.validate();

        try {
            vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        } catch (RemoteException e) {
            throw new CloudPoolDriverException();
        }

        driverConfig = configuration;
    }

    @Override
    public List<Machine> listMachines() throws IllegalStateException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        List<Machine> machines = Lists.newArrayList();

        List<String> pendingNames = vsphereClient.pendingVirtualMachines();

        try {
            List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(cloudPoolTag());
            machines.addAll(Lists.transform(virtualMachines, new VirtualMachineToMachine()));
        } catch (RemoteException e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\": %s", getPoolName(), e.getMessage()), e);
        }

        // Make sure no machine is listed as both pending and running
        removeDoubles(machines, pendingNames);

        machines.addAll(placeholderMachines(pendingNames));
        return machines;
    }

    @Override
    public List<Machine> startMachines(int count) throws IllegalStateException, StartMachinesException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        List<String> names = Lists.newArrayListWithCapacity(count);
        try {
            names = vsphereClient.launchVirtualMachines(count, cloudPoolTag());
        } catch (RemoteException e) {
            throw new StartMachinesException(count, placeholderMachines(names), e);
        }
        return placeholderMachines(names);
    }

    @Override
    public void terminateMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        getMachineOrFail(machineId);
        try {
            vsphereClient.terminateVirtualMachines(Arrays.asList(machineId));
        } catch (RemoteException e) {
            String message = format("Failed to terminate machine \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void attachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
    }

    @Override
    public void detachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
    }

    @Override
    public String getPoolName() throws IllegalStateException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        return driverConfig.getPoolName();
    }

    public boolean isConfigured() {
        return driverConfig != null;
    }

    private List<Tag> cloudPoolTag() {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        return Lists.newArrayList(new VsphereTag(ScalingTag.CLOUD_POOL, driverConfig.getPoolName()));
    }

    /**
     * Create a list of machines to use as placeholders for pending machines.
     *
     * @param names A list of names to set for the created machines.
     * @return  A list of pending machines.
     */
    private List<Machine> placeholderMachines(List<String> names) {
        List<Machine> placeholderMachines = Lists.newArrayListWithCapacity(names.size());
        VsphereProvisioningTemplate provisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);

        for (String name : names) {
            placeholderMachines
                    .add(Machine.builder()
                            .id(name)
                            .cloudProvider(CloudProviders.VSPHERE)
                            .machineSize("unknown")
                            .region(provisioningTemplate.getResourcePool())
                            .machineState(MachineState.PENDING).launchTime(UtcTime.now()).build());
        }
        return placeholderMachines;
    }

    /**
     * Remove pending machines that are already running.
     *
     * @param machines     running machines
     * @param pendingNames names of pending machines
     */
    private void removeDoubles(List<Machine> machines, List<String> pendingNames) {
        List<String> runningNames = machines.stream().map(Machine::getId).collect(Collectors.toList());
        pendingNames.removeIf(name -> runningNames.contains(name));
    }

    /**
     * Retrieves a particular machine from the pool or throws an
     * exception if it could not be found.
     *
     * @param machineId The id of the machine of interest.
     * @return The machine with the given ID
     * @throws NotFoundException if the machine cannot be found.
     */
    private Machine getMachineOrFail(String machineId) {
        for (Machine machine : listMachines()) {
            if (machine.getId().equals(machineId)) {
                return machine;
            }
        }
        throw new NotFoundException(String.format("no machine with id '%s' found in cloud pool", machineId));
    }
}
