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
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class StandardVsphereClient implements VsphereClient {

    Logger logger = LoggerFactory.getLogger(StandardVsphereClient.class);

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private VsphereApiSettings vsphereApiSettings;
    private VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private ServiceInstance serviceInstance;
    private CustomAttributeTagger tagger;

    private ConcurrentHashSet<FutureNamePair> pendingMachines = new ConcurrentHashSet<>();

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

        // array is useful here as some ManagedEntities may be in a volatile state (under termination by vcenter)
        for (int i = 0; i < meArr.length; i++) {
            try {
                ManagedEntity me = meArr[i];
                if (hasAllTags(me, tags)) {
                    vms.add((VirtualMachine) me);
                }
            } catch (RuntimeException e) {
                if (!e.getMessage().contains("ManagedObjectNotFound")) {
                    throw e;
                }
                logger.debug("ManagedObjectNotFound: an object in getVirtualMachines was already removed");
            }
        }

        return vms;
    }

    public List<String> pendingVirtualMachines() {
        pendingMachines.removeIf(futureNamePair -> futureNamePair.future.isDone());
        return pendingMachines.stream().map(FutureNamePair::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> launchVirtualMachines(int count, List<Tag> tags) throws RemoteException {
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

        // create CloneTasks
        List<String> names = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            String name = UUID.randomUUID().toString();
            names.add(name);
            Task task = template.cloneVM_Task(folder, name, cloneSpec);
            CloneTask cloneTask = new CloneTask(tagger, task, tags);
            pendingMachines.add(new FutureNamePair(executor.submit(cloneTask), name));
        }
        return names;
    }

    @Override
    public void terminateVirtualMachines(List<String> ids) throws RemoteException {
        Folder rootFolder = serviceInstance.getRootFolder();
        //List<ManagedEntity> managedEntities = Arrays.asList(createInventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine"));

        List<VirtualMachine> virtualMachines = Lists.newArrayList();
        for(String id : ids) {
            VirtualMachine vm = (VirtualMachine) searchManagedEntity(rootFolder, "VirtualMachine", id);
            virtualMachines.add(vm);
        }

        for (VirtualMachine vm : virtualMachines) {

            if (ids.contains(vm.getName())) {
                executor.submit(createDestroyTask(vm));
            }
        }
    }

    @Override
    public void tag(String id, List<Tag> tags) {

    }

    @Override
    public void untag(String id, List<Tag> tags) {

    }

    DestroyTask createDestroyTask(VirtualMachine virtualMachine) {
        return new DestroyTask(virtualMachine);
    }

    ServiceInstance createServiceInstance(URL url, String username, String password, boolean ignoreSsl) throws
            MalformedURLException, RemoteException {
        return new ServiceInstance(url, username, password, ignoreSsl);
    }

    InventoryNavigator createInventoryNavigator(Folder folder) {
        return new InventoryNavigator(folder);
    }

    ManagedEntity searchManagedEntity(Folder folder, String type, String name) throws RemoteException {
        ManagedEntity me = createInventoryNavigator(folder).searchManagedEntity(type, name);
        if (me == null) {
            throw new NotFoundException("Entity: " + type + " Name: \"" + name + "\" not found");
        }
        return me;
    }

    private boolean hasAllTags(ManagedEntity me, List<Tag> tags) throws RemoteException {
        for (Tag tag : tags) {
            if (!tagger.isTagged(me, tag)) {
                return false;
            }
        }
        return true;
    }

}