package com.elastisys.scale.cloudpool.aws.ec2.driver.client;

import java.util.List;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;

/**
 * A simplified client interface towards the AWS EC2 API that only provides the
 * functionality needed by the {@link Ec2PoolDriver}.
 * <p/>
 * The {@link #configure} method must be called before calling any other
 * methods.
 */
public interface Ec2Client {

	/**
	 * Configures this {@link Ec2Client} with credentials to allow it to access
	 * the AWS EC2 API.
	 *
	 * @param configuration
	 *            A client configuration.
	 */
	void configure(Ec2PoolDriverConfig configuration);

	/**
	 * Retrieves all instances that match the given filters.
	 *
	 * @param filters
	 *            The query {@link Filter}s that returned instances must match.
	 * @return All {@link Instance}s matching the filters.
	 */
	List<Instance> getInstances(List<Filter> filters);

	/**
	 * Retrieves instance meta data about a particular EC2 {@link Instance}.
	 *
	 * @param instanceId
	 *            An instance identifier.
	 * @return The requested {@link Instance} meta data.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 */
	Instance getInstanceMetadata(String instanceId) throws NotFoundException;

	/**
	 * Launches a new EC2 {@link Instance}.
	 *
	 * @param provisioningDetails
	 *            The provisioning details on how to launch the new machine
	 *            {@link Instance}.
	 * @return The launched {@link Instance}.
	 */
	Instance launchInstance(ScaleOutConfig provisioningDetails);

	/**
	 * Sets a collection of tags on an EC2 instance.
	 *
	 * @param instanceId
	 *            An instance identifier.
	 * @param tags
	 *            The {@link Tag}s to set on the {@link Instance}.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 */
	void tagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException;

	/**
	 * Removes tags from an EC2 instance.
	 *
	 * @param instanceId
	 *            An instance identifier.
	 * @param tags
	 *            The tags to remove from the instance.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 */
	void untagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException;

	/**
	 * Terminates a particular EC2 {@link Instance}.
	 *
	 * @param instanceId
	 *            Identifier of the {@link Instance} to be terminated.
	 * @throws NotFoundException
	 *             if the instance doesn't exist
	 */
	void terminateInstance(String instanceId) throws NotFoundException;
}
