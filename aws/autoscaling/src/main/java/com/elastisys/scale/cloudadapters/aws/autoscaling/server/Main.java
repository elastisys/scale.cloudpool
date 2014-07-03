package com.elastisys.scale.cloudadapters.aws.autoscaling.server;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroup;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;

/**
 * Main class for starting the REST API server for the
 * {@link AwsAutoScalingCloudAdapter} {@link CloudAdapter}.
 *
 * 
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		AwsAsScalingGroup scalingGroup = new AwsAsScalingGroup(
				new AwsAutoScalingClient());
		CloudAdapterServer.main(new BaseCloudAdapter(scalingGroup), args);
	}
}
