package com.elastisys.scale.cloudpool.vsphere.tag.impl;

import org.junit.Test;

public class TestVsphereTag {

    @Test
    public void shouldBeAbleToCreateCloudPoolTag() {
        new VsphereTag(ScalingTag.CLOUD_POOL, "MyCloudPool");
    }

}