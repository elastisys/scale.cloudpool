package com.elastisys.scale.cloudpool.vsphere.tagger.impl;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Sets;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class IntegrationTestTagger {

    private static DriverConfig driverConfig;
    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;

    private static ServiceInstance serviceInstance;
    private static VirtualMachine minimalVm;

    private static CustomAttributeTagger tagger;

    private static String testDef = "NoSuchTag";

    @BeforeClass
    public static void setup() throws Exception {
        driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("myconfig.json"), DriverConfig.class);
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
    }

    @AfterClass
    public static void teardown() throws Exception {
        Task task = minimalVm.destroy_Task();
        task.waitForTask();
        CustomFieldsManager customFieldsManager = serviceInstance.getCustomFieldsManager();
        CustomFieldDef[] cfdArr = customFieldsManager.getField();
        int key = -1;
        for(CustomFieldDef cfd : cfdArr) {
            if (cfd.getName().equals(testDef)){
                key = cfd.getKey();
            }
        }
        if(key != -1) {
            customFieldsManager.removeCustomFieldDef(key);
        }
        else {
            System.err.println("IntegrationTestTagger failed to remove CustomAttribute testing definition");
        }
        serviceInstance.getServerConnection().logout();
    }

    @Test
    public void taggedResourceShouldBeTagged() throws RemoteException {
        Tag tag = new VsphereTag(ScalingTag.CLOUD_POOL, "MyCloudPool");
        tagger.tag(minimalVm, tag);
        assertTrue(tagger.isTagged(minimalVm, tag));
        tagger.untag(minimalVm, tag);
        assertFalse(tagger.isTagged(minimalVm, tag));
    }

    @Test
    public void testTaggerInitialization() throws RemoteException {
        CustomAttributeTagger spyCustomAttributeTagger = spy(new CustomAttributeTagger());
        Collection<String> mockTags = Sets.newHashSet();
        mockTags.add(testDef);
        when(spyCustomAttributeTagger.getTags()).thenReturn(mockTags);
        spyCustomAttributeTagger.initialize(serviceInstance);

        CustomFieldsManager customFieldsManager = serviceInstance.getCustomFieldsManager();
        List<CustomFieldDef> cfdList = Arrays.asList(customFieldsManager.getField());
        List<String> tagDefinitions = cfdList.stream().map(CustomFieldDef::getName).collect(Collectors.toList());
        for(String tag : mockTags) {
            if(!tagDefinitions.contains(tag)) {
                fail("Expected to find tag definition");
            }
        }
    }

}
