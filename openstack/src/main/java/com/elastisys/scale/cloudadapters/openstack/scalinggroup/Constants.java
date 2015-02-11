package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

/**
 * Various constants for the {@link OpenStackCloudClient}.
 *
 *
 *
 */
public class Constants {

	/**
	 * The meta data tag used to hold the scaling group membership on Openstack
	 * server instances.
	 */
	public static final String SCALING_GROUP_TAG = "elastisys:scalingGroup";
	/**
	 * The meta data tag used to store the machine service state for scaling
	 * group members.
	 */
	public static final String SERVICE_STATE_TAG = "elastisys:serviceState";
}
