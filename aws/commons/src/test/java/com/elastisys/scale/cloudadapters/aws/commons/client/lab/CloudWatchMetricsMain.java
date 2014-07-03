package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.elastisys.scale.cloudadapters.aws.commons.client.CloudWatchClient;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;

public class CloudWatchMetricsMain extends AbstractClient {
	// TODO: set to region where your CloudWatch endpoint is hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		logger.info(format("Contacting CloudWatch region %s", region));

		CloudWatchClient client = new CloudWatchClient(
				new PropertiesCredentials(credentialsFile), region);
		// ListMetricsResult metrics = client.getApi().listMetrics(
		// new ListMetricsRequest().withMetricName("CPUUtilization"));
		// for (Metric metric : metrics.getMetrics()) {
		// logger.info("namespace: '{}', name: '{}', dimensions: {}",
		// metric.getNamespace(), metric.getMetricName(),
		// metric.getDimensions());
		// }

		// request metric values for all instances
		// List<Dimension> dimensions = Arrays.asList();
		// narrow down: only request metric values for a particular Auto Scaling
		// Group
		List<Dimension> dimensions = Arrays.asList(new Dimension().withName(
				"AutoScalingGroupName").withValue("end2end-scalinggroup"));
		// narrow down: only request metric values for a particular instance
		// List<Dimension> dimensions = Arrays.asList(new Dimension().withName(
		// "InstanceId").withValue("i-09951164"));
		DateTime start = UtcTime.now().minusSeconds(3600);
		DateTime end = UtcTime.now();
		List<String> statistics = Arrays.asList("Sum", "SampleCount");
		// List<String> statistics = Arrays.asList("Sum", "Average", "Minimum",
		// "Maximum", "SampleCount");
		GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
				.withMetricName("CPUUtilization").withDimensions(dimensions)
				.withNamespace("AWS/EC2").withStartTime(start.toDate())
				.withEndTime(end.toDate()).withPeriod(60)
				.withStatistics(statistics);
		System.out.println("Fetching metrics for " + request);
		GetMetricStatisticsResult result = client.getApi().getMetricStatistics(
				request);
		logger.info("Statistics for " + result.getLabel());
		List<Datapoint> dataPoints = Lists.newArrayList(result.getDatapoints());
		Collections.sort(dataPoints, new Comparator<Datapoint>() {
			@Override
			public int compare(Datapoint o1, Datapoint o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		});
		for (Datapoint dataPoint : dataPoints) {
			logger.info("  " + dataPoint);
		}
	}
}
