package com.elastisys.scale.cloudpool.vsphere.client.impl;

import java.util.concurrent.Future;

/**
 * Auxiliary class that represents the result of a DestroyTask or CloneTask together with the name of the associated
 * VirtualMachine.
 */
class FutureNamePair {
    Future future;
    String name;

    public FutureNamePair(Future future, String name) {
        this.future = future;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
