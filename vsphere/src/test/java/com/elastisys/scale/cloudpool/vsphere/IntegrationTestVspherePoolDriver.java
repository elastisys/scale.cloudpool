package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class IntegrationTestVspherePoolDriver {

    private static VspherePoolDriver vspherePoolDriver;
    private DriverConfig driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("myconfig.json"), DriverConfig.class);

    @BeforeClass
    public static void setUpBeforeClass() {
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
    public void testListMachines() {
        vspherePoolDriver.configure(driverConfig);
        List<Machine> result = vspherePoolDriver.listMachines();
        assertTrue(result.isEmpty());
    }

}
