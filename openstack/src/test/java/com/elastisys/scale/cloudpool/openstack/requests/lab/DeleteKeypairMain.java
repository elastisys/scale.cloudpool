package com.elastisys.scale.cloudpool.openstack.requests.lab;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.model.compute.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

public class DeleteKeypairMain {

	private static Logger LOG = LoggerFactory.getLogger(CreateServerMain.class);

	private static final String keyPairName = "mykey";

	public static void main(String[] args) throws Exception {
		HttpLoggingFilter.toggleLogging(false);
		OpenStackPoolDriverConfig driverConfig = DriverConfigLoader
				.loadDefault();

		OSClient client = new OSClientFactory()
				.createAuthenticatedClient(driverConfig.getAuth());
		KeypairService keyApi = client.compute().keypairs();

		ActionResponse response = keyApi.delete(keyPairName);
		LOG.info("deleted keypair {}: {}", keyPairName, response);
	}
}
