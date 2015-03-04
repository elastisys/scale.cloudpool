package com.elastisys.scale.cloudpool.openstack.driver;

/**
 * Various constants for the {@link OpenStackCloudClient}.
 *
 *
 *
 */
public class Constants {

	/**
	 * The meta data tag used to hold the pool membership on Openstack server
	 * instances.
	 */
	public static final String CLOUD_POOL_TAG = "elastisys:cloudPool";
	/**
	 * The meta data tag used to store the machine service state for scaling
	 * group members.
	 */
	public static final String SERVICE_STATE_TAG = "elastisys:serviceState";
}
