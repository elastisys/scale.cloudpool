package com.elastisys.scale.cloudpool.vsphere.client.tag;

import java.util.HashSet;

public final class Tag {

    private Tag () {}

    public static final String CLOUD_POOL = "elastisys:cloudPool";
    public static final String SERVICE_STATE = "elastisys:serviceState";
    public static final String MEMBERSHIP_STATUS = "elastisys:membershipStatus";
    public static final String INSTANCE_NAME = "Name";

    public static HashSet<String> getTags(){
        HashSet<String> tags = new HashSet();
        tags.add(Tag.CLOUD_POOL);
        tags.add(Tag.SERVICE_STATE);
        tags.add(Tag.MEMBERSHIP_STATUS);
        tags.add(Tag.INSTANCE_NAME);
        return tags;
    }

}
