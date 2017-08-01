package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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

    @AfterClass
    public static void tearDown() throws InterruptedException {
        List<Machine> machines = vspherePoolDriver.listMachines();
        waitUntilRunning();
        machines.forEach(machine -> vspherePoolDriver.terminateMachine(machine.getId()));
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
        assertTrue(result.isEmpty());
    }

    @Test
    public void simpleScale() throws InterruptedException {
        vspherePoolDriver.configure(driverConfig);
        List<Machine> result = vspherePoolDriver.listMachines();
        assertTrue(result.isEmpty());

        result = vspherePoolDriver.startMachines(1);
        assertThat(result.size(), is(1));

        result = vspherePoolDriver.listMachines();
        assertThat(result.size(), is(1));

        waitUntilRunning();

        vspherePoolDriver.terminateMachine(result.get(0).getId());
        result = vspherePoolDriver.listMachines();
        assertTrue(result.isEmpty());
    }

    @Test
    public void scaleMore() throws InterruptedException {
        vspherePoolDriver.configure(driverConfig);

        // Scale 0 -> 1
        List<Machine> result = vspherePoolDriver.startMachines(1);
        assertThat(result.size(), is(1));
        // Check size
        result = vspherePoolDriver.listMachines();
        assertThat(result.size(), is(1));
        // Scale 1 -> 3
        result = vspherePoolDriver.startMachines(2);
        assertThat(result.size(), is(2));

        result = vspherePoolDriver.listMachines();
        assertThat(result.size(), is(3));

        waitUntilRunning();

        // Scale 3 -> 1
        vspherePoolDriver.terminateMachine(result.get(0).getId());
        vspherePoolDriver.terminateMachine(result.get(1).getId());
        result = vspherePoolDriver.listMachines();
        assertThat(result.size(), is(1));
        // Scale 1 -> 0
        vspherePoolDriver.terminateMachine(result.get(0).getId());
        result = vspherePoolDriver.listMachines();
        assertTrue(result.isEmpty());
    }

    private static void waitUntilRunning() throws InterruptedException {
        boolean running;
        do {
            running = true;
            for (Machine machine : vspherePoolDriver.listMachines()) {
                if (!machine.getMachineState().equals(MachineState.RUNNING)) {
                    running = false;
                    sleep(10);
                }
            }
        } while (!running);
    }

}
