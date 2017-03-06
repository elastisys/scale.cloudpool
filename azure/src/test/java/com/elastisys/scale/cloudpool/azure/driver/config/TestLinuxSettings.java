package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidCustomScript;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validCustomScript;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.elastisys.scale.commons.util.base64.Base64Utils;

/**
 * Exercises VM {@link LinuxSettings}.
 */
public class TestLinuxSettings {

    /** Sample root username. */
    private static final String ROOT_USER = "admin";
    /** Sample public SSH key. */
    private static final String PUBLIC_SSH_KEY = "ssh-rsa XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX foo@bar";
    /** Sample root password. */
    private static final String ROOT_PASSWORD = "secret";

    /** Sample custom data cloud-init config. */
    private static final String CLOUD_INIT = Base64Utils.toBase64(Arrays.asList(//
            "#cloud-config", //
            "write_files:", //
            "  - path: /home/ubuntu/important.sh", //
            "    permissions: 0700", //
            "    owner: ubuntu:ubuntu", //
            "    content: |", //
            "      #!/bin/bash", //
            "      echo \"important script being executed ...\""));

    /**
     * Should be okay to only specify SSH login key.
     */
    @Test
    public void onlySpecifySshKey() {
        String nullRootUserName = null;
        String nullPassword = null;
        String nullCustomData = null;
        CustomScriptExtension nullCustomScript = null;
        LinuxSettings settings = new LinuxSettings(nullRootUserName, PUBLIC_SSH_KEY, nullPassword, nullCustomData,
                nullCustomScript);
        settings.validate();

        assertThat(settings.getPublicSshKey(), is(PUBLIC_SSH_KEY));
        // verify defaults
        assertThat(settings.getRootUserName(), is(LinuxSettings.DEFAULT_ROOT_USER));

        assertThat(settings.getPassword(), is(nullValue()));
        assertThat(settings.getCustomData(), is(nullValue()));
        assertThat(settings.getCustomScript(), is(nullValue()));
    }

    /**
     * Should be okay to only specify root password.
     */
    @Test
    public void onlySpecifyRootPassword() {
        String nullRootUserName = null;
        String nullSshKey = null;
        String nullCustomData = null;
        CustomScriptExtension nullCustomScript = null;
        LinuxSettings settings = new LinuxSettings(nullRootUserName, nullSshKey, ROOT_PASSWORD, nullCustomData,
                nullCustomScript);
        settings.validate();

        assertThat(settings.getPassword(), is(ROOT_PASSWORD));
        // verify defaults
        assertThat(settings.getRootUserName(), is(LinuxSettings.DEFAULT_ROOT_USER));

        assertThat(settings.getPublicSshKey(), is(nullValue()));
        assertThat(settings.getCustomData(), is(nullValue()));
        assertThat(settings.getCustomScript(), is(nullValue()));
    }

    @Test
    public void specifyRootUsername() {
        LinuxSettings settings = new LinuxSettings("myrootuser", null, ROOT_PASSWORD, null, null);
        settings.validate();

        assertThat(settings.getRootUserName(), is("myrootuser"));
        assertThat(settings.getPassword(), is(ROOT_PASSWORD));
    }

    @Test
    public void specifyCustomData() {
        String nullRootUser = null;
        String nullPassword = null;
        CustomScriptExtension nullCustomScript = null;
        LinuxSettings settings = new LinuxSettings(nullRootUser, PUBLIC_SSH_KEY, nullPassword, CLOUD_INIT,
                nullCustomScript);
        settings.validate();

        assertThat(settings.getCustomData(), is(CLOUD_INIT));
    }

    @Test
    public void specifyCustomScript() {
        String nullPassword = null;
        String nullCustomData = null;
        LinuxSettings settings = new LinuxSettings("myrootuser", nullPassword, ROOT_PASSWORD, nullCustomData,
                validCustomScript());
        settings.validate();

        assertThat(settings.getRootUserName(), is("myrootuser"));
        assertThat(settings.getPassword(), is(ROOT_PASSWORD));
        assertThat(settings.getCustomScript(), is(validCustomScript()));
    }

    /**
     * only one of root password and public SSH key may be specified
     */
    @Test
    public void withBothRootPasswordAndSshKey() {
        String nullRootUser = null;
        String nullCustomData = null;
        CustomScriptExtension nullCustomScript = null;

        try {
            new LinuxSettings(nullRootUser, PUBLIC_SSH_KEY, ROOT_PASSWORD, nullCustomData, nullCustomScript).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("either publicSshKey or password must be given, not both"));
        }
    }

    /**
     * Validation should be recursively applied to fields to capture deep
     * validation errors.
     */
    @Test
    public void onIllegalCustomScript() {
        String nullRootUser = null;
        String nullPassword = null;
        String nullCustomData = null;
        try {
            new LinuxSettings(nullRootUser, PUBLIC_SSH_KEY, nullPassword, nullCustomData, invalidCustomScript())
                    .validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("customScript"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithLeadingWhitespace() {
        String nullPassword = null;
        String nullCustomData = null;
        new LinuxSettings(" illegal", PUBLIC_SSH_KEY, nullPassword, nullCustomData, validCustomScript()).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithLeadingDigit() {
        String nullPassword = null;
        String nullCustomData = null;

        new LinuxSettings("4oot", PUBLIC_SSH_KEY, nullPassword, nullCustomData, validCustomScript()).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithColon() {
        String nullPassword = null;
        String nullCustomData = null;

        new LinuxSettings("root:user", PUBLIC_SSH_KEY, nullPassword, nullCustomData, validCustomScript()).validate();
    }

}
