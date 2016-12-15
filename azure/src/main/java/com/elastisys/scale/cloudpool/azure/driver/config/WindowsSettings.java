package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * Settings specific to launching Windows VMs.
 *
 * @see AzureScaleOutConfig
 */
public class WindowsSettings {
    /**
     * A Windows user name may not contain any of the following characters: / \
     * [ ] : ; | = , + * ? < >
     */
    private static final String ALLOWED_USERNAME_REGEXP = "^[^/\\\\\\[\\]:;|=,+\\*\\?<>]+$";

    private static String DEFAULT_ADMIN_USER = "windowsadmin";

    /**
     * The administrator user name for the Windows VM. May be <code>null</code>.
     * Default: {@link #DEFAULT_ADMIN_USER}.
     */
    private final String adminUserName;

    /**
     * A password used to login to created VMs. May be {@code null} (if so, a
     * {@link #publicSshKey} must be set).
     */
    private final String password;

    /**
     * A (set of) custom script(s) to run when a VM is booted. May be
     * {@code null}.
     * <p/>
     * See <a href=
     * "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-windows-extensions-customscript">Windows
     * VM Custom Script extensions </a> for details.
     */
    private final CustomScriptExtension customScript;

    /**
     * Creates {@link LinuxSettings}.
     *
     * @param adminUserName
     *            The administrator user name for the Windows VM. May be
     *            <code>null</code>. Default: {@link #DEFAULT_ADMIN_USER}.
     * @param password
     *            A password used to login to created VMs.
     * @param customScript
     *            A (set of) custom script(s) to run when a VM is booted. May be
     *            {@code null}.
     *            <p/>
     *            See <a href=
     *            "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-windows-extensions-customscript">Windows
     *            VM Custom Script extensions </a> for details.
     */
    public WindowsSettings(String adminUserName, String password, CustomScriptExtension customScript) {
        this.adminUserName = adminUserName;
        this.password = password;
        this.customScript = customScript;
    }

    /**
     * The administrator user name for the Windows VM.
     *
     * @return
     */
    public String getAdminUserName() {
        return Optional.ofNullable(this.adminUserName).orElse(DEFAULT_ADMIN_USER);
    }

    /**
     * A password used to login to created VMs.
     *
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * A (set of) custom script(s) to run when a VM is booted. May be
     * {@code null}.
     * <p/>
     * See <a href=
     * "https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-windows-extensions-customscript">Windows
     * VM Custom Script extensions </a> for details.
     *
     * @return the customScript
     */
    public CustomScriptExtension getCustomScript() {
        return this.customScript;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getAdminUserName(), this.password, this.customScript);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WindowsSettings) {
            WindowsSettings that = (WindowsSettings) obj;
            return Objects.equal(getAdminUserName(), that.getAdminUserName())
                    && Objects.equal(this.password, that.password)
                    && Objects.equal(this.customScript, that.customScript);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(getAdminUserName().matches(ALLOWED_USERNAME_REGEXP),
                "windowsSettings: illegal admin username '%s'", getAdminUserName());
        checkArgument(this.password != null, "windowsSettings: no password given");

        try {
            if (this.customScript != null) {
                this.customScript.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("windowsSettings: " + e.getMessage(), e);
        }
    }
}
