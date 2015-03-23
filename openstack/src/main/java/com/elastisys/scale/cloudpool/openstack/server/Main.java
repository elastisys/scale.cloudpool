package com.elastisys.scale.cloudpool.openstack.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.StandardOpenstackClient;

/**
 * Main class for starting the REST API server for an OpenStack
 * {@link CloudPool}.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		CloudPoolDriver openstackDriver = new OpenStackPoolDriver(
				new StandardOpenstackClient());
		CloudPoolServer.main(new BaseCloudPool(openstackDriver), args);
	}
}
