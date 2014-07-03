package com.elastisys.scale.cloudadapters.aws.autoscaling.lab;

import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroupConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

public class AbstractClient {
	protected static final String awsAccessKeyId = System
			.getenv("AWS_ACCESS_KEY_ID");
	protected static final String awsSecretAccessKey = System
			.getenv("AWS_SECRET_ACCESS_KEY");

	// TODO: set to the region you wish to operate against
	protected static final String region = "us-east-1";

	protected static JsonObject awsClientConfig() {
		AwsAsScalingGroupConfig config = new AwsAsScalingGroupConfig(
				awsAccessKeyId, awsSecretAccessKey, region);
		return JsonUtils.toJson(config).getAsJsonObject();
	}
}
