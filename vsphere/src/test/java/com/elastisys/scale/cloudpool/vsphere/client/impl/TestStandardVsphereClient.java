package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.google.common.collect.Lists;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.mo.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static com.elastisys.scale.cloudpool.vsphere.util.TestUtils.loadDriverConfig;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestStandardVsphereClient {

    private static StandardVsphereClient vsphereClient;
    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;

    private static ServiceInstance mockServiceInstance;
    private static InventoryNavigator mockInventoryNavigator;
    private static CustomFieldsManager mockCustomFieldsManager;
    private VirtualMachine template;
    private static Folder root;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DriverConfig driverConfig = loadDriverConfig("config/valid-vsphere-config.json");
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
    }

    @Before
    public void setUp() throws Exception {
        vsphereClient = spy(new StandardVsphereClient());
        template = mock(VirtualMachine.class);
        mockServiceInstance = mock(ServiceInstance.class);
        mockCustomFieldsManager = mock(CustomFieldsManager.class);
        mockInventoryNavigator = mock(InventoryNavigator.class);
        root = mock(Folder.class);

        doReturn(mockServiceInstance).when(vsphereClient)
                .createServiceInstance(any(URL.class), anyString(), anyString(), anyBoolean());
        when(mockServiceInstance.getCustomFieldsManager()).thenReturn(mockCustomFieldsManager);
        when(mockCustomFieldsManager.getField()).thenReturn(new CustomFieldDef[0]);
        doReturn(mockInventoryNavigator).when(vsphereClient).createInventoryNavigator(any(Folder.class));
        doReturn(root).when(mockServiceInstance).getRootFolder();
        // Make it possible to search for template, folder and resource pool defined in the settings.
        doReturn(template).when(vsphereClient).searchManagedEntity(root, VirtualMachine.class.getSimpleName(),
                vsphereProvisioningTemplate.getTemplate());
        doReturn(mock(Folder.class)).when(vsphereClient)
                .searchManagedEntity(root, Folder.class.getSimpleName(), vsphereProvisioningTemplate.getFolder());
        doReturn(mock(ResourcePool.class)).when(vsphereClient)
                .searchManagedEntity(root, ResourcePool.class.getSimpleName(),
                        vsphereProvisioningTemplate.getResourcePool());
    }

    @Test
    public void testConfigure() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    // URL is already checked when we get to the client, but we want coverage :P
    @Test(expected = RemoteException.class)
    public void configureWithMalformedUrl()
            throws RemoteException, MalformedURLException {
        doThrow(MalformedURLException.class).when(vsphereClient)
                .createServiceInstance(any(), anyString(), anyString(), anyBoolean());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    @Test
    public void getNoMachines() throws RemoteException {
        ManagedEntity[] vms = null;
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertEquals(virtualMachines.size(), 0);
    }

    @Test
    public void listMachinesShouldReturnListOfVms() throws Exception {
        ManagedEntity[] vms = {new MockedVm().build()};
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertEquals(virtualMachines.size(), 1);
    }

    @Test
    public void getMachinesWithWrongTag() throws RemoteException {
        Tag existingTag = new VsphereTag(ScalingTag.CLOUD_POOL, "TestCloudPool");
        ManagedEntity[] vms = {new MockedVm().withTag(existingTag).build()};
        Tag noSuchTag = new VsphereTag(ScalingTag.CLOUD_POOL, "NoSuchThing");
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient
                .getVirtualMachines(Lists.newArrayList(existingTag, noSuchTag));
        assertEquals(virtualMachines.size(), 0);
    }

    @Test
    public void launchMachinesShouldAddVms() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<Tag> tags = Lists.newArrayList();
        Tag tag = new VsphereTag(ScalingTag.CLOUD_POOL, "StandardVsphereClientTestPool");
        tags.add(tag);

        when(template.cloneVM_Task(any(), anyString(), any())).thenReturn(mock(Task.class));

        List<String> names = vsphereClient.launchVirtualMachines(2, tags);
        verify(template, times(2)).cloneVM_Task(any(Folder.class), anyString(), any(VirtualMachineCloneSpec.class));
        assertEquals(names.size(), 2);
    }

    @Test
    public void interruptedLaunchOfMachine() throws RemoteException, InterruptedException {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        Task task = mock(Task.class);

        when(template.cloneVM_Task(any(), anyString(), any())).thenReturn(task);
        when(task.waitForTask()).thenThrow(InterruptedException.class);

        vsphereClient.launchVirtualMachines(1, Lists.newArrayList());
    }

    @Test(expected = NotFoundException.class)
    public void launchWithWrongTemplate() throws RemoteException {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);

        Folder root = mockServiceInstance.getRootFolder();
        doThrow(NotFoundException.class).when(vsphereClient)
                .searchManagedEntity(root, VirtualMachine.class.getSimpleName(),
                        vsphereProvisioningTemplate.getTemplate());

        vsphereClient.launchVirtualMachines(1, Lists.newArrayList());
    }

    @Test
    public void terminateMachinesShouldRemoveVms() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        String name = "Vm_destroy";
        VirtualMachine virtualMachine = new MockedVm().withName(name).build();

        doReturn(virtualMachine).when(vsphereClient)
                .searchManagedEntity(root, VirtualMachine.class.getSimpleName(), name);
        when(virtualMachine.powerOffVM_Task()).thenReturn(mock(Task.class));
        when(virtualMachine.destroy_Task()).thenReturn(mock(Task.class));

        vsphereClient.terminateVirtualMachines(Arrays.asList(name));
        verify(vsphereClient, times(1)).createDestroyTask(virtualMachine);
    }

    /**
     * This test does not test much, just that the client does not crash in case of
     * interruptions when trying to terminate a machine.
     */
    @Test
    public void interruptedPowerOff() throws RemoteException, InterruptedException {
        String name = "Vm_destroy";
        VirtualMachine vm = new MockedVm().withName(name).build();
        Task powerOffTask = mock(Task.class);
        Task destroyTask = mock(Task.class);

        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);

        doReturn(vm).when(vsphereClient).searchManagedEntity(root, VirtualMachine.class.getSimpleName(), name);
        when(vm.powerOffVM_Task()).thenReturn(powerOffTask);
        when(vm.destroy_Task()).thenReturn(destroyTask);

        when(powerOffTask.waitForTask()).thenThrow(InterruptedException.class);
        vsphereClient.terminateVirtualMachines(Arrays.asList(name));
        reset(powerOffTask);

        when(destroyTask.waitForTask()).thenThrow(InterruptedException.class);
        vsphereClient.terminateVirtualMachines(Arrays.asList(name));
    }

    @Test
    public void tryListTerminatingMachine() throws RemoteException {
        Tag existingTag = new VsphereTag(ScalingTag.CLOUD_POOL, "TestCloudPool");
        VirtualMachine terminatingVm = new MockedVm().withTag(existingTag).build();
        ManagedEntity[] vms = {terminatingVm};

        when(terminatingVm.getCustomValue()).thenThrow(new RuntimeException("ManagedObjectNotFound"));
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());

        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList(existingTag));

        verify(terminatingVm, times(1)).getCustomValue();
        assertTrue(virtualMachines.isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void failToListMachines() throws RemoteException {
        Tag existingTag = new VsphereTag(ScalingTag.CLOUD_POOL, "TestCloudPool");
        VirtualMachine terminatingVm = new MockedVm().withTag(existingTag).build();
        ManagedEntity[] vms = {terminatingVm};

        when(terminatingVm.getCustomValue()).thenThrow(new RuntimeException("NotTheExceptionYourWereLookingFor"));
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());

        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        vsphereClient.getVirtualMachines(Lists.newArrayList(existingTag));
    }

    @Test
    public void listMachinesWithOneTerminating() throws RemoteException {
        Tag existingTag = new VsphereTag(ScalingTag.CLOUD_POOL, "TestCloudPool");
        VirtualMachine terminatingVm = new MockedVm().withTag(existingTag).withName("terminatingVm").build();
        VirtualMachine normalVm = new MockedVm().withTag(existingTag).withName("normalVm").build();
        ManagedEntity[] vms = {terminatingVm, normalVm};

        when(terminatingVm.getCustomValue()).thenThrow(new RuntimeException("ManagedObjectNotFound"));
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());

        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList(existingTag));

        assertThat(virtualMachines.size(), is(1));
        assertThat(virtualMachines.get(0).getName(), is("normalVm"));
    }

    @Test
    public void searchForExistingEntity() throws RemoteException {
        VirtualMachine virtualMachine = new MockedVm().withName("vm1").build();
        when(mockInventoryNavigator.searchManagedEntity("VirtualMachine", "vm1")).thenReturn(virtualMachine);
        ManagedEntity me = vsphereClient.searchManagedEntity(root, "VirtualMachine", "vm1");
        assertThat(me, is(virtualMachine));
    }

    @Test(expected = NotFoundException.class)
    public void searchForMissingEntity() throws RemoteException {
        when(mockInventoryNavigator.searchManagedEntity("VirtualMachine", "vm1")).thenReturn(null);
        vsphereClient.searchManagedEntity(root, "VirtualMachine", "vm1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedOperationTag() {
        vsphereClient.tag("vm1", Lists.newArrayList());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedOperationUntag() {
        vsphereClient.untag("vm1", Lists.newArrayList());
    }

    @Test
    public void testPendingMachines() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        CloneTask t = mock(CloneTask.class);
        doReturn(t).when(vsphereClient).createCloneTask(any(), any(), any());
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                try {
                    while (true) {
                        sleep(1);
                    }
                } catch (Exception e) {
                    fail("Mocked CloneTask callback failed to block");
                }
                return null;
            }
        }).when(t).call();
        List<String> virtualMachines = vsphereClient.launchVirtualMachines(1, Lists.newArrayList());
        List<String> pending = vsphereClient.pendingVirtualMachines();
        assertThat(virtualMachines, is(pending));
        assertThat(pending.size(), is(1));
    }

}
