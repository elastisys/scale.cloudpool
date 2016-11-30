package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidLinuxSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidNetworkSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidWindowsSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validLinuxSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validNetworkSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validTags;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validWindowsSettings;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Exercise {@link ScaleOutExtConfig}.
 *
 */
public class TestScaleOutExtConfig {

    /** Sample VM name prefix. */
    private static final String VM_NAME_PREFIX = "apache";
    /** Sample storage account name. */
    private static final String STORAGE_ACCOUNT = "apache-disks";

    /**
     * Only network and one of linuxSettings and windowsSettings is required
     * input.
     */
    @Test
    public void defaults() {
        ScaleOutExtConfig conf = new ScaleOutExtConfig(null, validLinuxSettings(), null, null, validNetworkSettings(),
                null);
        conf.validate();

        assertThat(conf.getVmNamePrefix().isPresent(), is(false));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getStorageAccountName().isPresent(), is(false));
        assertThat(conf.getTags(), is(Collections.emptyMap()));
        assertThat(conf.getWindowsSettings().isPresent(), is(false));
    }

    /**
     * Make sure that scale-out config for a linux VM can be created.
     */
    @Test
    public void linuxVm() {
        ScaleOutExtConfig conf = new ScaleOutExtConfig(VM_NAME_PREFIX, validLinuxSettings(), null, STORAGE_ACCOUNT,
                validNetworkSettings(), validTags());
        conf.validate();

        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getStorageAccountName().get(), is(STORAGE_ACCOUNT));
        assertThat(conf.getTags(), is(validTags()));

        // no windows VM settings
        assertThat(conf.getWindowsSettings().isPresent(), is(false));
    }

    /**
     * Make sure that scale-out config for a windows VM can be created.
     */
    @Test
    public void windowsVm() {
        ScaleOutExtConfig conf = new ScaleOutExtConfig(VM_NAME_PREFIX, null, validWindowsSettings(), STORAGE_ACCOUNT,
                validNetworkSettings(), validTags());
        conf.validate();

        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getWindowsSettings().get(), is(validWindowsSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getStorageAccountName().get(), is(STORAGE_ACCOUNT));
        assertThat(conf.getTags(), is(validTags()));

        // no linux VM settings
        assertThat(conf.getLinuxSettings().isPresent(), is(false));
    }

    /**
     * Must specify either Linux or Windows settings for VM.
     */
    @Test
    public void missingLinuxAndWindowsSettings() {
        try {
            LinuxSettings linuxSettings = null;
            WindowsSettings windowsSettings = null;
            new ScaleOutExtConfig(VM_NAME_PREFIX, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                    validNetworkSettings(), validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("neither linuxSettings nor windowsSettings given"));
        }
    }

    /**
     * Either a Linux or a Windows VM can be launched, not both.
     */
    @Test
    public void onBothLinuxAndWindowsSettings() {
        try {
            new ScaleOutExtConfig(VM_NAME_PREFIX, validLinuxSettings(), validWindowsSettings(), STORAGE_ACCOUNT,
                    validNetworkSettings(), validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("may only specify one of linuxSettings and windowsSettings, not both"));
        }
    }

    /**
     * Validation should be recursively applied to fields to catch deep
     * validation errors.
     */
    @Test
    public void onIllegalLinuxSettings() {
        try {
            new ScaleOutExtConfig(VM_NAME_PREFIX, invalidLinuxSettings(), null, STORAGE_ACCOUNT, validNetworkSettings(),
                    validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("linuxSettings"));
        }
    }

    /**
     * Validation should be recursively applied to fields to catch deep
     * validation errors.
     */
    @Test
    public void onIllegalWindowsSettings() {
        try {
            new ScaleOutExtConfig(VM_NAME_PREFIX, null, invalidWindowsSettings(), STORAGE_ACCOUNT,
                    validNetworkSettings(), validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("windowsSettings"));
        }
    }

    /**
     * Validation should be recursively applied to fields to catch deep
     * validation errors.
     */
    @Test
    public void onIllegalNetworkSettings() {
        try {
            new ScaleOutExtConfig(VM_NAME_PREFIX, null, validWindowsSettings(), STORAGE_ACCOUNT,
                    invalidNetworkSettings(), validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("network"));
        }
    }

    /**
     * The following characters are forbidden in tag keys: <>*%&:\\?/+.
     */
    @Test
    public void onIllegalTags() {
        try {
            Map<String, String> invalidTags = ImmutableMap.of("illegal/key", "value");
            new ScaleOutExtConfig(VM_NAME_PREFIX, null, validWindowsSettings(), STORAGE_ACCOUNT, validNetworkSettings(),
                    invalidTags).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("illegal tag key"));
        }
    }

}
