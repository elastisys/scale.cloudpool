package com.elastisys.scale.cloudpool.vsphere.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;

/**
 * This is an implementation of the Tag interface for Vsphere.
 */
public class VsphereTag implements Tag {

    private final ScalingTag key;
    private final String value;

    /**
     * Create a VsphereTag with the given key and value.
     * 
     * @param key
     *            the tag key
     * @param value
     *            the tag value
     */
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
