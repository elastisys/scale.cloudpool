package com.elastisys.scale.cloudpool.vsphere.tag.impl;

import org.junit.Test;

public class TestTag {

    @Test
    public void shouldBeAbleToCreateCloudPoolTag() {
        new VsphereTag(VsphereTag.ScalingTag.CLOUD_POOL, "MyCloudPool");
    }

}