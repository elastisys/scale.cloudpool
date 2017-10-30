package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.azure.driver.Constants;
import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.cloudpool.azure.driver.util.TagValidator;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;

/**
 * Azure-specific VM provisioning template.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class ProvisioningTemplate {
    /** Default for {@link #osDiskType}. */
    public static final String DEFAULT_OS_DISK_TYPE = StorageAccountTypes.STANDARD_LRS.toString();

    /** VM size to use for created VMs. */
    private final String vmSize;
    /**
     * VM image used to boot created VMs from. Can be specified either as an
     * image reference ({@code <publisher>:<offer>:<sku>[:<version>]}) to use an
     * existing image from the market place or as the id of a custom image
     * ({@code /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.Compute/images/<imageName>}).
     */
    private final String vmImage;

    /**
     * The disk type to use for the managed OS disk that will be created for
     * each VM. Allowed values are {@code Standard_LRS} (HDD-based) and
     * {@code Premium_LRS} (SSD-based). Premium storage may not be supported by
     * all types of VM series [1]. Default: {@code Standard_LRS}.
     * <p/>
     * [1]
     * https://docs.microsoft.com/en-us/azure/virtual-machines/windows/premium-storage
     */
    private final String osDiskType;
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

    /** Network settings for created VMs. */
    private final NetworkSettings network;

    /**
     * An existing availability set to which created VMs are to be added. May be
     * <code>null</code>. If <code>null</code>, created VMs will not be grouped
     * into an availability set.
     */
    private final String availabilitySet;

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
     *            Size to use for created VMs.
     * @param vmImage
     *            VM image used to boot created VMs from. Can be specified
     *            either as an image reference
     *            ({@code <publisher>:<offer>:<sku>[:<version>]}) to use an
     *            existing image from the market place or as the id of a custom
     *            image
     *            ({@code /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.Compute/images/<imageName>}).
     * @param osDiskType
     *            The disk type to use for the managed OS disk that will be
     *            created for each VM. Allowed values are {@code Standard_LRS}
     *            (HDD-based) and {@code Premium_LRS} (SSD-based). Premium
     *            storage may not be supported by all types of VM series [1].
     *            Default: {@code Standard_LRS}.
     *            <p/>
     *            [1]
     *            https://docs.microsoft.com/en-us/azure/virtual-machines/windows/premium-storage
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
     * @param availabilitySet
     *            An existing availability set to which created VMs are to be
     *            added. Optional. If <code>null</code>, created VMs will not be
     *            grouped into an availability set.
     * @param tags
     *            Tags to associate with created VMs. May be <code>null</code>.
     *            Note: the {@link Constants#CLOUD_POOL_TAG} will automatically
     *            be set and should not be overridden.
     */
    public ProvisioningTemplate(String vmSize, String vmImage, StorageAccountTypes osDiskType, String vmNamePrefix,
            LinuxSettings linuxSettings, WindowsSettings windowsSettings, NetworkSettings network,
            String availabilitySet, Map<String, String> tags) {
        this.vmSize = vmSize;
        this.vmImage = vmImage;
        this.osDiskType = osDiskType == null ? null : osDiskType.toString();
        this.vmNamePrefix = vmNamePrefix;
        this.linuxSettings = linuxSettings;
        this.windowsSettings = windowsSettings;
        this.network = network;
        this.availabilitySet = availabilitySet;
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
     * VM image used to boot created VMs from. Can be specified either as an
     * image reference ({@code <publisher>:<offer>:<sku>[:<version>]}) to use an
     * existing image from the market place or as the id of a custom image
     * ({@code /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.Compute/images/<imageName>}).
     *
     * @return
     */
    public VmImage getVmImage() {
        return new VmImage(this.vmImage);
    }

    /**
     * The disk type to use for the managed OS disk that will be created for
     * each VM. Allowed values are {@code Standard_LRS} (HDD-based) and
     * {@code Premium_LRS} (SSD-based). Premium storage may not be supported by
     * all types of VM series [1]. Default: {@code Standard_LRS}.
     * <p/>
     * [1]
     * https://docs.microsoft.com/en-us/azure/virtual-machines/windows/premium-storage
     *
     * @return
     */
    public StorageAccountTypes getOsDiskType() {
        return toStorageAccountType(Optional.ofNullable(this.osDiskType).orElse(DEFAULT_OS_DISK_TYPE));
    }

    /**
     * Returns the {@link StorageAccountTypes} that a given string represents or
     * throws an {@link IllegalArgumentException} if the account type is not
     * recognized.
     *
     * @param storageAccountType
     * @return
     * @throws IllegalArgumentException
     */
    private StorageAccountTypes toStorageAccountType(String storageAccountType) throws IllegalArgumentException {
        StorageAccountTypes accountType = StorageAccountTypes.fromString(storageAccountType);
        if (accountType == null) {
            throw new IllegalArgumentException("unrecognized storage account type: " + storageAccountType);
        }
        return accountType;
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
     * Network settings for created VMs.
     *
     * @return the network
     */
    public NetworkSettings getNetwork() {
        return this.network;
    }

    /**
     * An existing availability set to which created VMs are to be added. If
     * left out, created VMs will not be grouped into an availability set.
     *
     * @return
     */
    public Optional<String> getAvailabilitySet() {
        return Optional.ofNullable(this.availabilitySet);
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
        return Objects.hash(this.vmSize, this.vmImage, this.osDiskType, this.vmNamePrefix, this.linuxSettings,
                this.windowsSettings, this.network, this.availabilitySet, this.tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningTemplate) {
            ProvisioningTemplate that = (ProvisioningTemplate) obj;
            return Objects.equals(this.vmSize, that.vmSize) //
                    && Objects.equals(this.vmImage, that.vmImage) //
                    && Objects.equals(this.osDiskType, that.osDiskType) //
                    && Objects.equals(this.vmNamePrefix, that.vmNamePrefix) //
                    && Objects.equals(this.linuxSettings, that.linuxSettings) //
                    && Objects.equals(this.windowsSettings, that.windowsSettings) //
                    && Objects.equals(this.network, that.network) //
                    && Objects.equals(this.availabilitySet, that.availabilitySet)
                    && Objects.equals(this.tags, that.tags);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.vmSize != null, "provisioningTemplate: no vmSize given");
        checkArgument(this.vmImage != null, "provisioningTemplate: no vmImage given");

        try {
            getOsDiskType();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("provisioningTemplate: osDiskType: " + e.getMessage());
        }

        checkArgument(this.linuxSettings != null || this.windowsSettings != null,
                "provisioningTemplate: neither linuxSettings nor windowsSettings given");
        checkArgument(this.linuxSettings != null ^ this.windowsSettings != null,
                "provisioningTemplate: may only specify one of linuxSettings and windowsSettings, not both");

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

    public static Builder builder(VirtualMachineSizeTypes vmSize, String vmImage, NetworkSettings network) {
        return new Builder(vmSize, vmImage, network);
    }

    public static class Builder {
        private final VirtualMachineSizeTypes vmSize;
        private final String vmImage;
        private StorageAccountTypes osDiskType;
        private String vmNamePrefix;
        private LinuxSettings linuxSettings;
        private WindowsSettings windowsSettings;
        private final NetworkSettings network;
        private String availabilitySet;
        private Map<String, String> tags = new HashMap<>();

        public Builder(VirtualMachineSizeTypes vmSize, String vmImage, NetworkSettings network) {
            this.vmSize = vmSize;
            this.vmImage = vmImage;
            this.network = network;
        }

        public Builder osDiskType(StorageAccountTypes osDiskType) {
            this.osDiskType = osDiskType;
            return this;
        }

        public Builder vmNamePrefix(String vmNamePrefix) {
            this.vmNamePrefix = vmNamePrefix;
            return this;
        }

        public Builder linuxSettings(LinuxSettings linuxSettings) {
            this.linuxSettings = linuxSettings;
            return this;
        }

        public Builder windowsSettings(WindowsSettings windowsSettings) {
            this.windowsSettings = windowsSettings;
            return this;
        }

        public Builder availabilitySet(String availabilitySet) {
            this.availabilitySet = availabilitySet;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder tag(String key, String value) {
            this.tags.put(key, value);
            return this;
        }

        public ProvisioningTemplate build() {
            ProvisioningTemplate template = new ProvisioningTemplate(this.vmSize.toString(), this.vmImage,
                    this.osDiskType, this.vmNamePrefix, this.linuxSettings, this.windowsSettings, this.network,
                    this.availabilitySet, this.tags);
            template.validate();
            return template;
        }
    }
}
