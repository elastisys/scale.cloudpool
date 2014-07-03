package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.elastisys.scale.cloudadapters.openstack.predicates.ServerPredicates.withStateIn;
import static com.google.common.collect.Iterables.filter;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.Constants;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.google.common.collect.Lists;

/**
 * An OpenStack request task that, when executed, retrieves all active
 * (pending/running) {@link Server}s that are member of a certain scaling group
 * (that is, that have a {@link Constants#SCALING_GROUP_TAG} tag with a given
 * value).
 *
 * 
 *
 */
public class ListRunningScalingGroupServersRequest extends
		AbstractNovaRequest<List<Server>> {

	/** The scaling group whose members are to be retrieved. */
	private final String scalingGroup;

	/**
	 * Constructs a new {@link ListServersWithTagRequest} task.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack endpoint.
	 * @param scalingGroup
	 *            The scaling group whose members are to be retrieved.
	 *
	 */
	public ListRunningScalingGroupServersRequest(
			OpenStackScalingGroupConfig account, String scalingGroup) {
		super(account);
		this.scalingGroup = scalingGroup;
	}

	@Override
	public List<Server> doRequest(NovaApi api) {
		ListServersWithTagRequest getGroupMembers = new ListServersWithTagRequest(
				getAccount(), Constants.SCALING_GROUP_TAG, this.scalingGroup);
		List<Server> serversInGroup = getGroupMembers.call();

		List<Server> activeServers = Lists.newArrayList(filter(serversInGroup,
				withStateIn(Status.BUILD, Status.ACTIVE)));

		return activeServers;
	}
}
