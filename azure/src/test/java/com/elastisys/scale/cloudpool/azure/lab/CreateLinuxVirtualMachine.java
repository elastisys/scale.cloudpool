package com.elastisys.scale.cloudpool.azure.lab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.VmLauncher;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.WindowsSettings;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

/**
 * Lab program for creating a Linux VM in Azure.
 */
public class CreateLinuxVirtualMachine extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateLinuxVirtualMachine.class);

    /** TODO: set to region to create vm in */
    private static final Region region = Region.US_EAST;
    /** TODO: set to resource group to create vm under */
    private static final String resourceGroup = "itest";

    /** TODO: set to size of created VM. */
    private static final String vmSize = "Standard_DS1_v2";
    /** TODO: set to image of created VM. */
    private static final VmImage vmImage = new VmImage("Canonical:UbuntuServer:16.04.0-LTS:latest");
    /** TODO: set to the disk type to create for the VM's OS disk. */
    private static final StorageAccountTypes osDiskType = StorageAccountTypes.PREMIUM_LRS;
    /** TODO: set to name of created VM. */
    private static final String vmName = "my-vm";
    /** TODO: set to virtual network in which to create VM. */
    private static final String vnet = "itestnet";
    /** TODO: set to virtual network subnet in which to create VM. */
    private static final String subnet = "default";
    /**
     * TODO: set to name to an existing network interface card that is to be
     * attached to the VM. See {@link CreateNetworkInterface}.
     */
    private static final String nicName = "my-nic";

    /** TODO: set to indicate if a public IP is to be assigned to VM. */
    private static final boolean assignPublicIp = true;
    /** TODO: set to the security groups to associate with VM */
    private static final List<String> vmSecurityGroups = Arrays.asList("itest-webserverz");
    /** TODO: set if vm is to be attached to an availability set. */
    private static final String availabilitySet = null;

    /** TODO: set to vm boot script */
    private static final String bootScript = Base64Utils
            .toBase64("#!/bin/bash\napt-get update -qy && apt-get install -qy apache2");
    /** TODO: set any tags to be applied to created VM. */
    private static final Map<String, String> tags = ImmutableMap.of();
    /** TODO: set to public ssh key */
    private static final Path publicKeyPath = Paths.get(System.getenv("HOME"), ".ssh", "id_rsa.pub");
    private static String publicKey;
    static {
        try {
            publicKey = new String(Files.readAllBytes(publicKeyPath));
        } catch (IOException e) {
            LOG.error("failed to read public ssh key {}: {}", publicKeyPath, e.getMessage());
            System.exit(1);
        }
    }
    private static Optional<LinuxSettings> linuxSettings = Optional
            .of(new LinuxSettings("ubuntu", publicKey, null, bootScript, null));
    private static Optional<WindowsSettings> windowsSettings = Optional.empty();

    private static final NetworkSettings network = new NetworkSettings(vnet, subnet, assignPublicIp, vmSecurityGroups);

    private static final VmSpec vmSpec = new VmSpec(vmSize, vmImage, osDiskType, vmName, linuxSettings, windowsSettings,
            network, availabilitySet, tags);

    public static void main(String[] args) throws CloudException, IOException {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS), LogLevel.BASIC);

        VmLauncher vmLauncher = new VmLauncher(apiAccess, resourceGroup, region);
        List<VirtualMachine> createdVms = vmLauncher.createVms(Arrays.asList(vmSpec));
        createdVms.stream().forEach(vm -> LOG.info("created vm {}: {}", vm.name()));
    }
}
