package com.elastisys.scale.cloudpool.google.commons.api.compute.metadata;

/**
 * Instance metadata tags used to store cloud pool state.
 */
public class MetadataKeys {
    /** Metadata key used to mark the service state of machine pool members. */
    public static final String SERVICE_STATE = "elastisys-service-state";
    /**
     * Metadata key used to mark the membership status of machine pool members.
     */
    public static final String MEMBERSHIP_STATUS = "elastisys-membership-status";
}
