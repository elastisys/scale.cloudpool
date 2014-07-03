package com.elastisys.scale.cloudadapters.aws.commons.requests.cloudwatch;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.joda.time.Interval;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests statistics for a
 * particular metric to be fetched from AWS CloudWatch in a given region.
 * <p/>
 * The returned {@link GetMetricStatisticsResult} object will store the query
 * metric in its {@code label} field and will store the list of
 * {@link Datapoint}s sorted in increasing order of time stamp (oldest first).
 * 
 * 
 * 
 */
public class GetMetricStatistics extends
		AmazonCloudWatchRequest<GetMetricStatisticsResult> {

	/**
	 * The Amazon CloudWatch namespace of the metric to fetch.
	 * <p/>
	 * See <a href=
	 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
	 * >CloudWatch concepts</a> for an explanation.
	 */
	private final String namespace;

	/** The CloudWatch metric that to retrieve data points for. */
	private final String metric;

	/**
	 * The dimensions (key-value pairs) used to narrow down the result set. Only
	 * data points matching the specified dimensions will be returned.
	 * <p/>
	 * See <a href=
	 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
	 * >CloudWatch concepts</a> for an explanation.
	 */
	private final Map<String, String> dimensions;

	/**
	 * The aggregation methods to apply to metric values. Can be any of "Sum",
	 * "Average", "Minimum", "Maximum" and "SampleCount". The returned
	 * {@link Datapoint}s will contain one value for each specified statistic.
	 */
	private final List<String> statistics;

	/**
	 * The granularity of returned metric values (in seconds). The
	 * {@code statistics} function will aggregate metric values with this level
	 * of granularity. Must be at least {@code 60} seconds and must be a
	 * multiple of {@code 60}.
	 */
	private final int period;

	/**
	 * The time interval for the query. Only {@link Datapoint}s whose time stamp
	 * lies within this interval will be returned.
	 */
	private final Interval queryInterval;

	/**
	 * Constructs a new {@link GetMetricStatistics} request.
	 * 
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 * @param namespace
	 *            The Amazon CloudWatch namespace of the metric to fetch.
	 * @param metric
	 *            The CloudWatch metric that to retrieve data points for.
	 * @param dimensions
	 *            The dimensions (key-value pairs) used to narrow down the
	 *            result set. Only data points matching the specified dimensions
	 *            will be returned.
	 * @param statistic
	 *            The aggregation method(s) to apply to metric values. Can be
	 *            any of "Sum", "Average", "Minimum", "Maximum" and
	 *            "SampleCount". The returned {@link Datapoint}s will contain
	 *            one value for each specified statistic.
	 * @param period
	 *            The granularity of returned metric values (in seconds). The
	 *            {@code statistics} function will aggregate metric values with
	 *            this level of granularity. Must be at least {@code 60} seconds
	 *            and must be a multiple of {@code 60}.
	 * @param queryInterval
	 *            The time interval for the query. Only {@link Datapoint}s whose
	 *            time stamp lies within this interval will be returned.
	 */
	public GetMetricStatistics(AWSCredentials awsCredentials, String region,
			String namespace, String metric, Map<String, String> dimensions,
			List<String> statistics, int period, Interval queryInterval) {
		super(awsCredentials, region);
		this.namespace = namespace;
		this.metric = metric;
		this.dimensions = dimensions;
		this.statistics = statistics;
		this.period = period;
		this.queryInterval = queryInterval;
	}

	@Override
	public GetMetricStatisticsResult call() throws Exception {
		GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
				.withNamespace(this.namespace).withMetricName(this.metric)
				.withDimensions(toDimensionList(this.dimensions))
				.withStatistics(this.statistics).withPeriod(this.period)
				.withStartTime(this.queryInterval.getStart().toDate())
				.withEndTime(this.queryInterval.getEnd().toDate());
		GetMetricStatisticsResult result = getClient().getApi()
				.getMetricStatistics(request);

		// sort result set
		List<Datapoint> resultSet = Lists.newArrayList(result.getDatapoints());
		Collections.sort(resultSet, new Comparator<Datapoint>() {
			@Override
			public int compare(Datapoint o1, Datapoint o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		});
		return new GetMetricStatisticsResult().withLabel(this.metric)
				.withDatapoints(resultSet);
	}

	private List<Dimension> toDimensionList(Map<String, String> dimensions) {
		List<Dimension> dimensionList = Lists.newArrayList();
		for (Entry<String, String> dimension : dimensions.entrySet()) {
			dimensionList.add(new Dimension().withName(dimension.getKey())
					.withValue(dimension.getValue()));
		}
		return dimensionList;
	}
}
