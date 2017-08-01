package com.elastisys.scale.cloudpool.vsphere.client;

import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.List;

public interface VsphereClient {

    void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate) throws RemoteException;

    List<VirtualMachine> getVirtualMachines(List<Tag> tags) throws RemoteException;

    List<String> launchVirtualMachines(int count, List<Tag> tags) throws RemoteException;

    void terminateVirtualMachines(List<String> ids) throws RemoteException;

    void tag(String id, List<Tag> tags);

    void untag(String id, List<Tag> tags);

}
