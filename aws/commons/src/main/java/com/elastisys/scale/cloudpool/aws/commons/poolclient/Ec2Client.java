package com.elastisys.scale.cloudpool.aws.commons.poolclient;

import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;

/**
 * A simplified client interface towards the AWS EC2 API that provides
 * functionality that is useful to AWS-based {@link CloudPoolDriver}s.
 * <p/>
 * The {@link #configure} method must be called before calling any other
 * methods.
 */
public interface Ec2Client {

	/**
	 * Configures the {@link Ec2Client} with credentials to allow it to access
	 * the AWS EC2 API.
	 *
	 * @param awsAccessKeyId
	 *            The access key id of the AWS account.
	 * @param awsSecretAccessKey
	 *            The secret access key of the AWS account.
	 * @param region
	 *            The targeted region.
	 * @param clientConfig
	 *            Client configuration options such as connection timeout, etc.
	 */
	void configure(String awsAccessKeyId, String awsSecretAccessKey,
			String region, ClientConfiguration clientConfig);

	/**
	 * Retrieves all instances that match the given filters.
	 *
	 * @param filters
	 *            The query {@link Filter}s that returned instances must match.
	 * @return All {@link Instance}s matching the filters.
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	List<Instance> getInstances(List<Filter> filters)
			throws AmazonClientException;

	/**
	 * Retrieves meta data about a particular EC2 {@link Instance}.
	 *
	 * @param instanceId
	 *            An instance identifier.
	 * @return The requested {@link Instance} meta data.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	Instance getInstanceMetadata(String instanceId)
			throws NotFoundException, AmazonClientException;

	/**
	 * Requests a number of new EC2 {@link Instance}s to be launched and
	 * (optionally) tags the instances with a given set of {@link Tag}s.
	 *
	 * @param provisioningDetails
	 *            The provisioning details on how to launch the new machine
	 *            {@link Instance}s.
	 * @param count
	 *            The number of {@link Instance}s to launch.
	 * @param tags
	 *            Tags to set on the launched instances. May be empty.
	 * @return The launched {@link Instance}s.
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	List<Instance> launchInstances(ScaleOutConfig provisioningDetails,
			int count, List<Tag> tags) throws AmazonClientException;

	/**
	 * Sets a collection of tags on an EC2 resource (such as an instance or a
	 * spot request).
	 *
	 * @param resourceId
	 *            The identifier of the EC2 resource to be tagged.
	 * @param tags
	 *            The {@link Tag}s to set on the resource.
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	void tagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException;

	/**
	 * Removes tags from an EC2 resource (such as an instance or a spot
	 * request).
	 *
	 * @param resourceId
	 *            The identifier of the EC2 resource to be un-tagged.
	 * @param tags
	 *            The tags to remove from the resource.
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	void untagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException;

	/**
	 * Terminates a collection of EC2 {@link Instance}s.
	 *
	 * @param instanceIds
	 *            The identifiers of the {@link Instance}s to be terminated.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 * @throws AmazonClientException
	 *             if the request failed
	 */
	void terminateInstances(List<String> instanceIds)
			throws NotFoundException, AmazonClientException;
}
