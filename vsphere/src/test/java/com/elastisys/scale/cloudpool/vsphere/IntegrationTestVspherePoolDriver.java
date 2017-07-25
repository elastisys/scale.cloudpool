package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class IntegrationTestVspherePoolDriver {

    private static VspherePoolDriver vspherePoolDriver;

    @BeforeClass
    public static void setUpBeforeClass() {
        VsphereClient vsphereClient = new StandardVsphereClient();
        vspherePoolDriver = new VspherePoolDriver(vsphereClient);
    }

    @Test
    public void testConfiguration() {
        DriverConfig driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("VcenterInfo.json"), DriverConfig.class);
        vspherePoolDriver.configure(driverConfig);
        assertTrue(vspherePoolDriver.isConfigured());
    }

    @Test
    public void testListMachines() {
        DriverConfig driverConfig = TestUtils.loadDriverConfig("VcenterInfo.json");
        vspherePoolDriver.configure(driverConfig);
        System.err.println("testListMachines driverConfig.getPoolName(): " + driverConfig.getPoolName());
        List<Machine> listMachines = vspherePoolDriver.listMachines();
        System.err.println("listMachines.size: " + listMachines.size());
        for(Machine machine : listMachines) {
            System.err.println("machine: " + machine);
        }
        //System.err.println(vspherePoolDriver.listMachines());
    }

}
