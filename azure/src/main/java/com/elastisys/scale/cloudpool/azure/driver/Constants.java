package com.elastisys.scale.cloudpool.azure.driver;

/**
 * Various constants for the {@link AzurePoolDriver}.
 */
public class Constants {

    /**
     * The virtual machine tag used to mark pool membership. Note: the following
     * characters are not supported: {@code <>*%&:\?/+.}.
     */
    public static final String CLOUD_POOL_TAG = "elastisys-CloudPool";
    /**
     * The virtual machine tag used to store the service state for pool members.
     */
    public static final String SERVICE_STATE_TAG = "elastisys-ServiceState";
    /**
     * The virtual machine tag used to store the membership status for pool
     * members.
     */
    public static final String MEMBERSHIP_STATUS_TAG = "elastisys-MembershipStatus";

}
