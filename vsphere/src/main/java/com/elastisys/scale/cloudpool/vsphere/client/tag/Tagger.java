package com.elastisys.scale.cloudpool.vsphere.client.tag;

import com.vmware.vim25.mo.ManagedEntity;

import java.rmi.RemoteException;

public interface Tagger {

    boolean isTagged(ManagedEntity me, String tag);

    void tag(ManagedEntity me, String tag) throws RemoteException;

}