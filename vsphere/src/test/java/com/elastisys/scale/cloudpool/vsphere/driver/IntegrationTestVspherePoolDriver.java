package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class IntegrationTestVspherePoolDriver {

    private static VspherePoolDriver vspherePoolDriver;
    private DriverConfig driverConfig = TestUtils.loadDriverConfig("myconfig.json");

    @Before
    public void setUp() {
        VsphereClient vsphereClient = new StandardVsphereClient();
        vspherePoolDriver = new VspherePoolDriver(vsphereClient);
    }

    @Test
    public void testConfiguration() {
        vspherePoolDriver.configure(driverConfig);
        assertTrue(vspherePoolDriver.isConfigured());
    }

    @Test(expected = IllegalStateException.class)
    public void testListWithoutConfig() {
        vspherePoolDriver.listMachines();
    }

    @Test
    public void listNoMachines() {
        vspherePoolDriver.configure(driverConfig);
        List<Machine> result = vspherePoolDriver.listMachines();
        assertTrue(onlyTerminatedMachines(result));
    }

    @Test
    public void simpleScale() throws InterruptedException {
        vspherePoolDriver.configure(driverConfig);
        List<Machine> result = vspherePoolDriver.listMachines();
        assertTrue(onlyTerminatedMachines(result));

        List<Machine> started = vspherePoolDriver.startMachines(1);
        assertThat(started.size(), is(1));

        result = vspherePoolDriver.listMachines();
        assertTrue(containsAllById(result, started));

        waitUntilRunning(started);

        vspherePoolDriver.terminateMachine(started.get(0).getId());
        waitForTermination(started.get(0).getId());
        result = vspherePoolDriver.listMachines();
        assertTrue(onlyTerminatedMachines(result));
    }

    @Test
    public void scaleMore() throws InterruptedException {
        vspherePoolDriver.configure(driverConfig);

        // Scale 0 -> 1
        List<Machine> started = vspherePoolDriver.startMachines(1);
        assertThat(started.size(), is(1));
        // Check size
        List<Machine> result = vspherePoolDriver.listMachines();
        assertTrue(containsAllById(result, started));
        // Scale 1 -> 3
        started.addAll(vspherePoolDriver.startMachines(2));
        assertThat(started.size(), is(3));

        result = vspherePoolDriver.listMachines();
        assertTrue(containsAllById(result, started));

        waitUntilRunning(started);

        // Scale 3 -> 1
        vspherePoolDriver.terminateMachine(started.get(0).getId());
        vspherePoolDriver.terminateMachine(started.get(1).getId());
        waitForTermination(started.get(0).getId(), started.get(1).getId());
        result = vspherePoolDriver.listMachines();

        assertTrue(isTerminated(started.get(0), result));
        assertTrue(isTerminated(started.get(1), result));
        // Scale 1 -> 0
        vspherePoolDriver.terminateMachine(started.get(2).getId());
        waitForTermination(started.get(2).getId());
        result = vspherePoolDriver.listMachines();
        assertTrue(onlyTerminatedMachines(result));
    }

    private boolean isTerminated(Machine machine, List<Machine> machineList) {
        String id = machine.getId();
        for (Machine m : machineList) {
            if (m.getId().equals(id) && !m.getMachineState().equals(MachineState.TERMINATED)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAllById(List<Machine> superSet, List<Machine> subSet) {
        List<String> superIds = superSet.stream().map(Machine::getId).collect(Collectors.toList());
        List<String> subIds = subSet.stream().map(Machine::getId).collect(Collectors.toList());

        return superIds.containsAll(subIds);
    }

    private static void waitUntilRunning(List<Machine> startedMachines) throws InterruptedException {
        boolean running;
        do {
            running = true;
            for (Machine machine : getMachinesById(startedMachines)) {
                if (!machine.getMachineState().equals(MachineState.RUNNING)) {
                    running = false;
                    sleep(100);
                }
            }
        } while (!running);
    }

    private static List<Machine> getMachinesById(List<Machine> machines) {
        List<String> ids = machines.stream().map(Machine::getId).collect(Collectors.toList());
        List<Machine> result = Lists.newArrayList();
        for (Machine m : vspherePoolDriver.listMachines()) {
            if (ids.contains(m.getId())) {
                result.add(m);
            }
        }
        return result;
    }

    private static void waitForTermination(String... ids) throws InterruptedException {
        boolean terminated;

        do {
            terminated = true;
            List<Machine> machines = vspherePoolDriver.listMachines();
            for (String id : ids) {
                for (Machine machine : machines) {
                    if (machine.getId().equals(id) && !machine.getMachineState().equals(MachineState.TERMINATED)) {
                        terminated = false;
                    }
                }
            }
            sleep(100);
        } while (!terminated);
    }

    private boolean onlyTerminatedMachines(List<Machine> machines) {
        for (Machine machine : machines) {
            if (!machine.getMachineState().equals(MachineState.TERMINATED)) {
                return false;
            }
        }
        return true;
    }

}
