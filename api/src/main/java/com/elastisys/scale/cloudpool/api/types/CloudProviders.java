package com.elastisys.scale.cloudpool.api.types;

/**
 * Enumeration of known cloud providers/platforms for which there are cloud pool
 * implementations.
 */
public class CloudProviders {
    /** Generic OpenStack cloud provider. */
    public static final String OPENSTACK = "OpenStack";
    public static final String CITYCLOUD = "CityCloud";
    public static final String AWS_EC2 = "AWS-EC2";
    public static final String AWS_SPOT = "AWS-SPOT";
    public static final String AWS_AUTO_SCALING_GROUP = "AWS-AUTO-SCALING-GROUP";
    public static final String AZURE = "Azure";
}