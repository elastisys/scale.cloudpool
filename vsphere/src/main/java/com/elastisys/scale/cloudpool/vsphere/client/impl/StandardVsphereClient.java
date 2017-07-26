package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tagger.TaggerFactory;
import com.elastisys.scale.cloudpool.vsphere.tagger.impl.CustomAttributeTagger;
import com.google.common.collect.Lists;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class StandardVsphereClient implements VsphereClient {

    Logger logger = LoggerFactory.getLogger(StandardVsphereClient.class);

    private VsphereApiSettings vsphereApiSettings;
    private VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private ServiceInstance serviceInstance;
    private CustomAttributeTagger tagger;

    @Override
    public void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate)
            throws RemoteException {
        this.vsphereApiSettings = vsphereApiSettings;
        this.vsphereProvisioningTemplate = vsphereProvisioningTemplate;
        try {
            this.serviceInstance = createServiceInstance(this.vsphereApiSettings.getUrl(),
                    this.vsphereApiSettings.getUsername(), this.vsphereApiSettings.getPassword(), true);
        } catch (MalformedURLException e) {
            throw new RemoteException();
        }
        this.tagger = (CustomAttributeTagger) TaggerFactory.getTagger();
        tagger.initialize(serviceInstance);
    }

    @Override
    public List<VirtualMachine> getVirtualMachines(List<Tag> tags) throws RemoteException {
        Folder rootFolder = serviceInstance.getRootFolder();
        List<VirtualMachine> vms = Lists.newArrayList();
        ManagedEntity[] meArr = createInventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        if (meArr == null) {
            return vms;
        }
        List<ManagedEntity> meList = Arrays.asList(meArr);
        for (ManagedEntity me : meList) {
            boolean addMe = true;
            for (Tag tag : tags) {
                if (!tagger.isTagged(me, tag)) {
                    addMe = false;
                    break;
                }
            }
            if (addMe) {
                vms.add((VirtualMachine) me);
            }
        }
        return vms;
    }

    @Override
    public List<VirtualMachine> launchVirtualMachines(int count, List<Tag> tags) throws RemoteException {

        Folder rootFolder = this.serviceInstance.getRootFolder();
        VirtualMachine template = (VirtualMachine) searchManagedEntity(rootFolder, VirtualMachine.class.getSimpleName(),
                vsphereProvisioningTemplate.getTemplate());
        Folder folder = (Folder) searchManagedEntity(rootFolder, Folder.class.getSimpleName(),
                vsphereProvisioningTemplate.getFolder());
        ResourcePool resourcePool = (ResourcePool) searchManagedEntity(rootFolder, ResourcePool.class.getSimpleName(),
                vsphereProvisioningTemplate.getResourcePool());
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setPool(resourcePool.getMOR());
        cloneSpec.setLocation(relocateSpec);
        cloneSpec.setTemplate(false);
        cloneSpec.setPowerOn(true);

        // Initiate cloning tasks on the Vcenter server
        List<Task> tasks = Lists.newArrayList();
        List<String> names = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            String name = UUID.randomUUID().toString();
            names.add(name);
            tasks.add(template.cloneVM_Task(folder, name, cloneSpec));
        }

        // Wait for the Vcenter server to finish cloning VirtualMachines
        for (Task task : tasks) {
            try {
                task.waitForTask();
            } catch (InterruptedException e) {
                logger.error("Failed to create new VirtualMachine: cloneVM_task was interrupted");
            }
        }

        // Find the references to created VirtualMachines in order to return them
        List<VirtualMachine> vms = Lists.newArrayList();
        for(String name : names) {
            vms.add((VirtualMachine) searchManagedEntity(folder, VirtualMachine.class.getSimpleName(), name));
        }

        return vms;
    }

    @Override
    public void terminateVirtualMachines(List<String> ids) {
    }

    @Override
    public void tag(String id, List<Tag> tags) {

    }

    @Override
    public void untag(String id, List<Tag> tags) {

    }

    ServiceInstance createServiceInstance(URL url, String username, String password, boolean ignoreSsl) throws
            MalformedURLException, RemoteException {
        return new ServiceInstance(url, username, password, ignoreSsl);
    }

    InventoryNavigator createInventoryNavigator(Folder folder) {
        return new InventoryNavigator(folder);
    }

    ManagedEntity searchManagedEntity(Folder folder, String type, String name) throws RemoteException {
        ManagedEntity me = createInventoryNavigator(folder).searchManagedEntity(type,
                vsphereProvisioningTemplate.getTemplate());
        if (me == null) {
            throw new NotFoundException("Entity: " + type + " Name: " + name + " not found");
        }
        return me;
    }

}