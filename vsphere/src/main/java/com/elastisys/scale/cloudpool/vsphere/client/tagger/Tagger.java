package com.elastisys.scale.cloudpool.vsphere.client.tagger;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

import java.rmi.RemoteException;

public interface Tagger {

    void initialize(ServiceInstance si) throws RemoteException;

    boolean isTagged(ManagedEntity me, Tag vsphereTag) throws RemoteException;

    void tag(ManagedEntity me, Tag vsphereTag) throws RemoteException;

    void untag(ManagedEntity me, Tag vsphereTag) throws RemoteException;

}
