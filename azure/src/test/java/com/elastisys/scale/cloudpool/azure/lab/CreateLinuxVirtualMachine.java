package com.elastisys.scale.cloudpool.azure.lab;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.VmLauncher;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

/**
 * Lab program for creating a Linux VM in Azure.
 */
public class CreateLinuxVirtualMachine extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateLinuxVirtualMachine.class);

    public static void main(String[] args) throws CloudException, IOException {
        String resourceGroup = "itest";
        String region = "northeurope";
        String storageAccountName = "itestdisks";

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        String vmSize = "Standard_DS1_v2";
        String vmName = String.format("testvm-%s", System.currentTimeMillis());

        String networkName = "itestnet";
        String subnetName = "default";
        boolean assignPublicIp = true;
        String imageRef = "Canonical:UbuntuServer:16.04.0-LTS:latest";
        String rootUserName = "ubuntu";
        String publicKey = Files.toString(new File(System.getenv("HOME"), ".ssh/id_rsa.pub"), Charsets.UTF_8);
        Map<String, String> tags = ImmutableMap.of("elastisys-cloudPool", "testpool");
        String bootScript = "#!/bin/bash\nsudo apt update -qy && sudo apt install -qy apache2";

        VmSpec vmSpec = new VmSpec(vmSize, imageRef, vmName,
                Optional.of(new LinuxSettings(rootUserName, publicKey, null, Base64Utils.toBase64(bootScript), null)),
                Optional.empty(), storageAccountName,
                new NetworkSettings(networkName, subnetName, assignPublicIp, Arrays.asList("itest-webserver")), tags);
        VmLauncher vmLauncher = new VmLauncher(apiAccess, resourceGroup, region);
        List<VirtualMachine> createdVms = vmLauncher.createVms(Arrays.asList(vmSpec));
        LOG.debug("created VM: {}", createdVms.get(0).id());
    }

}
