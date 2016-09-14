package com.elastisys.scale.cloudpool.aws.commons;

import com.amazonaws.services.ec2.model.Filter;
import com.elastisys.scale.cloudpool.api.CloudPool;

/**
 * {@link Filter}s related to AWS {@link CloudPool}s.
 */
public class ScalingFilters {

    /**
     * The key prefix to use for tag-based filters in the EC2 API. See the
     * <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
     * >EC2 API reference</a>.
     */
    private static final String TAG_FILTER_KEY_PREFIX = "tag:";

    /**
     * The filter key used to filter instances with a given value for the cloud
     * pool tag.
     */
    public static final String CLOUD_POOL_TAG_FILTER = TAG_FILTER_KEY_PREFIX + ScalingTags.CLOUD_POOL_TAG;

    /**
     * The filter key to use for instance state-based filters in the EC2 API.
     * See the <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
     * >EC2 API reference</a>.
     */
    public static final String INSTANCE_STATE_FILTER = "instance-state-name";

    /** A filter key for filtering spot requests on state. */
    public static final String SPOT_REQUEST_STATE_FILTER = "state";

    /** A filter key for filtering spot requests by identifier. */
    public static final String SPOT_REQUEST_ID_FILTER = "spot-instance-request-id";
}
