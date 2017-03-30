package com.elastisys.scale.cloudpool.azure.lab;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateVmsRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithFromImageCreateOptionsManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootUsernameManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPublicIpAddress;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.model.CreatedResources;
import com.microsoft.azure.management.storage.StorageAccount;
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
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        String vmSize = "Standard_DS1_v2";
        String vmName = String.format("testvm-%s", System.currentTimeMillis());

        String networkName = "itestnet";
        String subnetName = "default";
        boolean assignPublicIp = true;
        String imageRef = "Canonical:UbuntuServer:16.04.0-LTS:latest";
        String rootUserName = "ubuntu";
        String publicKey = Files.toString(new File(System.getenv("HOME"), ".ssh/id_rsa.pub"), Charsets.UTF_8);
        Map<String, String> tags = ImmutableMap.of("elastisys:cloudPool", "testpool");
        String bootScript = "#!/bin/bash\nsudo apt update -qy && sudo apt install -qy apache2";

        StorageAccount storageAccount = api.storageAccounts().getByGroup(resourceGroup, storageAccountName);

        Network network = api.networks().getByGroup(resourceGroup, networkName);
        LOG.debug("found network {}: {}", network.name(), network.id());
        if (!network.subnets().containsKey(subnetName)) {
            throw new IllegalArgumentException(String.format("subnet '%s' not found in network '%s' ('%s')", subnetName,
                    network.name(), network.id()));
        }

        WithPublicIpAddress vmDef = api.virtualMachines().define(vmName).withRegion(region)
                .withExistingResourceGroup(resourceGroup).withExistingPrimaryNetwork(network).withSubnet(subnetName)
                .withPrimaryPrivateIpAddressDynamic();
        WithOS vmDefWithIp;
        if (assignPublicIp) {
            String leafDnsLabel = vmName;
            vmDefWithIp = vmDef.withNewPrimaryPublicIpAddress(leafDnsLabel);
        } else {
            vmDefWithIp = vmDef.withoutPrimaryPublicIpAddress();
        }

        WithLinuxRootUsernameManagedOrUnmanaged vmDefWithOs = null;

        ImageReference imageReference = new VmImage(imageRef).getImageReference();
        vmDefWithOs = vmDefWithIp.withSpecificLinuxImageVersion(imageReference);

        WithFromImageCreateOptionsManaged vmDefWithLinux = vmDefWithOs.withRootUsername(rootUserName).withSsh(publicKey)
                .withCustomData(Base64Utils.toBase64(bootScript));
        vmDefWithLinux.withSize(vmSize);
        vmDefWithLinux.withTags(tags);
        vmDefWithLinux.withExistingStorageAccount(storageAccount);

        LOG.debug("creating VM ...");
        CreatedResources<VirtualMachine> createdVms = new CreateVmsRequest(apiAccess, Arrays.asList(vmDefWithLinux))
                .call();
        LOG.debug("VM created.");
        LOG.debug("VM: {}", createdVms.values());
    }

}
