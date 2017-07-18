package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.vmware.vim25.mo.VirtualMachine;

import java.util.List;

public class StandardVsphereClient implements VsphereClient {

    @Override
    public void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate) {

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
}