package com.elastisys.scale.cloudpool.juju.lab;

import java.io.File;

import com.elastisys.scale.cloudpool.juju.client.impl.CommandLineJujuClient;
import com.elastisys.scale.cloudpool.juju.config.JujuCloudPoolConfig;
import com.elastisys.scale.cloudpool.juju.config.JujuEnvironmentConfig;
import com.elastisys.scale.cloudpool.juju.config.OperationsMode;
import com.elastisys.scale.commons.util.base64.Base64Utils;

public class Main {
    
    public static void main(String[] args) throws Exception {
        String encodedConfig = Base64Utils.toBase64(new File("~/.juju/environments.yaml"));
        String encodedJenv = Base64Utils.toBase64(new File("~/.juju/environments/local.jenv"));
        String encodedPubkey = Base64Utils.toBase64(new File("~/.juju/ssh/juju_id_rsa.pub"));
        String encodedPrivkey = Base64Utils.toBase64(new File("~/.juju/ssh/juju_id_rsa"));
        
        JujuEnvironmentConfig environment = new JujuEnvironmentConfig(
                "local", encodedConfig, encodedJenv, encodedPubkey,
                encodedPrivkey);
        
        OperationsMode mode = OperationsMode.UNITS;
        
        String service = "mysql";
        
        CommandLineJujuClient client = new CommandLineJujuClient();
        
        client.configure(new JujuCloudPoolConfig(environment, mode, service));
    }
}
