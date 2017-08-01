package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
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
import com.vmware.vim25.mo.VirtualMachine;
import jersey.repackaged.com.google.common.collect.Lists;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class VspherePoolDriver implements CloudPoolDriver {

    private VsphereClient vsphereClient;
    private DriverConfig driverConfig;

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
        try {
            List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(cloudPoolTag());
            return Lists.transform(virtualMachines, new VirtualMachineToMachine());
        } catch (RemoteException e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\": %s", getPoolName(), e.getMessage()), e);
        }
    }

    @Override
    public List<Machine> startMachines(int count) throws IllegalStateException, StartMachinesException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
        List<Machine> startedMachines = Lists.newArrayListWithCapacity(count);
        try {
            List<VirtualMachine> newVms = vsphereClient.launchVirtualMachines(count, cloudPoolTag());
            startedMachines.addAll(Lists.transform(newVms, new VirtualMachineToMachine()));
        } catch (RemoteException|NotFoundException e) {
            throw new StartMachinesException(count, startedMachines, e);
        }
        return startedMachines;
    }

    @Override
    public void terminateMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured VspherePoolDriver");
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
}
