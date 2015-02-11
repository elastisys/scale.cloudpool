package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import com.elastisys.scale.cloudadapters.aws.commons.ScalingTags;

/**
 * Various constants for the {@link Ec2CloudClient}.
 *
 *
 *
 */
public class Constants {

	/**
	 * The key prefix to use for tag-based filters in the EC2 API. See the <a
	 * href=
	 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
	 * >EC2 API reference</a>.
	 */
	private static final String TAG_FILTER_KEY_PREFIX = "tag:";

	/**
	 * The filter key used to filter instances with a given value for the
	 * scaling group tag.
	 */
	public static final String SCALING_GROUP_TAG_FILTER_KEY = TAG_FILTER_KEY_PREFIX
			+ ScalingTags.SCALING_GROUP_TAG;

	/**
	 * The key prefix to use for instance state-based filters in the EC2 API.
	 * See the <a href=
	 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
	 * >EC2 API reference</a>.
	 */
	public static final String STATE_FILTER_KEY = "instance-state-name";

	/** The instance tag that holds the human-readable name of an instance. */
	public static final String NAME_TAG = "Name";

}
