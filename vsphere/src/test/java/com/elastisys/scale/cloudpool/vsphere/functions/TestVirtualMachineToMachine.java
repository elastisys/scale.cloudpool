package com.elastisys.scale.cloudpool.vsphere.functions;

import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestVirtualMachineToMachine {

    private String region = "region";
    private DateTime launchTime = UtcTime.now();
    private int numCpu = 2;
    private int ram = 1024;
    private String machineSize = String.format("cpu-%d-mem-%d", numCpu, ram);
    private VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void convertSimpleVm() throws RemoteException {
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

    @Test
    public void convertVmWithIp() throws RemoteException {
        String name = "vm1";
        String publicIp = "192.168.104.230";

        VirtualMachine vm = new MockedVm().withName(name)
                .withLaunchTime(launchTime)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .withPublicIps(publicIp)
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
        assertThat(result.getPublicIps().size(), is(1));
        assertThat(result.getPublicIps().get(0), is(publicIp));
        assertThat(result.getPrivateIps().size(), is(0));
    }

    @Test
    public void convertWithGuestInfoButNullIp() throws RemoteException {
        String name = "vm1";
        VirtualMachine vm = new MockedVm().withName(name)
                .withLaunchTime(launchTime)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        // Mock guest info in order to detect a specific bug where
        // the ip address "null" is returned
        GuestInfo guestInfo = mock(GuestInfo.class);
        when(vm.getGuest()).thenReturn(guestInfo);

        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getPublicIps().size(), is(0));
        assertThat(result.getPrivateIps().size(), is(0));
    }

    @Test
    public void withoutLaunchTime() throws RemoteException {
        String name = "vm1";
        VirtualMachine vm = new MockedVm().withName(name)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getLaunchTime(), is(nullValue()));
    }

    @Test
    public void poweredOffOrSuspended() throws RemoteException {
        String name1 = "vmOff";
        String name2 = "vmSuspended";
        VirtualMachinePowerState poweredOff = VirtualMachinePowerState.poweredOff;
        VirtualMachinePowerState suspended = VirtualMachinePowerState.suspended;

        VirtualMachine vmOff = new MockedVm().withName(name1)
                .withPowerState(poweredOff)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        VirtualMachine vmSuspended = new MockedVm().withName(name2)
                .withPowerState(suspended)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        Machine result = new VirtualMachineToMachine().apply(vmOff);
        assertThat(result.getMachineState(), is(MachineState.TERMINATED));
        result = new VirtualMachineToMachine().apply(vmSuspended);
        assertThat(result.getMachineState(), is(MachineState.RUNNING));
    }

    @Test
    public void unableToGetRegion() throws RemoteException {
        String name = "vm1";
        VirtualMachine vm = new MockedVm().withName(name)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        when(vm.getResourcePool()).thenThrow(RemoteException.class);
        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getRegion(), is("unknown"));
    }

    @Test
    public void requestedMachine() throws RemoteException {
        String name = "vm1";
        VirtualMachine vm = new MockedVm().withName(name)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();

        when(vm.getGuest()).thenReturn(null);
        when(vm.getRuntime()).thenReturn(null);
        when(vm.getConfig()).thenReturn(null);

        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getId(), is(name));
        assertThat(result.getMachineSize(), is("unknown"));

        VirtualMachineConfigInfo configInfo = mock(VirtualMachineConfigInfo.class);
        when(configInfo.getHardware()).thenReturn(null);
        when(vm.getConfig()).thenReturn(configInfo);

        result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getId(), is(name));
        assertThat(result.getMachineSize(), is("unknown"));
    }

    @Test
    public void convertTerminatingMachine() {
        VirtualMachine vm = new MockedVm().build();
        when(vm.getName()).thenThrow(new RuntimeException("ManagedObjectNotFound"));

        Machine result = new VirtualMachineToMachine().apply(vm);
        assertThat(result.getId(), is("unknown"));
        assertThat(result.getLaunchTime(), is(nullValue()));
        assertThat(result.getMachineState(), is(MachineState.TERMINATED));
        assertThat(result.getCloudProvider(), is(CloudProviders.VSPHERE));
        assertThat(result.getRegion(), is("unknown"));
        assertThat(result.getMachineSize(), is("unknown"));
        assertTrue(result.getPublicIps().isEmpty());
        assertTrue(result.getPrivateIps().isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void failingConvertion() {
        VirtualMachine vm = new MockedVm().build();
        when(vm.getName()).thenThrow(new RuntimeException("Strange exception"));
        new VirtualMachineToMachine().apply(vm);
    }

}
