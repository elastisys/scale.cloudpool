package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.elastisys.scale.cloudadapters.openstack.predicates.ServerPredicates.withTag;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

/**
 * An OpenStack request task that, when executed, retrieves all servers with a
 * certain meta data tag. Note that this task returns servers that are in any
 * state (for example, running, booting, terminating).
 *
 * 
 *
 */
public class ListServersWithTagRequest extends
		AbstractNovaRequest<List<Server>> {

	/** A meta data tag that must be present on returned servers. */
	private final String tag;
	/**
	 * The value for the meta data tag that must be present on returned servers.
	 */
	private final String tagValue;

	/**
	 * Constructs a new {@link ListServersWithTagRequest} task.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack endpoint.
	 * @param tag
	 *            A meta data tag that must be present on returned servers.
	 * @param tagValue
	 *            The value for the meta data tag that must be present on
	 *            returned servers.
	 */
	public ListServersWithTagRequest(OpenStackScalingGroupConfig account,
			String tag, String tagValue) {
		super(account);
		this.tag = tag;
		this.tagValue = tagValue;

	}

	@Override
	public List<Server> doRequest(NovaApi api) {
		List<Server> response = Lists.newArrayList();
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		FluentIterable<? extends Server> servers = serverApi.listInDetail()
				.concat();
		response.addAll(newArrayList(filter(servers,
				withTag(this.tag, this.tagValue))));
		return response;
	}
}
