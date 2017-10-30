package com.elastisys.scale.cloudpool.azure.lab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateVmsRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkInterfaceRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.CreatedResources;
import com.microsoft.rest.LogLevel;

/**
 * Lab program for creating a Linux VM in Azure from an existing generalized
 * Linux image. Such an image can be captured from a VM with managed disks.
 * Before capturing the VM image, the VM needs to be "generalized" by running
 * {@code sudo waagent -deprovision+user} (see [1]), before shutting the VM
 * down.
 * <p/>
 * Find the image id of the captured VM image and enter it below as
 * {@link #vmImageId}. The created VM will be cloned from the image.
 * <p/>
 * [1]
 * https://docs.microsoft.com/en-us/azure/virtual-machines/linux/classic/capture-image
 */
public class CreateVmFromCustomImageIdManagedOsDisk extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateVmFromCustomImageIdManagedOsDisk.class);

    /** TODO: set to region to create vm in */
    private static final Region region = Region.US_EAST;
    /** TODO: set to resource group to create vm under */
    private static final String resourceGroup = "itest";

    /** TODO: set to size of created VM. */
    private static final String vmSize = "Standard_DS1_v2";
    /** TODO: set to id of custom VM image. */
    private static final String vmImageId = "/subscriptions/6d328f78-195a-4246-a637-fcf73e43caf4/resourceGroups/itest/providers/Microsoft.Compute/images/ubuntu-apache";
    /** TODO: set to the type of disk to use for the managed OS disk */
    private static final StorageAccountTypes osDiskType = StorageAccountTypes.PREMIUM_LRS;
    /** TODO: set to name of created VM. */
    private static final String vmName = "my-vm-from-custom-image-managed";
    /**
     * TODO: set to name to an existing network interface card that is to be
     * attached to the VM. See {@link CreateNetworkInterface}.
     */
    private static final String nicName = "my-nic";

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
    private static LinuxSettings linuxSettings = new LinuxSettings("ubuntu", publicKey, null, bootScript, null);

    public static void main(String[] args) throws CloudException, IOException {

        AzureApiAccess apiAccess = new AzureApiAccess(SUBSCRIPTION_ID, AZURE_AUTH,
                new TimeInterval(300L, TimeUnit.SECONDS), new TimeInterval(300L, TimeUnit.SECONDS),
                LogLevel.BODY_AND_HEADERS);

        String nicId = String.format(
                "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s", SUBSCRIPTION_ID,
                resourceGroup, nicName);
        NetworkInterface networkInterface = new GetNetworkInterfaceRequest(apiAccess, nicId).call();

        Azure api = ApiUtils.acquireApiClient(apiAccess);

        WithOS rawVmDef = api.virtualMachines().define(vmName) //
                .withRegion(region) //
                .withExistingResourceGroup(resourceGroup) //
                .withExistingPrimaryNetworkInterface(networkInterface);
        WithLinuxCreateManaged linuxVm = rawVmDef //
                .withLinuxCustomImage(vmImageId) //
                .withRootUsername(linuxSettings.getRootUserName()) //
                .withRootPassword(linuxSettings.getPassword()) //
                .withSsh(linuxSettings.getPublicSshKey());//
        linuxVm.withComputerName(vmName);

        linuxVm.withOSDiskName(vmName);
        linuxVm.withOSDiskStorageAccountType(osDiskType);

        // add custom data (for example, cloud-init script)
        if (linuxSettings.getCustomData() != null) {
            linuxVm.withCustomData(linuxSettings.getCustomData());
        }

        WithCreate vmWithOs = linuxVm.withSize(vmSize);
        vmWithOs.withTags(tags);

        Creatable<VirtualMachine> vmDefinition = vmWithOs;

        CreatedResources<VirtualMachine> createdVms = new CreateVmsRequest(apiAccess, Arrays.asList(vmDefinition))
                .call();
        createdVms.forEach((name, vm) -> {
            LOG.info("created vm {}: {}", name, vm.id());
        });
    }
}
