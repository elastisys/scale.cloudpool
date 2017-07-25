package com.elastisys.scale.cloudpool.vsphere.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.ResourcePool;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.Locale;

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

        VirtualMachine vm = getMockedVM(name);
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

    private VirtualMachine getMockedVM(String name) throws RemoteException {
        VirtualMachineRuntimeInfo runtimeInfo = mock(VirtualMachineRuntimeInfo.class);
        ResourcePool resourcePool = mock(ResourcePool.class);
        VirtualMachineConfigInfo config = mock(VirtualMachineConfigInfo.class);
        VirtualHardware hardware = mock(VirtualHardware.class);

        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getName()).thenReturn(name);

        when(vm.getRuntime()).thenReturn(runtimeInfo);
        when(runtimeInfo.getBootTime()).thenReturn(launchTime.toCalendar(Locale.ENGLISH));
        when(runtimeInfo.getPowerState()).thenReturn(poweredOn);

        when(vm.getResourcePool()).thenReturn(resourcePool);
        when(resourcePool.getName()).thenReturn(region);

        when(vm.getConfig()).thenReturn(config);
        when(config.getHardware()).thenReturn(hardware);
        when(hardware.getNumCPU()).thenReturn(numCpu);
        when(hardware.getMemoryMB()).thenReturn(ram);
        return vm;
    }
}
