package com.elastisys.scale.cloudpool.vsphere.tagger;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.vmware.vim25.mo.ManagedEntity;

import java.rmi.RemoteException;

/**
 * A Tagger can be used to set tags and check for tags on a ManagedEntity.
 *
 * This interface was created to allow tagging to be implemented in more than
 * one way.
 */
public interface Tagger {

    /**
     * Check if a ManagedEntity has the specified tag.
     * @param me The ManagedEntity to check.
     * @param vsphereTag The Tag to check for.
     * @return  True if the ManagedEntity has the specified Tag, False otherwise.
     * @throws RemoteException
     */
    boolean isTagged(ManagedEntity me, Tag vsphereTag) throws RemoteException;

    /**
     * Add a Tag to a ManagedEntity.
     * @param me    The ManagedEntity to tag.
     * @param vsphereTag    The Tag to add.
     * @throws RemoteException
     */
    void tag(ManagedEntity me, Tag vsphereTag) throws RemoteException;

    /**
     * Remove a Tag from a ManagedEntity.
     * @param me    ManagedEntity to remove tag from.
     * @param vsphereTag    Tag to remove.
     * @throws RemoteException
     */
    void untag(ManagedEntity me, Tag vsphereTag) throws RemoteException;

}
