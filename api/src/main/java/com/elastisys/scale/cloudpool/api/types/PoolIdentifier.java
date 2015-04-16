package com.elastisys.scale.cloudpool.api.types;

/**
 * Enumeration of currently known cloud pool implementations and unique
 * identifiers for each.
 */
public enum PoolIdentifier {
	SPLITTER, OPENSTACK, AWS_EC2, AWS_SPOT_INSTANCES, AWS_AUTO_SCALING_GROUPS
}