package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tagger.TaggerFactory;
import com.elastisys.scale.cloudpool.vsphere.tagger.impl.CustomAttributeTagger;
import com.google.common.collect.Lists;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class StandardVsphereClient implements VsphereClient {

    Logger logger = LoggerFactory.getLogger(StandardVsphereClient.class);

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private VsphereApiSettings vsphereApiSettings;
    private VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private ServiceInstance serviceInstance;
    private CustomAttributeTagger tagger;

    private ConcurrentHashSet<Foo> pendingMachines = new ConcurrentHashSet<>();

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
            if (hasAllTags(me, tags)) {
                vms.add((VirtualMachine) me);
            }
        }
        return vms;
    }

    public List<String> pendingVirtualMachines(){
        for(Foo foo : pendingMachines){
            if(foo.future.isDone()) {
                pendingMachines.remove(foo);
            }
        }
        return pendingMachines.stream().map(Foo::getName).collect(Collectors.toList());
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
            CloneTask cloneTask = new CloneTask(task, tags);
            pendingMachines.add(new Foo(executor.submit(cloneTask), name));
        }
        return names;
    }

    public class Foo {
        Future future;
        String name;
        public Foo(Future future, String name) {
            this.future = future;
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
    }

    public class CloneTask implements Callable {

        Task task;
        List<Tag> tags;

        public CloneTask(Task task, List<Tag> tags) {
            this.task = task;
            this.tags = tags;
        }

        @Override
        public VirtualMachine call() throws RemoteException, InterruptedException {
            task.waitForTask();
            ManagedObjectReference mor = (ManagedObjectReference) task.getTaskInfo().getResult();
            VirtualMachine virtualMachine = new VirtualMachine(task.getServerConnection(), mor);
            for(Tag tag : tags){
                System.err.println("Tagging: " +virtualMachine.getName() + " with: " + tag.getKey() + ":" + tag.getValue());
                tagger.tag(virtualMachine, tag);
            }
            return virtualMachine;
        }
    }

    @Override
    public void terminateVirtualMachines(List<String> ids) throws RemoteException {
        Folder rootFolder = serviceInstance.getRootFolder();
        List<ManagedEntity> managedEntities = Arrays.asList(createInventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine"));


        // Initiate and wait for powerOff tasks
        List<Task> powerOffTasks = Lists.newArrayList();
        for (ManagedEntity managedEntity : managedEntities) {
            if (ids.contains(managedEntity.getName())) {
                VirtualMachine virtualMachine = (VirtualMachine) managedEntity;
                powerOffTasks.add(virtualMachine.powerOffVM_Task());
            }
        }
        for (Task task : powerOffTasks) {
            try {
                task.waitForTask();
            } catch (InterruptedException e) {
                logger.error("Failed to power off VirtualMachine: powerOffVM_Task was interrupted");
            }
        }

        // Initiate and wait for destroy tasks
        List<Task> destroyTasks = Lists.newArrayList();
        for (ManagedEntity managedEntity : managedEntities) {
            if (ids.contains(managedEntity.getName())) {
                destroyTasks.add(managedEntity.destroy_Task());
            }
        }
        for (Task task : destroyTasks) {
            try {
                task.waitForTask();
            } catch (InterruptedException e) {
                logger.error("Failed to destroy VirtualMachine: destroy_Task was interrupted");
            }
        }
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