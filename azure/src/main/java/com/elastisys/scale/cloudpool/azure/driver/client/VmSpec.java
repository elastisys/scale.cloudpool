package com.elastisys.scale.cloudpool.azure.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.WindowsSettings;
import com.elastisys.scale.cloudpool.azure.driver.util.TagValidator;
import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.management.compute.StorageAccountTypes;

/**
 * Captures provisioning details for a single Azure VM to be created via the
 * {@link AzureClient}.
 *
 * @see AzureClient
 */
public class VmSpec {

    /** VM size to use for VM. */
    private final String vmSize;
    /** Image used to boot VM. */
    private final VmImage vmImage;

    /**
     * The disk type to use for the managed OS disk that will be created for the
     * VM.
     */
    private final StorageAccountTypes osDiskType;

    /** Name to assign to the created VM. */
    private final String vmName;

    /**
     * Settings for creating a Linux VM. Should be set unless a Windows VM is to
     * be created.
     */
    private final Optional<LinuxSettings> linuxSettings;

    /**
     * Settings for creating a Windows VM.Should be set unless a Linux VM is to
     * be created.
     */
    private final Optional<WindowsSettings> windowsSettings;

    /** Network settings for the created VM. */
    private final NetworkSettings network;

    /**
     * An existing availability set to which created VMs are to be added. May be
     * <code>null</code>. If left out, created VMs will not be grouped into an
     * availability set.
     */
    private final String availabilitySet;

    /** Tags to associate with the created VM. */
    private final Map<String, String> tags;

    /**
     * Creates a {@link VmSpec} for a VM to be created.
     *
     * @param vmSize
     *            VM size to use for VM.
     * @param vmImage
     *            Image used to boot VM.
     * @param osDiskType
     *            The disk type to use for the managed OS disk that will be
     *            created for the VM.
     * @param vmName
     *            Name to assign to the created VM.
     * @param linuxSettings
     *            Settings for creating a Linux VM. May be <code>null</code> if
     *            {@link #windowsSettings} is given.
     * @param windowsSettings
     *            Settings for creating a Windows VM. May be <code>null</code>
     *            if {@link #linuxSettings} is given.
     * @param network
     *            Network settings for the created VM.
     * @param availabilitySet
     *            An existing availability set to which created VMs are to be
     *            added. Optional. If left out, created VMs will not be grouped
     *            into an availability set.
     *
     * @param tags
     *            Tags to associate with the created VM.
     */
    public VmSpec(String vmSize, VmImage vmImage, StorageAccountTypes osDiskType, String vmName,
            Optional<LinuxSettings> linuxSettings, Optional<WindowsSettings> windowsSettings, NetworkSettings network,
            String availabilitySet, Map<String, String> tags) {
        this.vmSize = vmSize;
        this.vmImage = vmImage;
        this.osDiskType = osDiskType;
        this.vmName = vmName;
        this.linuxSettings = linuxSettings;
        this.windowsSettings = windowsSettings;
        this.network = network;
        this.availabilitySet = availabilitySet;
        this.tags = tags;

        validate();
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
    public VmImage getVmImage() {
        return this.vmImage;
    }

    /**
     * The disk type to use for the managed OS disk that will be created for the
     * VM.
     *
     * @return
     */
    public StorageAccountTypes getOsDiskType() {
        return this.osDiskType;
    }

    /**
     * Name to assign to the created VM.
     *
     * @return
     */
    public String getVmName() {
        return this.vmName;
    }

    /**
     * Settings for creating a Linux VM. Will only be set if this is a
     * {@link VmSpec} for a Linux VM.
     *
     * @return
     */
    public Optional<LinuxSettings> getLinuxSettings() {
        return this.linuxSettings;
    }

    /**
     * Settings for creating a Windows VM. Will only be set if this is a
     * {@link VmSpec} for a Windows VM.
     *
     * @return
     */
    public Optional<WindowsSettings> getWindowsSettings() {
        return this.windowsSettings;
    }

    /**
     * Network settings for the created VM.
     *
     * @return
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
     * Tags to associate with the created VM.
     *
     * @return
     */
    public Map<String, String> getTags() {
        return this.tags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vmSize, this.vmImage, this.osDiskType, this.vmName, this.linuxSettings,
                this.windowsSettings, this.network, this.availabilitySet, this.tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VmSpec) {
            VmSpec that = (VmSpec) obj;
            return Objects.equals(this.vmSize, that.vmSize) //
                    && Objects.equals(this.vmImage, that.vmImage) //
                    && Objects.equals(this.vmName, that.vmName) //
                    && Objects.equals(this.osDiskType, that.osDiskType) //
                    && Objects.equals(this.linuxSettings, that.linuxSettings)
                    && Objects.equals(this.windowsSettings, that.windowsSettings) //
                    && Objects.equals(this.network, that.network) //
                    && Objects.equals(this.availabilitySet, that.availabilitySet) //
                    && Objects.equals(this.tags, that.tags);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.vmSize != null, "vmSpec: no vmSize given");
        checkArgument(this.vmImage != null, "vmSpec: no vmImage given");
        checkArgument(this.osDiskType != null, "vmSpec: no osDiskType given");
        checkArgument(this.vmName != null, "vmSpec: no vmName given");
        checkArgument(this.linuxSettings != null, "vmSpec: linuxSettings is null");
        checkArgument(this.windowsSettings != null, "vmSpec: windowsSettings is null");
        checkArgument(this.linuxSettings.isPresent() || this.windowsSettings.isPresent(),
                "vmSpec: neither  linuxSettings nor windowsSettings given");
        checkArgument(this.linuxSettings.isPresent() ^ this.windowsSettings.isPresent(),
                "vmSpec: may only specify one of linuxSettings and windowsSettings, not both");
        checkArgument(this.network != null, "vmSpec: no network given");
        checkArgument(this.tags != null, "vmSpec: tags cannot be null");

        if (this.linuxSettings.isPresent()) {
            this.linuxSettings.get().validate();
        }
        if (this.windowsSettings.isPresent()) {
            this.windowsSettings.get().validate();
        }
        this.network.validate();

        new TagValidator().validate(getTags());
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
