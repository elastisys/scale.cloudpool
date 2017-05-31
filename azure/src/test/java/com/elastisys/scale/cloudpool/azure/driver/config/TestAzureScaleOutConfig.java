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
 * Exercise {@link ProvisioningTemplate}.
 *
 */
public class TestAzureScaleOutConfig {

    /** Sample VM size. */
    private static final String VM_SIZE = "Standard_DS1_v2";
    /** Sample VM image. */
    private static final String VM_IMAGE = "Canonical:UbuntuServer:16.04.0-LTS:latest";
    private static final String WINDOWS_IMAGE = "MicrosoftWindowsServer:WindowsServer:2008-R2-SP1:2.0.20161109";
    /** Sample VM name prefix. */
    private static final String VM_NAME_PREFIX = "apache";
    /** Sample storage account name. */
    private static final String STORAGE_ACCOUNT = "apache-disks";
    /** Sample availability set. */
    private static final String AVAILABILITY_SET = "availability-set";

    /**
     * Only storage account, network and one of linuxSettings and
     * windowsSettings are required inputs.
     */
    @Test
    public void defaults() {
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, VM_IMAGE, null, validLinuxSettings(), null,
                STORAGE_ACCOUNT, validNetworkSettings(), null, null);
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(VM_IMAGE));
        assertThat(conf.getVmNamePrefix().isPresent(), is(false));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getAvailabilitySet().isPresent(), is(false));
        assertThat(conf.getStorageAccountName(), is(STORAGE_ACCOUNT));
        assertThat(conf.getTags(), is(Collections.emptyMap()));
        assertThat(conf.getWindowsSettings().isPresent(), is(false));
    }

    /**
     * Make sure that scale-out config for a linux VM can be created.
     */
    @Test
    public void linuxVm() {
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, VM_IMAGE, VM_NAME_PREFIX, validLinuxSettings(),
                null, STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags());
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(VM_IMAGE));
        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getStorageAccountName(), is(STORAGE_ACCOUNT));
        assertThat(conf.getAvailabilitySet().get(), is(AVAILABILITY_SET));
        assertThat(conf.getTags(), is(validTags()));

        // no windows VM settings
        assertThat(conf.getWindowsSettings().isPresent(), is(false));
    }

    /**
     * Make sure that scale-out config for a windows VM can be created.
     */
    @Test
    public void windowsVm() {
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, null,
                validWindowsSettings(), STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags());
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(WINDOWS_IMAGE));

        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getWindowsSettings().get(), is(validWindowsSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getStorageAccountName(), is(STORAGE_ACCOUNT));
        assertThat(conf.getAvailabilitySet().get(), is(AVAILABILITY_SET));
        assertThat(conf.getTags(), is(validTags()));

        // no linux VM settings
        assertThat(conf.getLinuxSettings().isPresent(), is(false));
    }

    /**
     * Must specify VM size.
     */
    @Test
    public void missingVmSize() {
        try {
            LinuxSettings linuxSettings = validLinuxSettings();
            WindowsSettings windowsSettings = null;
            new ProvisioningTemplate(null, VM_IMAGE, VM_NAME_PREFIX, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                    validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("vmSize"));
        }
    }

    /**
     * Must specify VM image.
     */
    @Test
    public void missingVmImage() {
        try {
            LinuxSettings linuxSettings = validLinuxSettings();
            WindowsSettings windowsSettings = null;
            new ProvisioningTemplate(VM_SIZE, null, VM_NAME_PREFIX, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                    validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("vmImage"));
        }
    }

    /**
     * Must specify either Linux or Windows settings for VM.
     */
    @Test
    public void missingLinuxAndWindowsSettings() {
        try {
            LinuxSettings linuxSettings = null;
            WindowsSettings windowsSettings = null;
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, linuxSettings, windowsSettings,
                    STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, validLinuxSettings(),
                    validWindowsSettings(), STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags())
                            .validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, invalidLinuxSettings(), null,
                    STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, null, invalidWindowsSettings(),
                    STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("windowsSettings"));
        }
    }

    /**
     * Network settings are mandatory.
     */
    @Test
    public void missingNetworkSettings() {
        try {
            LinuxSettings linuxSettings = validLinuxSettings();
            WindowsSettings windowsSettings = null;
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, linuxSettings, windowsSettings,
                    STORAGE_ACCOUNT, null, AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("network"));
        }
    }

    /**
     * Validation should be recursively applied to fields to catch deep
     * validation errors.
     */
    @Test
    public void onIllegalNetworkSettings() {
        try {
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, null, validWindowsSettings(),
                    STORAGE_ACCOUNT, invalidNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("network"));
        }
    }

    /**
     * Storage account is mandatory.
     */
    @Test
    public void missingStorageAccount() {
        try {
            LinuxSettings linuxSettings = validLinuxSettings();
            WindowsSettings windowsSettings = null;
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, linuxSettings, windowsSettings, null,
                    validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("storageAccountName"));
        }
    }

    /**
     * The following characters are forbidden in tag keys: <>*%&:\\?/+.
     */
    @Test
    public void onIllegalTags() {
        try {
            Map<String, String> invalidTags = ImmutableMap.of("illegal/key", "value");
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, VM_NAME_PREFIX, null, validWindowsSettings(),
                    STORAGE_ACCOUNT, validNetworkSettings(), AVAILABILITY_SET, invalidTags).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("illegal tag key"));
        }
    }

}
