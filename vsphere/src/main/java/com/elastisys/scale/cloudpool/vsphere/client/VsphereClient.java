package com.elastisys.scale.cloudpool.vsphere.client;

import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Client interface which may be used by a Driver to communicate with the remote Vcenter server.
 */
public interface VsphereClient {

    /**
     * Sets the configuration for the Vsphere client.
     *
     * @param vsphereApiSettings          Information about the Vcenter server itself.
     * @param vsphereProvisioningTemplate Information the virtual machines which should be provisioned.
     * @throws RemoteException This exception is thrown if an error occurred in communication with Vcenter.
     */
    void configure(VsphereApiSettings vsphereApiSettings, VsphereProvisioningTemplate vsphereProvisioningTemplate) throws RemoteException;

    /**
     * Contains a list of ids of virtual machines that have been requested but may not yet be visible in the API.
     *
     * @return A list of pending virtual machines which may not yet be visible through getVirtualMachines.
     */
    List<String> pendingVirtualMachines();

    /**
     * Returns a list of the virtual machines on the Vcenter server.
     *
     * @param tags All returned machines are required to have all these tags. An empty list will return all machines.
     * @return A list of virtual machines.
     * @throws RemoteException This exception is thrown if an error occurred in communication with Vcenter.
     */
    List<VirtualMachine> getVirtualMachines(List<Tag> tags) throws RemoteException;

    /**
     * Launches a number of virtual machines asynchronously.
     *
     * @param count Number of virtual machines to launch.
     * @param tags  Tags to attach to the virtual machines.
     * @return A list of names for the launched virtual machines.
     * @throws RemoteException This exception is thrown if an error occurred in communication with Vcenter.
     */
    List<String> launchVirtualMachines(int count, List<Tag> tags) throws RemoteException;

    /**
     * Terminated a number of virtual machines asynchronously.
     *
     * @param ids Names of the virtual machines which should be terminated.
     * @throws RemoteException This exception is thrown if an error occurred in communication with Vcenter.
     */
    void terminateVirtualMachines(List<String> ids) throws RemoteException;

    /**
     * Sets a number of tags on a specific virtual machines.
     *
     * @param id   Name of the virtual machine which should be tagged.
     * @param tags List of tags to attach to the virtual machine.
     */
    void tag(String id, List<Tag> tags);

    /**
     * Removes a number of tags from a specific virtual machines.
     *
     * @param id   Name of the virtual machine which should be untagged.
     * @param tags List of tags to remove from the virtual machine.
     */
    void untag(String id, List<Tag> tags);

}
