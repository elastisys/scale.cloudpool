package com.elastisys.scale.cloudpool.azure.driver.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.CustomScriptExtension;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.WindowsSettings;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateNetworkInterfaceRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.CreateVmsRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.DeleteNetworkInterfaceRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetAvailabilitySetRequest;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.base.Charsets;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithFromImageCreateOptionsManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.CreatedResources;

/**
 * A launcher of Azure VM definitions.
 */
public class VmLauncher {
    private static final String LINUX_CUSTOM_SCRIPT_EXT_TYPE_VERSION = "2.0";
    private static final String LINUX_CUSTOM_SCRIPT_EXT_TYPE = "CustomScript";
    private static final String LINUX_CUSTOM_SCRPIT_EXT_PUBLISHER = "Microsoft.Azure.Extensions";

    private static final String WINDOWS_CUSTOM_SCRIPT_EXT_TYPE_VERSION = "1.8";
    private static final String WINDOWS_CUSTOM_SCRIPT_EXT_TYPE = "CustomScriptExtension";
    private static final String WINDOWS_CUSTOM_SCRPIT_EXT_PUBLISHER = "Microsoft.Compute";

    private static final Logger LOG = LoggerFactory.getLogger(VmLauncher.class);

    private final AzureApiAccess apiAccess;
    private final String resourceGroup;
    private final Region region;

    public VmLauncher(AzureApiAccess apiAccess, String resourceGroup, Region region) {
        this.apiAccess = apiAccess;
        this.resourceGroup = resourceGroup;
        this.region = region;
    }

    /**
     * Creates a number of VMs according to a list of provisioning specs.
     *
     * @param vmSpecs
     *            Provisioning specs for a collection of VMs.
     * @return
     */
    public List<VirtualMachine> createVms(List<VmSpec> vmSpecs) throws NotFoundException, AzureException {
        // first create one network interface per VM
        Map<String, NetworkInterface> vmNetworkInterfaces = new HashMap<>();
        for (VmSpec vmSpec : vmSpecs) {
            vmNetworkInterfaces.put(vmSpec.getVmName(), createNetworkInterface(vmSpec));
        }

        try {
            // create each VM definition and associate with a network interface
            List<Creatable<VirtualMachine>> vmDefinitions = new ArrayList<>();
            for (VmSpec vmSpec : vmSpecs) {
                NetworkInterface vmNetworkInterface = vmNetworkInterfaces.get(vmSpec.getVmName());
                vmDefinitions.add(renderVmDefinition(vmSpec, vmNetworkInterface));
            }

            // launch
            CreatedResources<VirtualMachine> createdVms = new CreateVmsRequest(this.apiAccess, vmDefinitions).call();
            return new ArrayList<>(createdVms.values());
        } catch (Exception e) {
            LOG.error("VM launch failed: {}", e.getMessage(), e);
            LOG.info("attempting to clean up created network interfaces after failed VM launch ...");
            cleanupNetworkInterfaces(vmNetworkInterfaces.values());
            throw e;
        }

    }

    /**
     * Creates a network interface for the given VM, possibly with associated
     * security groups and public IP.
     *
     * @param vmSpec
     *            A VM to be created.
     * @return
     */
    private NetworkInterface createNetworkInterface(VmSpec vmSpec) throws NotFoundException, AzureException {
        // create a network interface for VM (possibly with security groups
        // and public IP)
        String vmName = vmSpec.getVmName();
        LOG.debug("creating network interface {} for VM ...", vmName);
        NetworkInterface networkInterface = new CreateNetworkInterfaceRequest(this.apiAccess, this.resourceGroup,
                this.region, vmName, vmSpec.getNetwork()).call();
        return networkInterface;
    }

    /**
     * Creates a VM definition from a given {@link VmSpec} and with a given
     * network interface.
     *
     * @param vmSpec
     * @param vmNetworkInterface
     * @return
     */
    public Creatable<VirtualMachine> renderVmDefinition(VmSpec vmSpec, NetworkInterface vmNetworkInterface) {
        String vmName = vmSpec.getVmName();

        Azure api = ApiUtils.acquireApiClient(this.apiAccess);
        WithOS rawVmDef = api.virtualMachines().define(vmName) //
                .withRegion(this.region) //
                .withExistingResourceGroup(this.resourceGroup) //
                .withExistingPrimaryNetworkInterface(vmNetworkInterface);

        if (vmSpec.getLinuxSettings().isPresent()) {
            return linuxVmDefinition(vmSpec, rawVmDef);
        } else {
            return windowsVmDefinition(vmSpec, rawVmDef);
        }
    }

    private Creatable<VirtualMachine> linuxVmDefinition(VmSpec vmSpec, WithOS rawVmDef) {
        VmImage vmImage = vmSpec.getVmImage();
        LinuxSettings linuxSettings = vmSpec.getLinuxSettings().get();

        // NOTE: due to the poor azure SDK design we need two separate, almost
        // identical, VM builder call sequences depending on if using an
        // Azure-managed VM image or a private VM image.
        WithFromImageCreateOptionsManaged linuxVm;
        if (vmImage.isImageReference()) {
            linuxVm = rawVmDef.withSpecificLinuxImageVersion(vmImage.getImageReference()) //
                    .withRootUsername(linuxSettings.getRootUserName()) //
                    .withRootPassword(linuxSettings.getPassword()) //
                    .withSsh(linuxSettings.getPublicSshKey()) //
                    .withComputerName(vmSpec.getVmName());
        } else {
            linuxVm = rawVmDef.withLinuxCustomImage(vmImage.getImageId()) //
                    .withRootUsername(linuxSettings.getRootUserName()) //
                    .withRootPassword(linuxSettings.getPassword()) //
                    .withSsh(linuxSettings.getPublicSshKey()) //
                    .withComputerName(vmSpec.getVmName());
        }
        linuxVm.withOSDiskName(vmSpec.getVmName());
        linuxVm.withOSDiskStorageAccountType(vmSpec.getOsDiskType());

        // add custom data (for example, cloud-init script)
        if (linuxSettings.getCustomData() != null) {
            linuxVm.withCustomData(linuxSettings.getCustomData());
        }

        // if specified, add VM to availability set
        if (vmSpec.getAvailabilitySet().isPresent()) {
            String availabilitySet = vmSpec.getAvailabilitySet().get();
            AvailabilitySet set = new GetAvailabilitySetRequest(this.apiAccess, this.resourceGroup, availabilitySet)
                    .call();
            linuxVm.withExistingAvailabilitySet(set);
        }

        linuxVm.withSize(vmSpec.getVmSize());
        linuxVm.withTags(vmSpec.getTags());

        // add custom boot script to be executed
        CustomScriptExtension customScript = linuxSettings.getCustomScript();
        if (customScript != null) {
            attachLinuxCustomScript(linuxVm, customScript);
        }
        return linuxVm;
    }

