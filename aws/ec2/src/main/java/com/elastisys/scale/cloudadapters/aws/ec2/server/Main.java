package com.elastisys.scale.cloudadapters.aws.ec2.server;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.Ec2ScalingGroup;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.AwsEc2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;

/**
 * Main class for starting the REST API server for an AWS EC2
 * {@link CloudAdapter}.
 *
 * 
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		ScalingGroup scalingGroup = new Ec2ScalingGroup(new AwsEc2Client());
		CloudAdapterServer.main(new BaseCloudAdapter(scalingGroup), args);
	}
}
