package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.client.tagger.Tagger;
import com.elastisys.scale.cloudpool.vsphere.client.tagger.impl.CustomAttributeTagger;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ITVsphere {

    private static DriverConfig driverConfig;
    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;

    private static ServiceInstance serviceInstance;
    private static VirtualMachine minimalVm;

    private static Tagger tagger;

    @BeforeClass
    public static void setup() throws Exception {
        driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("VcenterInfo.json"), DriverConfig.class);
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
        serviceInstance = new ServiceInstance(vsphereApiSettings.getUrl(), vsphereApiSettings.getUsername(),
                vsphereApiSettings.getPassword(), true);

        Folder root = serviceInstance.getRootFolder();
        VirtualMachine template = (VirtualMachine) new InventoryNavigator(root).searchManagedEntity("VirtualMachine",
                vsphereProvisioningTemplate.getTemplate());
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        ResourcePool pool = (ResourcePool) new InventoryNavigator(root)
                .searchManagedEntity(ResourcePool.class.getSimpleName(), "Resources");
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setPool(pool.getMOR());
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);
        cloneSpec.setLocation(relocateSpec);
        String cloneName = "integration-test-" + UUID.randomUUID().toString();
        Task task = template.cloneVM_Task((Folder) template.getParent(), cloneName, cloneSpec);
        task.waitForTask();
        if (task.getTaskInfo().getState().equals(Task.SUCCESS)) {
            throw new Exception();
        }
        minimalVm = (VirtualMachine) new InventoryNavigator(root).searchManagedEntity("VirtualMachine", cloneName);

        tagger = new CustomAttributeTagger();
        tagger.initialize(serviceInstance);
    }

    @AfterClass
    public static void teardown() throws Exception {
        Task task = minimalVm.destroy_Task();
        task.waitForTask();
        serviceInstance.getServerConnection().logout();
    }

    @Test
    public void taggedResourceShouldBeTagged() throws RemoteException {
        Tag tag = new VsphereTag(VsphereTag.ScalingTag.CLOUD_POOL, "MyCloudPool");
        tagger.tag(minimalVm, tag);
        assertTrue(tagger.isTagged(minimalVm, tag));
        tagger.untag(minimalVm, tag);
        assertFalse(tagger.isTagged(minimalVm, tag));
    }

    @Test
    public void clientConfigurationShouldSucceed() throws RemoteException {
        VsphereClient vsphereClient = new StandardVsphereClient();
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    @Test
    public void shouldNotGetVirtualMachinesForTagThatDoesNotExist() throws RemoteException {
        VsphereClient vsphereClient = new StandardVsphereClient();
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(VsphereTag.ScalingTag.CLOUD_POOL, "NoSuchPool"));
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(tags);
        assertTrue(virtualMachines.isEmpty());
    }

    @Test
    public void shouldGetVirtualMachineWithoutTagRequirements() throws RemoteException {
        // this test assumes that there is at least one virtual machine or template ManagedEntity on the server
        VsphereClient vsphereClient = new StandardVsphereClient();
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<Tag> tags = Lists.newArrayList();
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(tags);
        assertFalse(virtualMachines.isEmpty());
    }

}
