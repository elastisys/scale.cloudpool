package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Test;

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

}
