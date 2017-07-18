package com.elastisys.scale.cloudpool.vsphere.client;

import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.List;

public interface VsphereClient {

    void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate) throws RemoteException;

    List<VirtualMachine> getVirtualMachines() throws RemoteException;

    List<VirtualMachine> launchVirtualMachines(int count) throws RemoteException;

    void terminateVirtualMachines(List<String> ids) throws RemoteException;

}
