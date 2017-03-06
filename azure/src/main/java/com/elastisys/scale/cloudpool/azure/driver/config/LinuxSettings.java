package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * Settings specific to launching Linux VMs.
 *
 * @see ProvisioningTemplate
 */
public class LinuxSettings {
    public static final String ALLOWED_USERNAME_REGEXP = "^[a-z_][a-z0-9_-]*$";

    public static String DEFAULT_ROOT_USER = "root";

    /**
     * Name of the root Linux account to create on created VMs. May be
     * <code>null</code>. Default: {@link #DEFAULT_ROOT_USER}.
     */
    private final String rootUserName;

    /**
     * An OpenSSH public key used to login to created VMs. For example,
     * {@code ssh-rsa ABCD...123 user@host}. May be {@code null} (if so, a
     * {@link #password} must be set).
     */
    private final String publicSshKey;
    /**
     * A password used to login to created VMs. May be {@code null} (if so, a
     * {@link #publicSshKey} must be set).
     */
    private final String password;

    /**
     * Allows passing of base64-encoded custom data to the VM. This can, for
     * example, be used to pass a
     * <a href="https://cloudinit.readthedocs.io/en/latest/">cloud-init</a> file
     * to bootstrap a VM on <a href=
     * "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-linux-using-cloud-init">Linux
     * VMs that support this format</a>. May be <code>null</code>.
     */
    private final String customData;

    /**
     * A (set of) custom script(s) to download and execute run when a VM has
     * booted. May be {@code null}.
     * <p/>
     * See
     * <a href="https://github.com/Azure/custom-script-extension-linux">custom
     * script extensions for linux</a> for details.
     */
    private final CustomScriptExtension customScript;

    /**
     * Creates {@link LinuxSettings}.
     *
     * @param rootUserName
     *            Name of the root Linux account to create on created VMs. May
     *            be <code>null</code>. Default: {@link #DEFAULT_ROOT_USER}.
     * @param publicSshKey
     *            An OpenSSH public key used to login to created VMs. For
     *            example, {@code ssh-rsa ABCD...123 user@host}. May be
     *            {@code null} (if so, a {@link #password} must be set).
     * @param password
     *            A password used to login to created VMs. May be {@code null}
     *            (if so, a {@link #publicSshKey} must be set).
     * @param customData
     *            Allows passing of base64-encoded custom data to the VM. This
     *            can, for example, be used to pass a <a href=
     *            "https://cloudinit.readthedocs.io/en/latest/">cloud-init</a>
     *            file to bootstrap a VM on <a href=
     *            "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-linux-using-cloud-init">Linux
     *            VMs that support this format</a>. May be <code>null</code>.
     * @param customScript
     *            A (set of) custom script(s) to download and execute run when a
     *            VM has booted. May be {@code null}.
     *            <p/>
     *            See <a href=
     *            "https://github.com/Azure/custom-script-extension-linux">custom
     *            script extensions for linux</a> for details.
     */
    public LinuxSettings(String rootUserName, String publicSshKey, String password, String customData,
            CustomScriptExtension customScript) {
        this.rootUserName = rootUserName;
        this.publicSshKey = publicSshKey;
        this.password = password;
        this.customData = customData;
        this.customScript = customScript;
    }

    /**
     * Name of the root Linux account to create on created VMs.
     *
     * @return the rootUserName
     */
    public String getRootUserName() {
        return Optional.ofNullable(this.rootUserName).orElse(DEFAULT_ROOT_USER);
    }

    /**
     * An OpenSSH public key used to login to created VMs. For example,
     * {@code ssh-rsa ABCD...123 user@host}. May be {@code null} (if so, a
     * {@link #password} must be set).
     *
     * @return the publicSshKey
     */
    public String getPublicSshKey() {
        return this.publicSshKey;
    }

    /**
     * A password used to login to created VMs. May be {@code null} (if so, a
     * {@link #publicSshKey} must be set).
     *
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Allows passing of base64-encoded custom data to the VM. This can, for
     * example, be used to pass a
     * <a href="https://cloudinit.readthedocs.io/en/latest/">cloud-init</a> file
     * to bootstrap a VM on <a href=
     * "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-linux-using-cloud-init">Linux
     * VMs that support this format</a>. May be <code>null</code>.
     * 
     * @return
     */
    public String getCustomData() {
        return this.customData;
    }

    /**
     * A (set of) custom script(s) to download and execute run when a VM has
     * booted. May be {@code null}.
     * <p/>
     * See
     * <a href= "https://github.com/Azure/custom-script-extension-linux">custom
     * script extensions for linux</a> for details.
     *
     * @return the customScript
     */
    public CustomScriptExtension getCustomScript() {
        return this.customScript;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getRootUserName(), this.publicSshKey, this.password, this.customData,
                this.customScript);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LinuxSettings) {
            LinuxSettings that = (LinuxSettings) obj;
            return Objects.equal(getRootUserName(), that.getRootUserName())
                    && Objects.equal(this.publicSshKey, that.publicSshKey)
                    && Objects.equal(this.password, that.password) //
                    && Objects.equal(this.customData, that.customData) //
                    && Objects.equal(this.customScript, that.customScript);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(getRootUserName().matches(ALLOWED_USERNAME_REGEXP), "linuxSettings: illegal username '%s'",
                getRootUserName());
        checkArgument(this.publicSshKey != null || this.password != null,
                "linuxSettings: neither publicSshKey nor password given");
        checkArgument(this.publicSshKey != null ^ this.password != null,
                "linuxSettings: either publicSshKey or password must be given, not both");

        try {
            if (this.customScript != null) {
                this.customScript.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("linuxSettings: " + e.getMessage(), e);
        }
    }
}
