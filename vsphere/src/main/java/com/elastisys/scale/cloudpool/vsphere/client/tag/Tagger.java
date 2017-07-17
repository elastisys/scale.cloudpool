package com.elastisys.scale.cloudpool.vsphere.client.tag;

import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

import java.rmi.RemoteException;

public interface Tagger {

    void initialize(ServiceInstance si) throws RemoteException;

    boolean isTagged(ManagedEntity me, String tag) throws RemoteException;

    void tag(ManagedEntity me, String tag) throws RemoteException;

}