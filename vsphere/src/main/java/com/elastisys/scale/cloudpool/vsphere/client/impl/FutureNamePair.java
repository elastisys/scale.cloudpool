package com.elastisys.scale.cloudpool.vsphere.client.impl;

import java.util.concurrent.Future;

public class FutureNamePair {
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
