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

import java.util.List;

public class VspherePoolDriver implements CloudPoolDriver {

    private VsphereClient vsphereClient;

    public VspherePoolDriver(VsphereClient vsphereClient) {
        this.vsphereClient = vsphereClient;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {

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
    public void terminateMachine(String machineId) throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void attachMachine(String machineId) throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void detachMachine(String machineId) throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState) throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus) throws IllegalStateException, NotFoundException, CloudPoolDriverException {

    }

    @Override
    public String getPoolName() throws IllegalStateException {
        return null;
    }
}
