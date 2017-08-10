package com.elastisys.scale.cloudpool.vsphere.tag;

import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;

public interface Tag {

    /**
     * Gets the key of the tag.
     *
     * @return A string which should correspond to a {@link ScalingTag}.
     */
    String getKey();

    /**
     * Gets the value of the tag.
     *
     * @return A string which depends on the {@link ScalingTag} defined as key.
     */
    String getValue();

}
