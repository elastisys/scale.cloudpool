package com.elastisys.scale.cloudadapters.openstack.server;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroup;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.StandardOpenstackClient;

/**
 * Main class for starting the REST API server for an OpenStack
 * {@link CloudAdapter}.
 *
 * 
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		ScalingGroup openstackScalingGroup = new OpenStackScalingGroup(
				new StandardOpenstackClient());
		CloudAdapterServer.main(new BaseCloudAdapter(openstackScalingGroup),
				args);
	}
}
