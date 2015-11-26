package com.elastisys.scale.cloudpool.openstack.requests;

import static com.elastisys.scale.cloudpool.openstack.predicates.ServerPredicates.withTag;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;

/**
 * An OpenStack request task that, when executed, retrieves all {@link Server}s
 * with a certain meta data tag.
 */
public class ListServersWithTagRequest
		extends AbstractOpenstackRequest<List<Server>> {

	/** A meta data tag that must be present on returned servers. */
	private final String tag;
	/**
	 * The value for the meta data tag that must be present on returned servers.
	 */
	private final String tagValue;

	/**
	 * Constructs a new {@link ListServersWithTagRequest} task.
	 *
	 * @param clientFactory
	 *            OpenStack API client factory.
	 * @param tag
	 *            A meta data tag that must be present on returned servers.
	 * @param tagValue
	 *            The value for the meta data tag that must be present on
	 *            returned servers.
	 */
	public ListServersWithTagRequest(OSClientFactory clientFactory, String tag,
			String tagValue) {
		super(clientFactory);
		this.tag = tag;
		this.tagValue = tagValue;

	}

	@Override
	public List<Server> doRequest(OSClient api) {
		List<? extends Server> servers = api.compute().servers().list();
		return newArrayList(filter(servers, withTag(this.tag, this.tagValue)));
	}
}
