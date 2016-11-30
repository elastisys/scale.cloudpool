package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.invalidApiAccess;
import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validApiAccess;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link AzurePoolDriverConfig}.
 */
public class TestAzurePoolDriverConfig {

    /** Sample resource group name. */
    private static final String resourceGroup = "testpool";
    /** Sample Azure region. */
    private static final String region = "northeurope";

    /**
     * Appropriate configuration values should pass validation and set values
     * should be accesible via getters.
     */
    @Test
    public void onValidConfig() {
        AzurePoolDriverConfig driverConfig = new AzurePoolDriverConfig(validApiAccess(), resourceGroup, region);
        driverConfig.validate();

        assertThat(driverConfig.getApiAccess(), is(validApiAccess()));
        assertThat(driverConfig.getResourceGroup(), is(resourceGroup));
        assertThat(driverConfig.getRegion(), is(region));
    }

    /**
     * apiAccess is mandatory.
     */
    @Test
    public void onMissingApiAccess() {
        try {
            new AzurePoolDriverConfig(null, resourceGroup, region).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("apiAccess"));
        }
    }

    /**
     * resourceGroup is mandatory.
     */
    @Test
    public void onMissingResourceGroup() {
        try {
            new AzurePoolDriverConfig(validApiAccess(), null, region).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("resourceGroup"));
        }
    }

    /**
     * region is mandatory.
     */
    @Test
    public void onMissingRegion() {
        try {
            new AzurePoolDriverConfig(validApiAccess(), resourceGroup, null).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("region"));
        }
    }

    /**
     * Validation should be applied recusively to fields, discovering a broken
     * {@link AzureApiAccess} field.
     */
    @Test
    public void onInvalidApiAccess() {
        try {
            new AzurePoolDriverConfig(invalidApiAccess(), resourceGroup, region).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("apiAccess"));
        }
    }

}
