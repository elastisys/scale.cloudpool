package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.elastisys.scale.cloudadapters.aws.commons.requests.cloudwatch.GetMetricStatistics;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ImmutableMap;

public class GetMetricStatisticsMain extends AbstractClient {

	// TODO: set to region where your CloudWatch endpoint is hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		logger.info(format("Contacting CloudWatch region %s", region));

		DateTime start = UtcTime.now().minusDays(3);
		DateTime end = UtcTime.now().minusDays(3).plusHours(4);

		GetMetricStatisticsResult result = new GetMetricStatistics(
				new PropertiesCredentials(credentialsFile), region, "AWS/EC2",
				"CPUUtilization", ImmutableMap.of("AutoScalingGroupName",
						"end2end-scalinggroup"), Arrays.asList("Average",
						"Sum", "SampleCount"), 60, new Interval(start, end))
				.call();
		System.out.println("These data points were returned for metric "
				+ result.getLabel() + ":");
		for (Datapoint datapoint : result.getDatapoints()) {
			System.out.println(datapoint);
		}
	}
}
