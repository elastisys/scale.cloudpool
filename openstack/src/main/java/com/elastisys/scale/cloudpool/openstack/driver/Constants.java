package com.elastisys.scale.cloudpool.openstack.driver;

/**
 * Various constants for the {@link OpenStackCloudClient}.
 */
public class Constants {

	/**
	 * The server meta data tag used to hold the pool membership on Openstack
	 * server instances.
	 */
	public static final String CLOUD_POOL_TAG = "elastisys:cloudPool";
	/**
	 * The server meta data tag used to store the service state for pool
	 * members.
	 */
	public static final String SERVICE_STATE_TAG = "elastisys:serviceState";
	/**
	 * The server meta data tag used to store the membership status for pool
	 * members.
	 */
	public static final String MEMBERSHIP_STATUS_TAG = "elastisys:membershipStatus";

}
