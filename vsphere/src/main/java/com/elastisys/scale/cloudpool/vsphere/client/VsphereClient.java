package com.elastisys.scale.cloudpool.vsphere.client;

import com.elastisys.scale.cloudpool.vsphere.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.config.VsphereProvisioningTemplate;
import com.vmware.vim25.mo.VirtualMachine;

import java.util.List;

public interface VsphereClient {

    void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate);

    List<VirtualMachine> getVirtualMachines();

    List<VirtualMachine> launchVirtualMachines(int count);

    void terminateVirtualMachines(List<String> ids);

}
