package com.elastisys.scale.cloudadapters.openstack.scalinggroup.client;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroup;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;

/**
 * A simplified client interface towards the OpenStack API that only provides
 * the functionality needed by the {@link OpenStackScalingGroup}.
 * <p/>
 * The {@link #configure} method must be called before calling any other
 * methods.
 *
 *
 *
 */
public interface OpenstackClient {

	/**
	 * Configures this {@link OpenstackClient} with credentials to allow it to
	 * access the OpenStack API.
	 *
	 * @param configuration
	 *            A client configuration.
	 */
	void configure(OpenStackScalingGroupConfig configuration);

	/**
	 * Retrieves all servers that match a given tag.
	 *
	 * @param tag
	 *            A meta data tag that must be present on returned servers.
	 * @param tagValue
	 *            The value for the meta data tag that must be present on
	 *            returned servers.
	 * @return All {@link Server}s matching the filters.
	 */
	List<Server> getServers(String tag, String tagValue);

	/**
	 * Return meta data about a particular {@link Server} instance.
	 *
	 * @param serverId
	 *            The identifier of the requested server.
	 * @return {@link Server} meta data.
	 *
	 * @throws NotFoundException
	 *             if the server doesn't exist.
	 */
	Server getServer(String serverId) throws NotFoundException;

	/**
	 * Launch a new server.
	 *
	 * @param name
	 *            The name to assign to the created server.
	 * @param provisioningDetails
	 *            The provisioning details on how to launch the new server.
	 * @param tags
	 *            Meta data tags to set on the launched server.
	 * @return The launched {@link Server}.
	 */
	Server launchServer(String name, ScaleUpConfig provisioningDetails,
			Map<String, String> tags);

	/**
	 * Allocate a floating IP address and associate it with a given server.
	 *
	 * @param serverId
	 *            The identifier of the server that will be assigned a floating
	 *            IP address.
	 * @return The IP address that was assigned to the server.
	 */
	String assignFloatingIp(String serverId);

	/**
	 * Terminates a particular server.
	 *
	 * @param serverId
	 *            Identifier of the server to be terminated.
	 * @throws NotFoundException
	 *             if the server doesn't exist.
	 */
	void terminateServer(String serverId) throws NotFoundException;

	/**
	 * Adds meta data tags to a given server.
	 *
	 * @param serverId
	 *            Identifier of the server to be tagged.
	 * @param tags
	 *            Meta data tags to set on the server.
	 * @throws NotFoundException
	 *             if the server doesn't exist.
	 */
	void tagServer(String serverId, Map<String, String> tags)
			throws NotFoundException;

	/**
	 * Removes a collection of meta data tags from a given server.
	 *
	 * @param serverId
	 *            Identifier of the server to be untagged.
	 * @param tags
	 *            The meta data tag keys to remove from the server.
	 * @throws NotFoundException
	 *             if the server doesn't exist.
	 */
	void untagServer(String serverId, List<String> tagKeys)
			throws NotFoundException;

}
