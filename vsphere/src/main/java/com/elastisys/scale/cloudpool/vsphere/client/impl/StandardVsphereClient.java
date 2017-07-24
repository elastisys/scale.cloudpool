package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tagger.TaggerFactory;
import com.elastisys.scale.cloudpool.vsphere.tagger.impl.CustomAttributeTagger;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.google.common.collect.Lists;
import com.vmware.vim25.mo.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

public class StandardVsphereClient implements VsphereClient {

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
        if(meArr == null){
            return vms;
        }
        List<ManagedEntity> meList = Arrays.asList(meArr);
        for (ManagedEntity me : meList) {
            boolean addMe = true;
            for(Tag tag : tags) {
                if(!tagger.isTagged(me, tag)) {
                    addMe = false;
                    break;
                }
            }
            if(addMe) {
                vms.add((VirtualMachine) me);
            }
        }
        return vms;
    }

    @Override
    public List<VirtualMachine> launchVirtualMachines(int count, List<Tag> tags) {
        return null;
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
}