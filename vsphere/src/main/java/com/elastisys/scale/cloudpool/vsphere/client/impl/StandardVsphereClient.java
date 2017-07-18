package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagger;
import com.elastisys.scale.cloudpool.vsphere.client.tag.impl.CustomAttributeTagger;
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
    private Tagger tagger;

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
        this.tagger = new CustomAttributeTagger();
    }

    @Override
    public List<VirtualMachine> getVirtualMachines() throws RemoteException {
        Folder rootFolder = serviceInstance.getRootFolder();
        List<VirtualMachine> vms = Lists.newArrayList();
        ManagedEntity[] meArr = createInventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        if(meArr == null){
            return vms;
        }
        List<ManagedEntity> meList = Arrays.asList(meArr);
        for (ManagedEntity me : meList) {
            tagger.isTagged(me, Tag.CLOUD_POOL);
            vms.add((VirtualMachine) me);
        }
        return vms;
    }

    @Override
    public List<VirtualMachine> launchVirtualMachines(int count) {
        return null;
    }

    @Override
    public void terminateVirtualMachines(List<String> ids) {
    }

    ServiceInstance createServiceInstance(URL url, String username, String password, boolean ignoreSsl) throws
            MalformedURLException, RemoteException {
        return new ServiceInstance(url, username, password, ignoreSsl);
    }

    InventoryNavigator createInventoryNavigator(Folder folder) {
        return new InventoryNavigator(folder);
    }
}