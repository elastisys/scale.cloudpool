package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidCustomScript;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validCustomScript;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

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

    /**
     * Should be okay to only specify SSH login key.
     */
    @Test
    public void onlySpecifySshKey() {
        String rootUserName = null;
        CustomScriptExtension customScript = null;
        LinuxSettings settings = new LinuxSettings(rootUserName, PUBLIC_SSH_KEY, null, customScript);
        settings.validate();

        assertThat(settings.getPublicSshKey(), is(PUBLIC_SSH_KEY));
        // verify defaults
        assertThat(settings.getRootUserName(), is(LinuxSettings.DEFAULT_ROOT_USER));

        assertThat(settings.getPassword(), is(nullValue()));
        assertThat(settings.getCustomScript(), is(nullValue()));
    }

    /**
     * Should be okay to only specify root password.
     */
    @Test
    public void onlySpecifyRootPassword() {
        String rootUserName = null;
        CustomScriptExtension customScript = null;
        LinuxSettings settings = new LinuxSettings(rootUserName, null, ROOT_PASSWORD, customScript);
        settings.validate();

        assertThat(settings.getPassword(), is(ROOT_PASSWORD));
        // verify defaults
        assertThat(settings.getRootUserName(), is(LinuxSettings.DEFAULT_ROOT_USER));

        assertThat(settings.getPublicSshKey(), is(nullValue()));
        assertThat(settings.getCustomScript(), is(nullValue()));
    }

    @Test
    public void specifyRootUsername() {
        LinuxSettings settings = new LinuxSettings("myrootuser", null, ROOT_PASSWORD, null);
        settings.validate();

        assertThat(settings.getRootUserName(), is("myrootuser"));
        assertThat(settings.getPassword(), is(ROOT_PASSWORD));
    }

    @Test
    public void specifyCustomScript() {
        LinuxSettings settings = new LinuxSettings("myrootuser", null, ROOT_PASSWORD, validCustomScript());
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
        try {
            new LinuxSettings(null, PUBLIC_SSH_KEY, ROOT_PASSWORD, null).validate();
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
        try {
            new LinuxSettings(null, PUBLIC_SSH_KEY, null, invalidCustomScript()).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("customScript"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithLeadingWhitespace() {
        new LinuxSettings(" illegal", PUBLIC_SSH_KEY, null, validCustomScript()).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithLeadingDigit() {
        new LinuxSettings("4oot", PUBLIC_SSH_KEY, null, validCustomScript()).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onRootUsernameWithColon() {
        new LinuxSettings("root:user", PUBLIC_SSH_KEY, null, validCustomScript()).validate();
    }

}
