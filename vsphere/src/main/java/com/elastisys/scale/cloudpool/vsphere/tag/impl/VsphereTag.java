package com.elastisys.scale.cloudpool.vsphere.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;

public class VsphereTag implements Tag {

    private final ScalingTag key;
    private final String value;

    public VsphereTag(ScalingTag key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.key.value;
    }

    @Override
    public String getValue() {
        return value;
    }

}
