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

import java.rmi.RemoteException;
import java.util.List;

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
        return null;
    }

    @Override
    public List<Machine> startMachines(int count) throws IllegalStateException, StartMachinesException {
        return null;
    }

    @Override
    public void terminateMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void attachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void detachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public String getPoolName() throws IllegalStateException {
        return null;
    }

    public boolean isConfigured() {
        return driverConfig != null;
    }
}
