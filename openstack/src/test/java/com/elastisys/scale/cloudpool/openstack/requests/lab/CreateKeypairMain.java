package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.io.File;
import java.nio.file.Paths;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.model.compute.Keypair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.google.common.io.Files;

public class CreateKeypairMain {

    private static Logger LOG = LoggerFactory.getLogger(CreateServerMain.class);

    /** TODO: change to keypair name to use. */
    private static final String keyPairName = "myinstancekey";

    /** TODO: change to public key to upload */
    private static final File publicKeyFile = Paths
            .get(System.getenv("HOME"), "keys", "citycloud", "instancekey.pem.pub").toFile();

    public static void main(String[] args) throws Exception {
        HttpLoggingFilter.toggleLogging(false);
        CloudApiSettings driverConfig = DriverConfigLoader.loadDefault();

        OSClient client = new OSClientFactory(driverConfig.toApiAccessConfig()).authenticatedClient();
        KeypairService keyApi = client.compute().keypairs();

        String publicKeyContent = new String(Files.toByteArray(publicKeyFile));
        Keypair createdKeypair = keyApi.create(keyPairName, publicKeyContent);
        LOG.info("created keypair: {}", createdKeypair);
    }
}
