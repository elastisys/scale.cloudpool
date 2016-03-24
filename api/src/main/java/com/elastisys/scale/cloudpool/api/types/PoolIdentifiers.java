package com.elastisys.scale.cloudpool.api.types;

/**
 * Enumeration of currently known cloud pool implementations and unique
 * identifiers for each.
 */
public class PoolIdentifiers {
	public static final String SPLITTER = "Splitter";
	public static final String OPENSTACK = "OpenStack";
	public static final String CITYCLOUD = "CityCloud";
	public static final String AWS_EC2 = "AWS-EC2";
	public static final String AWS_SPOT = "AWS-SPOT";
	public static final String AWS_AUTO_SCALING_GROUP = "AWS-AUTO-SCALING-GROUP";
	public static final String KUBERNETES = "Kubernetes";
}