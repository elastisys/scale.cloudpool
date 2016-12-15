package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.azure.driver.Constants;
import com.elastisys.scale.cloudpool.azure.driver.util.TagValidator;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Azure-specific VM provisioning template.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class ProvisioningTemplate {

    /** VM size to use for VM. */
    private final String vmSize;
    /** Image used to boot VM. */
    private final String vmImage;
    /**
     * Name prefix to assign to created VMs. The cloudpool will add a VM-unique
     * suffix to the prefix to produce the final VM name:
     * {@code <vmNamePrefix>-<suffix>}. May be <code>null</code>. If left out,
     * the default is to use the cloud pool's name as VM name prefix.
     */
    private final String vmNamePrefix;

    /**
     * Settings particular to Linux VMs. May be <code>null</code>, if
     * {@link #windowsSettings} is specified.
     */
    private final LinuxSettings linuxSettings;

    /**
     * Settings particular to Windows VMs. May be <code>null</code>, if
     * {@link #linuxSettings} is specified.
     */
    private final WindowsSettings windowsSettings;

    /**
     * An existing storage account used to store the OS data disk VHD for
     * created VMs.
     */
    private final String storageAccountName;

    /** Network settings for created VMs. */
    private final NetworkSettings network;

    /**
     * Tags to associate with created VMs. Note: the
     * {@link Constants#CLOUD_POOL_TAG} will automatically be set and should not
     * be overridden.
     */
    private final Map<String, String> tags;

    /**
     * Creates a {@link ProvisioningTemplate}.
     *
     * @param vmSize
     * @param vmImage
     * @param vmNamePrefix
     *            Name prefix to assign to created VMs. The cloudpool will add a
     *            VM-unique suffix to the prefix to produce the final VM name:
     *            {@code <vmNamePrefix>-<suffix>}. May be <code>null</code>. If
     *            left out, the default is to use the cloud pool's name as VM
     *            name prefix.
     * @param linuxSettings
     *            Settings for creating Linux VMs. Optional if
     *            {@code windowsSettings} is given, otherwise required.
     * @param windowsSettings
     *            Settings for creating Windows VMs. Optional if
     *            {@code linuxSettings} is given, otherwise required.
     * @param storageAccountName
     *            An existing storage account used to store the OS disk VHD for
     *            created VMs.
     * @param network
     *            Network settings for created VMs. Required.
     * @param tags
     *            Tags to associate with created VMs. May be <code>null</code>.
     *            Note: the {@link Constants#CLOUD_POOL_TAG} will automatically
     *            be set and should not be overridden.
     */
    public ProvisioningTemplate(String vmSize, String vmImage, String vmNamePrefix, LinuxSettings linuxSettings,
            WindowsSettings windowsSettings, String storageAccountName, NetworkSettings network,
            Map<String, String> tags) {
        this.vmSize = vmSize;
        this.vmImage = vmImage;
        this.vmNamePrefix = vmNamePrefix;
        this.linuxSettings = linuxSettings;
        this.windowsSettings = windowsSettings;
        this.storageAccountName = storageAccountName;
        this.network = network;
        this.tags = tags;
    }

    /**
     * VM size to use for VM.
     *
     * @return
     */
    public String getVmSize() {
        return this.vmSize;
    }

    /**
     * Image used to boot VM.
     *
     * @return
     */
    public String getVmImage() {
        return this.vmImage;
    }

    /**
     * Name prefix to assign to created VMs. The cloudpool will add a VM-unique
     * suffix to the prefix to produce the final VM name:
     * {@code <vmNamePrefix>-<suffix>}. If left out, the default is to use the
     * cloud pool's name as VM name prefix.
     *
     * @return
     */
    public Optional<String> getVmNamePrefix() {
        return Optional.ofNullable(this.vmNamePrefix);
    }

    /**
     * Settings particular to creating Linux VMs. Will only be set if this pool
     * creates Linux VMs.
     *
     * @return the linuxSettings
     */
    public Optional<LinuxSettings> getLinuxSettings() {
        return Optional.ofNullable(this.linuxSettings);
    }

    /**
     * Settings particular to creating Windows VMs. Will only be set if this
     * pool creates Windows VMs.
     *
     * @return
     */
    public Optional<WindowsSettings> getWindowsSettings() {
        return Optional.ofNullable(this.windowsSettings);
    }

    /**
     * An existing storage account used to store the OS disk VHD for created
     * VMs.
     *
     * @return
     */
    public String getStorageAccountName() {
        return this.storageAccountName;
    }

    /**
     * Network settings for created VMs.
     *
     * @return the network
     */
    public NetworkSettings getNetwork() {
        return this.network;
    }

    /**
     * Tags to associate with created VMs. Note: the
     * {@link Constants#CLOUD_POOL_TAG} will automatically be set and should not
     * be overridden.
     *
     * @return the tags
     */
    public Map<String, String> getTags() {
        return Optional.ofNullable(this.tags).orElse(new HashMap<>());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vmSize, this.vmImage, this.vmNamePrefix, this.linuxSettings, this.storageAccountName,
                this.network, this.tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningTemplate) {
            ProvisioningTemplate that = (ProvisioningTemplate) obj;
            return Objects.equals(this.vmSize, that.vmSize) //
                    && Objects.equals(this.vmImage, that.vmImage) //
                    && Objects.equals(this.vmNamePrefix, that.vmNamePrefix)
                    && Objects.equals(this.linuxSettings, that.linuxSettings)
                    && Objects.equals(this.storageAccountName, that.storageAccountName)
                    && Objects.equals(this.network, that.network) && Objects.equals(this.tags, that.tags);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.vmSize != null, "provisioningTemplate: no vmSize given");
        checkArgument(this.vmImage != null, "provisioningTemplate: no vmImage given");
        checkArgument(this.linuxSettings != null || this.windowsSettings != null,
                "provisioningTemplate: neither linuxSettings nor windowsSettings given");
        checkArgument(this.linuxSettings != null ^ this.windowsSettings != null,
                "provisioningTemplate: may only specify one of linuxSettings and windowsSettings, not both");
        checkArgument(this.storageAccountName != null, "provisioningTemplate: no storageAccountName given");
        checkArgument(this.network != null, "provisioningTemplate: no network given");

        try {
            if (this.linuxSettings != null) {
                this.linuxSettings.validate();
            }
            if (this.windowsSettings != null) {
                this.windowsSettings.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("provisioningTemplate: " + e.getMessage(), e);
        }

        this.network.validate();

        new TagValidator().validate(getTags());
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
