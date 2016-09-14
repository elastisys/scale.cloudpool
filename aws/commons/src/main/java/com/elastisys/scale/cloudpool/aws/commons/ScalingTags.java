package com.elastisys.scale.cloudpool.aws.commons;

public class ScalingTags {
    /** The tag used to mark pool members. */
    public static final String CLOUD_POOL_TAG = "elastisys:cloudPool";
    /** The tag used to mark the service state of machine pool members. */
    public static final String SERVICE_STATE_TAG = "elastisys:serviceState";
    /** The tag used to mark the membership status of machine pool members. */
    public static final String MEMBERSHIP_STATUS_TAG = "elastisys:membershipStatus";
    /** The instance tag that holds the human-readable name of an instance. */
    public static final String INSTANCE_NAME_TAG = "Name";
}