    private Creatable<VirtualMachine> windowsVmDefinition(VmSpec vmSpec, WithOS rawVmDef) {
        VmImage vmImage = vmSpec.getVmImage();
        WindowsSettings windowsSettings = vmSpec.getWindowsSettings().get();

        // NOTE: due to the poor azure SDK design we need two separate, almost
        // identical, VM builder call sequences depending on if using an
        // Azure-managed VM image or a private VM image.
        WithFromImageCreateOptionsManaged windowsVm;
        if (vmImage.isImageReference()) {
            windowsVm = rawVmDef.withSpecificWindowsImageVersion(vmImage.getImageReference()) //
                    .withAdminUsername(windowsSettings.getAdminUserName()) //
                    .withAdminPassword(windowsSettings.getPassword()) //
                    .withComputerName(vmSpec.getVmName());
        } else {
            windowsVm = rawVmDef.withWindowsCustomImage(vmImage.getImageId()) //
                    .withAdminUsername(windowsSettings.getAdminUserName()) //
                    .withAdminPassword(windowsSettings.getPassword()) //
                    .withComputerName(vmSpec.getVmName());
        }
        windowsVm.withOSDiskName(vmSpec.getVmName());
        windowsVm.withOSDiskStorageAccountType(vmSpec.getOsDiskType());

        // if specified, add VM to availability set
        if (vmSpec.getAvailabilitySet().isPresent()) {
            String availabilitySet = vmSpec.getAvailabilitySet().get();
            AvailabilitySet set = new GetAvailabilitySetRequest(this.apiAccess, this.resourceGroup, availabilitySet)
                    .call();
            windowsVm.withExistingAvailabilitySet(set);
        }

        windowsVm.withTags(vmSpec.getTags());

        // add custom boot script to be executed
        CustomScriptExtension customScript = windowsSettings.getCustomScript();
        if (customScript != null) {
            attachWindowsCustomScript(windowsVm, customScript);
        }
        return windowsVm;
    }

    /**
     * Attaches a Linux custom script extension to a VM being defined.
     *
     * @param vmWithOs
     *            VM being defined.
     * @param customScript
     *            Custom script extension config.
     */
    private void attachLinuxCustomScript(WithCreate vmWithOs, CustomScriptExtension customScript) {
        List<String> fileDownloads = customScript.getFileUris();
        String command = Base64Utils.fromBase64(customScript.getEncodedCommand(), Charsets.UTF_8);
        vmWithOs.defineNewExtension(LINUX_CUSTOM_SCRIPT_EXT_TYPE) //
                .withPublisher(LINUX_CUSTOM_SCRPIT_EXT_PUBLISHER) //
                .withType(LINUX_CUSTOM_SCRIPT_EXT_TYPE) //
                .withVersion(LINUX_CUSTOM_SCRIPT_EXT_TYPE_VERSION) //
                .withPublicSetting("fileUris", fileDownloads) //
                .withPublicSetting("commandToExecute", command).attach();
    }

    /**
     * Attaches a Linux custom script extension to a VM being defined.
     *
     * @param vmWithOs
     *            VM being defined.
     * @param customScript
     *            Custom script extension config.
     */
    private void attachWindowsCustomScript(WithCreate vmWithOs, CustomScriptExtension customScript) {
        List<String> fileDownloads = customScript.getFileUris();
        String command = Base64Utils.fromBase64(customScript.getEncodedCommand(), Charsets.UTF_8);
        vmWithOs.defineNewExtension(WINDOWS_CUSTOM_SCRIPT_EXT_TYPE) //
                .withPublisher(WINDOWS_CUSTOM_SCRPIT_EXT_PUBLISHER) //
                .withType(WINDOWS_CUSTOM_SCRIPT_EXT_TYPE) //
                .withVersion(WINDOWS_CUSTOM_SCRIPT_EXT_TYPE_VERSION) //
                .withPublicSetting("fileUris", fileDownloads) //
                .withPublicSetting("commandToExecute", command).attach();
    }

    /**
     * Removes a collection of network interfaces (together with any associated
     * public IPs).
     *
     * @param networkInterfaces
     */
    private void cleanupNetworkInterfaces(Collection<NetworkInterface> networkInterfaces) {
        for (NetworkInterface networkInterface : networkInterfaces) {
            try {
                LOG.info("cleaning up network interface {} ...", networkInterface.id());
                new DeleteNetworkInterfaceRequest(this.apiAccess, networkInterface.id()).call();
            } catch (Exception e) {
                LOG.warn("failed to clean up network interface {}: {}", networkInterface.id(), e.getMessage(), e);
            }
        }
    }

}
