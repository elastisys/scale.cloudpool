package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;

/**
 * A {@link Callable} task that, when executed, applies {@link Tag}s to a
 * collection of EC2 resources (such as an instance or a spot request) in a
 * given region.
 * <p/>
 * Note that due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API, a recently created EC2
 * instance or spot instance request may not be immediately available for
 * tagging. Therefore, it might be wise to use a retry strategy (with
 * exponential back-off) when tagging a recently created resource.
 *
 * @see Retryable
 * @see Retryers
 */
public class TagEc2Resources extends AmazonEc2Request<Void> {
    static Logger LOG = LoggerFactory.getLogger(TagEc2Resources.class);

    /** The identifier of the resource to tag. For example, an instance id. */
    private final List<String> resourceIds;

    /** The tags to apply to the resource. */
    private List<Tag> tags = new ArrayList<>();

    public TagEc2Resources(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            List<String> resourceIds, List<Tag> tags) {
        super(awsCredentials, region, clientConfig);
        this.resourceIds = resourceIds;
        this.tags = new ArrayList<>(tags);
    }

    @Override
    public Void call() {
        LOG.debug("setting tags {} on resources {}", this.tags, this.resourceIds);

        CreateTagsRequest request = new CreateTagsRequest(this.resourceIds, this.tags);
        getClient().getApi().createTags(request);
        return null;
    }
}
