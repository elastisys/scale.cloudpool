package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestVspherePoolDriverOperations {

    private VspherePoolDriver driver;
    private VsphereClient mockedClient = mock(VsphereClient.class);

    @Before
    public void setup() {
        String specificConfigPath = "config/valid-vsphere-config.json";
        DriverConfig configuration = TestUtils.loadDriverConfig(specificConfigPath);
        driver = new VspherePoolDriver(mockedClient);
        driver.configure(configuration);
    }

    @Test
    public void testVspherePoolDriver() {
        fail("Not yet implemented");
    }

    @Test
    public void emptyListOfMachines() {
        assertTrue(driver.listMachines().isEmpty());
    }

    @Test
    public void listSingleMachine() throws RemoteException {
        String name = "vmName";
        List<VirtualMachine> vms = Lists.newArrayList(getMockedVM(name));
        when(mockedClient.getVirtualMachines(any())).thenReturn(vms);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        assertEquals(1, result.size());
        assertThat(result, is(MachinesMatcher.machines(name)));
    }

    @Test
    public void listMoreMachines() throws RemoteException {
        List<String> names = Lists.newArrayList("vm1", "vm2", "vm3");
        List<VirtualMachine> vms = Lists.newArrayList();

        vms.addAll(getMockedVMs(names));
        when(mockedClient.getVirtualMachines(any())).thenReturn(vms);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        assertEquals(names.size(), result.size());
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test
    public void listMachinesWithDifferentStates() throws RemoteException {
        List<String> names = Lists.newArrayList("vmOn", "vmOff");
        List<VirtualMachine> vms = Lists.newArrayList();
        VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;
        VirtualMachinePowerState poweredOff = VirtualMachinePowerState.poweredOff;

        VirtualMachine vmOn = getMockedVM(names.get(0));
        VirtualMachine vmOff = getMockedVM(names.get(1));

        when(vmOn.getRuntime().getPowerState()).thenReturn(poweredOn);
        when(vmOff.getRuntime().getPowerState()).thenReturn(poweredOff);
        vms.add(vmOn);
        vms.add(vmOff);

        when(mockedClient.getVirtualMachines(any())).thenReturn(vms);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        assertEquals(names.size(), result.size());
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test(expected = CloudPoolDriverException.class)
    public void failToGetMachines() throws CloudPoolDriverException, RemoteException {
        when(this.mockedClient.getVirtualMachines(any())).thenThrow(new RemoteException("API unreachable"));
        driver.listMachines();
    }

    @Test
    public void startSingleMachine() throws RemoteException {
        int count = 1;
        List<String> names = Lists.newArrayList("vm1");
        List<VirtualMachine> vms = getMockedVMs(names);
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenReturn(names);

        List<Machine> result = driver.startMachines(count);
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test
    public void startTwoMachines() throws RemoteException {
        int count = 2;
        List<String> names = Lists.newArrayList("vm1", "vm2");
        List<VirtualMachine> vms = getMockedVMs(names);
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenReturn(names);

        List<Machine> result = driver.startMachines(count);
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test(expected = StartMachinesException.class)
    public void failToStartMachines() throws RemoteException {
        int count = 2;
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenThrow(RemoteException.class);

        driver.startMachines(count);
    }

    @Test
    public void terminateMachine() throws RemoteException {
        String name = "vm1";
        driver.terminateMachine(name);
        verify(mockedClient).terminateVirtualMachines(Lists.newArrayList(name));
        verify(mockedClient, times(1)).terminateVirtualMachines(any());
    }

    @Test(expected = CloudPoolDriverException.class)
    public void failToTerminateMachine() throws RemoteException {
        doThrow(RemoteException.class).when(mockedClient).terminateVirtualMachines(any());
        driver.terminateMachine("vm1");
    }

    @Test
    public void testAttachMachine() {
        fail("Not yet implemented");
    }

    @Test
    public void testDetachMachine() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetServiceState() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetMembershipStatus() {
        fail("Not yet implemented");
    }

    private VirtualMachine getMockedVM(String name) throws RemoteException {
        String region = "region";
        DateTime launchTime = UtcTime.now();
        int numCpu = 2;
        int ram = 1024;
        String machineSize = String.format("%dvCPU %dMB RAM", numCpu, ram);
        VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;

        VirtualMachine vm = new MockedVm().withName(name)
                .withLaunchTime(launchTime)
                .withPowerState(poweredOn)
                .withResourcePool(region)
                .withMachineSize(machineSize)
                .build();
        return vm;
    }

    private List<VirtualMachine> getMockedVMs(String... names) throws RemoteException {
        return getMockedVMs(Arrays.asList(names));
    }

    private List<VirtualMachine> getMockedVMs(List<String> names) throws RemoteException {
        List<VirtualMachine> vms = Lists.newArrayListWithCapacity(names.size());
        for (String name : names) {
            vms.add(getMockedVM(name));
        }
        return vms;
    }
}
