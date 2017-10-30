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

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.elastisys.scale.cloudpool.azure.driver.client.VmImage;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.compute.StorageAccountTypes;

/**
 * Exercise {@link ProvisioningTemplate}.
 */
public class TestProvisioningTemplate {

    /** Sample VM size. */
    private static final String VM_SIZE = "Standard_DS1_v2";
    /** Sample VM image. */
    private static final String VM_IMAGE = "Canonical:UbuntuServer:16.04.0-LTS:latest";
    // TODO VM_IMAGE_ID
    private static final StorageAccountTypes STANDARD_OS_DISK = StorageAccountTypes.STANDARD_LRS;
    private static final StorageAccountTypes PREMIUM_OS_DISK = StorageAccountTypes.PREMIUM_LRS;

    private static final String WINDOWS_IMAGE = "MicrosoftWindowsServer:WindowsServer:2008-R2-SP1:2.0.20161109";
    /** Sample VM name prefix. */
    private static final String VM_NAME_PREFIX = "apache";
    /** Sample availability set. */
    private static final String AVAILABILITY_SET = "availability-set";

    /**
     * Only storage account, network and one of linuxSettings and
     * windowsSettings are required inputs.
     */
    @Test
    public void defaults() {
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, VM_IMAGE, null, null, validLinuxSettings(), null,
                validNetworkSettings(), null, null);
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(new VmImage(VM_IMAGE)));
        assertThat(conf.getOsDiskType().toString(), is(ProvisioningTemplate.DEFAULT_OS_DISK_TYPE));
        assertThat(conf.getVmNamePrefix().isPresent(), is(false));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
        assertThat(conf.getAvailabilitySet().isPresent(), is(false));
        assertThat(conf.getTags(), is(Collections.emptyMap()));
        assertThat(conf.getWindowsSettings().isPresent(), is(false));
    }

    /**
     * Make sure that scale-out config for a linux VM can be created.
     */
    @Test
    public void linuxVm() {
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, VM_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX,
                validLinuxSettings(), null, validNetworkSettings(), AVAILABILITY_SET, validTags());
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(new VmImage(VM_IMAGE)));
        assertThat(conf.getOsDiskType(), is(StorageAccountTypes.PREMIUM_LRS));
        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getLinuxSettings().get(), is(validLinuxSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
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
        ProvisioningTemplate conf = new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, STANDARD_OS_DISK, VM_NAME_PREFIX,
                null, validWindowsSettings(), validNetworkSettings(), AVAILABILITY_SET, validTags());
        conf.validate();

        assertThat(conf.getVmSize(), is(VM_SIZE));
        assertThat(conf.getVmImage(), is(new VmImage(WINDOWS_IMAGE)));
        assertThat(conf.getOsDiskType(), is(StorageAccountTypes.STANDARD_LRS));
        assertThat(conf.getVmNamePrefix().get(), is(VM_NAME_PREFIX));
        assertThat(conf.getWindowsSettings().get(), is(validWindowsSettings()));
        assertThat(conf.getNetwork(), is(validNetworkSettings()));
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
            new ProvisioningTemplate(null, VM_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, linuxSettings, windowsSettings,
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
            new ProvisioningTemplate(VM_SIZE, null, PREMIUM_OS_DISK, VM_NAME_PREFIX, linuxSettings, windowsSettings,
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, linuxSettings,
                    windowsSettings, validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, validLinuxSettings(),
                    validWindowsSettings(), validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, invalidLinuxSettings(),
                    null, validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, null,
                    invalidWindowsSettings(), validNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, linuxSettings,
                    windowsSettings, null, AVAILABILITY_SET, validTags()).validate();
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
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, PREMIUM_OS_DISK, VM_NAME_PREFIX, null,
                    validWindowsSettings(), invalidNetworkSettings(), AVAILABILITY_SET, validTags()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("network"));
        }
    }

    /**
     * Validation should fail if the provided OS disk type is not recognized.
     */
    @Test
    public void illegalOsDiskType() throws Exception {
        try {
            // simulate an erroneous config coming over JSON
            StringWriter json = new StringWriter();
            json.append("{\n");
            json.append("  \"vmSize\": \"Standard_DS1_v2\",\n");
            json.append("  \"vmImage\": \"Canonical:UbuntuServer:16.04.0-LTS:latest\",\n");
            json.append("  \"osDiskType\": \"unrecognized-data-type\",\n");
            json.append("  \"linuxSettings\": {\n");
            json.append("    \"rootUserName\": \"ubuntu\",\n");
            json.append(
                    "    \"publicSshKey\": \"ssh-rsa XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX foo@bar\"\n");
            json.append("  },\n");
            json.append("  \"network\": {\n");
            json.append("    \"virtualNetwork\": \"testnet\",\n");
            json.append("    \"subnetName\": \"default\"\n");
            json.append("  }\n");
            json.append("}");
            ProvisioningTemplate template = JsonUtils.toObject(JsonUtils.parseJsonString(json.toString()),
                    ProvisioningTemplate.class);
            template.validate();

            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("osDiskType"));
        }
    }

    /**
     * The following characters are forbidden in tag keys: <>*%&:\\?/+.
     */
    @Test
    public void onIllegalTags() {
        try {
            Map<String, String> invalidTags = ImmutableMap.of("illegal/key", "value");
            new ProvisioningTemplate(VM_SIZE, WINDOWS_IMAGE, STANDARD_OS_DISK, VM_NAME_PREFIX, null,
                    validWindowsSettings(), validNetworkSettings(), AVAILABILITY_SET, invalidTags).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("illegal tag key"));
        }
    }

}
