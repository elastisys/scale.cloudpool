package com.elastisys.scale.cloudpool.azure.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.microsoft.azure.AzureEnvironment;

/**
 * Exercise the {@link AzureAuth} configuration block.
 */
public class TestAzureAuth {

    /** Sample clientId/appId. */
    private static final String CLIENT_ID = "12345678-abcd-efff-bcbc-90bbcfdeeecf";
    /** Sample domain/tenant. */
    private static final String DOMAIN = "87654321-abcd-efff-bcbc-90bbcfdeeecf";
    /** Sample secret/password. */
    private static final String SECRET = "12121212-aaaa-bbbb-cccc-000000000000";

    /**
     * clientId, domain and secret are mandatory.
     */
    @Test
    public void mandatoryOnly() {
        AzureAuth auth = new AzureAuth(CLIENT_ID, DOMAIN, SECRET);
        auth.validate();

        assertThat(auth.getClientId(), is(CLIENT_ID));
        assertThat(auth.getDomain(), is(DOMAIN));
        assertThat(auth.getSecret(), is(SECRET));

        // default values
        assertThat(auth.getEnvironment(), is(AzureAuth.DEFAULT_ENVIRONMENT));
    }

    /**
     * Specify explicit values for all parameters.
     */
    @Test
    public void complete() {
        AzureAuth auth = new AzureAuth(CLIENT_ID, DOMAIN, SECRET, "AZURE_CHINA");
        auth.validate();

        assertThat(auth.getClientId(), is(CLIENT_ID));
        assertThat(auth.getDomain(), is(DOMAIN));
        assertThat(auth.getSecret(), is(SECRET));
        assertThat(auth.getEnvironment(), is(AzureEnvironment.AZURE_CHINA));
    }

    /**
     * Test on a configuration that should pass validation.
     */
    @Test
    public void onlyMandatory() {
        AzureAuth auth = new AzureAuth(CLIENT_ID, DOMAIN, SECRET);
        auth.validate();

        assertThat(auth.getClientId(), is(CLIENT_ID));
        assertThat(auth.getDomain(), is(DOMAIN));
        assertThat(auth.getSecret(), is(SECRET));
        assertThat(auth.getEnvironment(), is(AzureEnvironment.AZURE));
    }

    /**
     * clientId is mandatory.
     */
    @Test
    public void withoutClientId() {
        try {
            new AzureAuth(null, DOMAIN, SECRET, "AZURE").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("clientId"));
        }
    }

    /**
     * domain is mandatory.
     */
    @Test
    public void withoutDomain() {
        try {
            new AzureAuth(CLIENT_ID, null, SECRET, "AZURE").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("domain"));
        }
    }

    /**
     * secret is mandatory.
     */
    @Test
    public void withoutSecret() {
        try {
            new AzureAuth(CLIENT_ID, DOMAIN, null, "AZURE").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("secret"));
        }
    }

    @Test
    public void withIllegalEnvironment() {
        try {
            new AzureAuth(CLIENT_ID, DOMAIN, SECRET, "AZURE_SWEDEN").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("environment"));
        }
    }

    /**
     * Only the following Azure environments should be supported: {@code AZURE},
     * {@code AZURE_CHINA}, {@code AZURE_US_GOVERNMENT}, {@code AZURE_GERMANY}.
     */
    @Test
    public void azureEnvironmentParsing() {
        assertThat(AzureAuth.parseEnvironment("AZURE"), is(AzureEnvironment.AZURE));
        assertThat(AzureAuth.parseEnvironment("AZURE_CHINA"), is(AzureEnvironment.AZURE_CHINA));
        assertThat(AzureAuth.parseEnvironment("AZURE_US_GOVERNMENT"), is(AzureEnvironment.AZURE_US_GOVERNMENT));
        assertThat(AzureAuth.parseEnvironment("AZURE_GERMANY"), is(AzureEnvironment.AZURE_GERMANY));

        try {
            AzureAuth.parseEnvironment("AZURE_BELARUS");
            fail("should not recognize AZURE_BELARUS");
        } catch (IllegalArgumentException e) {
            // expected
            System.out.println(e);
        }
    }
}
