package com.elastisys.scale.cloudpool.azure.lab;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPublicIpAddress;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithRootUserName;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.storage.StorageAccount;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Lab program for creating a Linux VM in Azure.
 */
public class CreateLinuxVirtualMachine extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateLinuxVirtualMachine.class);

    public static void main(String[] args) throws CloudException, IOException {
        String resourceGroup = "testpool";
        String region = "northeurope";

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(10L, TimeUnit.SECONDS), new TimeInterval(10L, TimeUnit.SECONDS),
                HttpLoggingInterceptor.Level.BASIC);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        String vmSize = "Standard_DS1_v2";
        String vmName = String.format("testvm-%s", System.currentTimeMillis());

        String networkName = "testnet";
        String subnetName = "default";
        boolean assignPublicIp = true;
        String imageRef = "Canonical:UbuntuServer:16.04.0-LTS:latest";
        String imageUrl = null;
        String rootUserName = "ubuntu";
        String publicKey = Files.toString(new File(System.getenv("HOME"), ".ssh/id_rsa.pub"), Charsets.UTF_8);
        Map<String, String> tags = ImmutableMap.of("elastisys:cloudPool", "testpool");
        // Disks for Azure VMs are created in storage accounts
        String storageAccountName = "testpooldisks";
        String availabilitySetName = null;

        List<String> linuxExtFileUris = Arrays.asList(); // don't set to null!
        String linuxExtCommandToExecute = "sh -c 'apt update -qy && apt install -qy apache2'";

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
            // TODO: assign public ip address as a separate step after vm
            // creation? (otherwise dangling IPs may be created)
            // vm name used as sensible default for dns name
            String leafDnsLabel = vmName;
            vmDefWithIp = vmDef.withNewPrimaryPublicIpAddress(leafDnsLabel);
        } else {
            vmDefWithIp = vmDef.withoutPrimaryPublicIpAddress();
        }

        WithRootUserName vmDefWithOs = null;
        Preconditions.checkArgument(imageUrl != null ^ imageRef != null,
                "specify exactly one of imageUrl and imageRef");
        if (imageUrl != null) {
            LOG.debug("using image URL: {}", imageUrl);
            vmDefWithOs = vmDefWithIp.withStoredLinuxImage(imageUrl);
        }
        if (imageRef != null) {
            ImageReference imageReference = parseImageReference(imageRef);
            LOG.debug("using image reference: {}", JsonUtils.toJson(imageReference));
            vmDefWithOs = vmDefWithIp.withSpecificLinuxImageVersion(imageReference);
        }

        WithLinuxCreate vmDefWithLinux = vmDefWithOs.withRootUserName(rootUserName).withSsh(publicKey);
        vmDefWithLinux.withSize(vmSize);
        // TODO: availability set
        // TODO: data disk
        vmDefWithLinux.withTags(tags);
        if (storageAccountName != null) {
            StorageAccount storageAccount = api.storageAccounts().getByGroup(resourceGroup, storageAccountName);
            // TODO: re-raise with other exception type on error?
            vmDefWithLinux.withExistingStorageAccount(storageAccount);
        } else {
            vmDefWithLinux.withNewStorageAccount(vmName + "-storageaccount");
        }
        if (availabilitySetName != null) {
            AvailabilitySet availabilitySet = api.availabilitySets().getByGroup(resourceGroup, availabilitySetName);
            vmDefWithLinux.withExistingAvailabilitySet(availabilitySet);
        }
        vmDefWithLinux.defineNewExtension("CustomScript").withPublisher("Microsoft.Azure.Extensions")
                .withType("CustomScript").withVersion("2.0").withPublicSetting("fileUris", linuxExtFileUris)
                .withPublicSetting("commandToExecute", linuxExtCommandToExecute).attach();

        LOG.debug("creating VM ...");
        WithCreate vmdef = vmDefWithLinux;
        VirtualMachine createdVm = vmDefWithLinux.create();
        LOG.debug("VM created.");
        LOG.debug("VM: {}", createdVm);
    }

    private static ImageReference parseImageReference(String imageRef) {
        Pattern imageRefRegexp = Pattern.compile("([^:]+):([^:]+):([^:]+)(:([^:]+))?");
        Matcher matcher = imageRefRegexp.matcher(imageRef);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("illegal image reference: " + imageRef);
        }
        ImageReference image = new ImageReference().withPublisher(matcher.group(1)).withOffer(matcher.group(2))
                .withSku(matcher.group(3));
        if (matcher.groupCount() > 3) {
            image.withVersion(matcher.group(5));
        } else {
            image.withVersion("latest");
        }
        return image;
    }

}
