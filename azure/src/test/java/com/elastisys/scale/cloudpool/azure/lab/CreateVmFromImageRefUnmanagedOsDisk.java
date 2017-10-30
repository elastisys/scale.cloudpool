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

import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateVmsRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkInterfaceRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetStorageAccountRequest;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.CreatedResources;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.rest.LogLevel;

/**
 * Lab program for creating a Linux VM in Azure from an image reference (such as
 * {@code Canonical:UbuntuServer:16.04.0-LTS:latest}). The created OS disk will
 * be unmanaged, so a storage account needs to be created beforehand and
 * provided for Azure to store the OS disk in.
 */
public class CreateVmFromImageRefUnmanagedOsDisk extends BaseLabProgram {

    private static final Logger LOG = LoggerFactory.getLogger(CreateVmFromImageRefUnmanagedOsDisk.class);

    /** TODO: set to region to create vm in */
    private static final Region region = Region.US_EAST;
    /** TODO: set to resource group to create vm under */
    private static final String resourceGroup = "itest";

    /** TODO: set to size of created VM. */
    private static final String vmSize = "Standard_DS1_v2";
    /** TODO: set to image of created VM. */
    private static final String vmImage = "Canonical:UbuntuServer:16.04.0-LTS:latest";
    /** TODO: set to name of created VM. */
    private static final String vmName = "my-vm-from-imageref-unmanaged";
    /** TODO: set to storage account to store disks of created VM. */
    private static final String storageAccountName = "itestdisks";
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

        VmImage image = new VmImage(vmImage);
        Azure api = ApiUtils.acquireApiClient(apiAccess);

        WithOS rawVmDef = api.virtualMachines().define(vmName) //
                .withRegion(region) //
                .withExistingResourceGroup(resourceGroup) //
                .withExistingPrimaryNetworkInterface(networkInterface);
        WithLinuxCreateManagedOrUnmanaged linuxVm = rawVmDef //
                .withSpecificLinuxImageVersion(image.getImageReference()) //
                .withRootUsername(linuxSettings.getRootUserName()) //
                .withRootPassword(linuxSettings.getPassword()) //
                .withSsh(linuxSettings.getPublicSshKey());
        linuxVm.withComputerName(vmName);

        // we want unmanaged disks. that is, we want to provide our own storage
        // account where the os disk is to be stored.
        linuxVm.withUnmanagedDisks();
        StorageAccount storageAccount = new GetStorageAccountRequest(apiAccess, resourceGroup, storageAccountName)
                .call();
        linuxVm.withExistingStorageAccount(storageAccount);
        linuxVm.withOSDiskName(vmName);

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
