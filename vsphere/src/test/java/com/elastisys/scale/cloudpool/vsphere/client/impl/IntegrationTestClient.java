package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class IntegrationTestClient {

    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private static VsphereClient vsphereClient;

    private static String testTagValue = "VsphereClientIntegrationTest";
    private static int terminationTimeout = 5;

    @BeforeClass
    public static void setUpBeforeClass() {
        DriverConfig driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("myconfig.json"),
                DriverConfig.class);
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, testTagValue));
        while (!vsphereClient.pendingVirtualMachines().isEmpty()) {
            sleep(100);
        }
        List<VirtualMachine> machines = vsphereClient.getVirtualMachines(tags);
        vsphereClient
                .terminateVirtualMachines(machines.stream().map(VirtualMachine::getName).collect(Collectors.toList()));
        while (!vsphereClient.getVirtualMachines(tags).isEmpty()) {
            sleep(100);
        }
    }

    @Before
    public void setUp() throws RemoteException {
        vsphereClient = new StandardVsphereClient();
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    @Test
    public void shouldNotGetVirtualMachinesForTagThatDoesNotExist() throws RemoteException {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, "NoSuchPool"));
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(tags);
        assertTrue(virtualMachines.isEmpty());
    }

    @Test
    public void shouldGetVirtualMachineWithoutTagRequirements() throws RemoteException {
        // this test assumes that there is at least one virtual machine or
        // template ManagedEntity on the server
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertFalse(virtualMachines.isEmpty());
    }

    @Test
    public void shouldLaunchNewMachines() throws RemoteException {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, testTagValue));
        List<String> names = vsphereClient.launchVirtualMachines(1, tags);
        assertEquals(names.size(), 1);
    }

    @Test
    public void shouldDestroyMachines() throws Exception {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, testTagValue));
        int startingSize = vsphereClient.getVirtualMachines(tags).size();
        List<String> names = vsphereClient.launchVirtualMachines(1, tags);
        while (!vsphereClient.pendingVirtualMachines().isEmpty()) {
            sleep(100);
        }
        assertEquals(vsphereClient.getVirtualMachines(tags).size(), (startingSize + 1));
        vsphereClient.terminateVirtualMachines(names);
        int numberOfMsWaited = 0;
        while (vsphereClient.getVirtualMachines(tags).size() != startingSize) {
            sleep(100);
            numberOfMsWaited += 100;
            if (numberOfMsWaited > 1000 * terminationTimeout) {
                fail("Termination took too long");
            }
        }
    }
}
