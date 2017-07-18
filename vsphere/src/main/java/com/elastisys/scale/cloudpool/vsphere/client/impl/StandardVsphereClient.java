package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

public class StandardVsphereClient implements VsphereClient {

    private VsphereApiSettings vsphereApiSettings;
    private VsphereProvisioningTemplate vsphereProvisioningTemplate;

    @Override
    public void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate)
            throws RemoteException {
        this.vsphereApiSettings = vsphereApiSettings;
        this.vsphereProvisioningTemplate = vsphereProvisioningTemplate;
        try {
            createServiceInstance(this.vsphereApiSettings.getUrl(), this.vsphereApiSettings.getUsername(),
                    this.vsphereApiSettings.getPassword(), true);
        } catch (MalformedURLException e) {
            throw new RemoteException();
        }
    }

    @Override
    public List<VirtualMachine> getVirtualMachines() {
        return null;
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
}