package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, deletes {@link Tag}s from an EC2
 * resource (such as an instance) in a given region.
 */
public class UntagEc2Resource extends AmazonEc2Request<Void> {
	static Logger LOG = LoggerFactory.getLogger(UntagEc2Resource.class);

	/** The identifier of the resource to tag. For example, an instance id. */
	private final String resourceId;

	/** The tags to remove from the resource. */
	private List<Tag> tags = Lists.newArrayList();

	public UntagEc2Resource(AWSCredentials awsCredentials, String region,
			ClientConfiguration clientConfig, String resourceId, Tag... tags) {
		this(awsCredentials, region, clientConfig, resourceId,
				(tags == null ? new ArrayList<Tag>() : Arrays.asList(tags)));
	}

	public UntagEc2Resource(AWSCredentials awsCredentials, String region,
			ClientConfiguration clientConfig, String resourceId,
			List<Tag> tags) {
		super(awsCredentials, region, clientConfig);
		this.resourceId = resourceId;
		this.tags = Lists.newArrayList(tags);
	}

	@Override
	public Void call() {
		for (Tag tag : this.tags) {
			LOG.debug("deleting {}={} tag on instance {}", tag.getKey(),
					tag.getValue(), this.resourceId);
		}

		DeleteTagsRequest request = new DeleteTagsRequest()
				.withResources(this.resourceId).withTags(this.tags);

		getClient().getApi().deleteTags(request);
		return null;
	}
}
