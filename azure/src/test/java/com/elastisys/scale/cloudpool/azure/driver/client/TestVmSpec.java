package com.elastisys.scale.cloudpool.azure.driver.client;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidLinuxSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidNetworkSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidWindowsSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validLinuxSettings;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validWindowsSettings;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.elastisys.scale.cloudpool.azure.driver.Constants;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.TestUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.WindowsSettings;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

/**
 * Exercise {@link VmSpec}
 *
 */
public class TestVmSpec {

    private static final NetworkSettings VM_NETWORK = TestUtils.validNetworkSettings();
    private static final String STORAGE_ACCOUNT = "vm-storage-account";
    private static final String VM_NAME = "vm-0";
    private static final String VM_IMAGE = "Canonical:UbuntuServer:16.04.0-LTS:latest";
    private static final String VM_SIZE = "Standard_DS1_v2";
    private static final Map<String, String> VM_TAGS = ImmutableMap.of(//
            Constants.CLOUD_POOL_TAG, "scaling-pool", //
            "tier", "web");

    /**
     * Specify all fields.
     */
    @Test
    public void completeSpec() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, VM_TAGS);
        vmSpec.validate();

        assertThat(vmSpec.getVmSize(), is(VM_SIZE));
        assertThat(vmSpec.getVmImage(), is(VM_IMAGE));
        assertThat(vmSpec.getVmName(), is(VM_NAME));
        assertThat(vmSpec.getLinuxSettings(), is(linuxSettings));
        assertThat(vmSpec.getWindowsSettings().isPresent(), is(false));
        assertThat(vmSpec.getStorageAccountName(), is(STORAGE_ACCOUNT));
        assertThat(vmSpec.getNetwork(), is(VM_NETWORK));
        assertThat(vmSpec.getTags(), is(VM_TAGS));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVmSize() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(null, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT, VM_NETWORK,
                VM_TAGS);
        vmSpec.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVmImage() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, null, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT, VM_NETWORK,
                VM_TAGS);
        vmSpec.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVmName() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, null, linuxSettings, windowsSettings, STORAGE_ACCOUNT, VM_NETWORK,
                VM_TAGS);
        vmSpec.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingLinuxAndWindowsSettings() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.empty();

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void bothLinuxAndWindowsSettings() {
        Optional<WindowsSettings> windowsSettings = Optional.of(validWindowsSettings());
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalLinuxSettings() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(invalidLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalWindowsSettings() {
        Optional<WindowsSettings> windowsSettings = Optional.of(invalidWindowsSettings());
        Optional<LinuxSettings> linuxSettings = Optional.empty();

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void missingStorageAccount() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, null, VM_NETWORK,
                VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void missingNetwork() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT, null,
                VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalNetwork() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                invalidNetworkSettings(), VM_TAGS);
        vmSpec.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void missingTags() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, null);
        vmSpec.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalTags() {
        Optional<WindowsSettings> windowsSettings = Optional.empty();
        Optional<LinuxSettings> linuxSettings = Optional.of(validLinuxSettings());

        Map<String, String> invalidTags = ImmutableMap.of("a/key", "value");
        VmSpec vmSpec = new VmSpec(VM_SIZE, VM_IMAGE, VM_NAME, linuxSettings, windowsSettings, STORAGE_ACCOUNT,
                VM_NETWORK, invalidTags);
        vmSpec.validate();

    }

}
