package com.elastisys.scale.cloudpool.vsphere.functions;

import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestVirtualMachineToMachine {

    private String region = "region";
    private DateTime launchTime = UtcTime.now();
    private int numCpu = 2;
    private int ram = 1024;
    private String machineSize = String.format("%dvCPU %dMB RAM", numCpu, ram);
    private VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;
    private VirtualMachinePowerState poweredOff = VirtualMachinePowerState.poweredOff;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void convertSimpleVM() throws RemoteException {
        String name = "vm1";

        VirtualMachine vm = new MockedVm().withName(name)
                .withLaunchTime(launchTime)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();
        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getId(), is(vm.getName()));
        assertThat(result.getLaunchTime(), is(launchTime));
        assertThat(result.getMachineState(), is(MachineState.RUNNING));
        assertThat(result.getCloudProvider(), is(CloudProviders.VSPHERE));
        assertThat(result.getRegion(), is(region));
        assertThat(result.getMachineSize(), is(machineSize));
        assertThat(result.getMembershipStatus(), is(MembershipStatus.defaultStatus()));
        assertThat(result.getServiceState(), is(ServiceState.UNKNOWN));
        assertThat(result.getPublicIps().size(), is(0));
        assertThat(result.getPrivateIps().size(), is(0));
    }

}
