package com.elastisys.scale.cloudpool.openstack.requests.lab;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.model.common.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class DeleteKeypairMain {

    private static Logger LOG = LoggerFactory.getLogger(CreateServerMain.class);

    private static final String keyPairName = "mykey";

    public static void main(String[] args) throws Exception {
        HttpLoggingFilter.toggleLogging(false);
        CloudApiSettings driverConfig = DriverConfigLoader.loadDefault();

        OSClient client = new OSClientFactory(driverConfig).authenticatedClient();
        KeypairService keyApi = client.compute().keypairs();

        ActionResponse response = keyApi.delete(keyPairName);
        LOG.info("deleted keypair {}: {}", keyPairName, response);
    }
}
