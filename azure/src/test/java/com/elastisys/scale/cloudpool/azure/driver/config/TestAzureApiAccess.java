package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.elastisys.scale.cloudpool.azure.driver.config.TestUtils.validAuth;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.types.TimeInterval;

import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Exercises the {@link AzureApiAccess} configuration block.
 */
public class TestAzureApiAccess {

    private static final String SUBECRIPTION_ID = "88888888-7777-6666-5555-444444444444";

    /**
     * Only subscriptionId and auth are required.
     */
    @Test
    public void defaults() {
        AzureApiAccess apiAccess = new AzureApiAccess(SUBECRIPTION_ID, validAuth());
        apiAccess.validate();

        assertThat(apiAccess.getSubscriptionId(), is(SUBECRIPTION_ID));
        assertThat(apiAccess.getAuth(), is(validAuth()));

        // check defaults
        assertThat(apiAccess.getConnectionTimeout(), is(AzureApiAccess.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(apiAccess.getReadTimeout(), is(AzureApiAccess.DEFAULT_READ_TIMEOUT));
        assertThat(apiAccess.getAzureSdkLogLevel(), is(AzureApiAccess.DEFAULT_AZURE_SDK_LOG_LEVEL));
    }

    /**
     * It should be possible to pass explicit values for all parameters.
     */
    @Test
    public void complete() {
        TimeInterval connectionTimeout = new TimeInterval(5L, TimeUnit.SECONDS);
        TimeInterval readTimeout = new TimeInterval(15L, TimeUnit.SECONDS);
        Level azureSdkLogLevel = Level.BODY;
        AzureApiAccess apiAccess = new AzureApiAccess(SUBECRIPTION_ID, validAuth(), connectionTimeout, readTimeout,
                azureSdkLogLevel);
        apiAccess.validate();

        assertThat(apiAccess.getSubscriptionId(), is(SUBECRIPTION_ID));
        assertThat(apiAccess.getAuth(), is(validAuth()));
        assertThat(apiAccess.getConnectionTimeout(), is(connectionTimeout));
        assertThat(apiAccess.getReadTimeout(), is(readTimeout));
        assertThat(apiAccess.getAzureSdkLogLevel(), is(azureSdkLogLevel));
    }

    /**
     * subscriptionId is manadatory
     */
    @Test
    public void withoutSubscriptionId() {
        try {
            new AzureApiAccess(null, validAuth()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("subscriptionId"));
        }
    }

    /**
     * auth is manadatory
     */
    @Test
    public void withoutAuth() {
        try {
            new AzureApiAccess(SUBECRIPTION_ID, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth"));
        }
    }

    /**
     * validation should recursively validate sub-configs
     */
    @Test
    public void withInvalidAuth() {
        try {
            new AzureApiAccess(SUBECRIPTION_ID, TestUtils.invalidAuth()).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth"));
        }
    }

    @Test
    public void withInvalidConnectionTimeout() {
        try {
            new AzureApiAccess(SUBECRIPTION_ID, validAuth(), TestUtils.illegalTimeInterval(),
                    AzureApiAccess.DEFAULT_READ_TIMEOUT, AzureApiAccess.DEFAULT_AZURE_SDK_LOG_LEVEL).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("connectionTimeout"));
        }
    }

    @Test
    public void withInvalidReadTimeout() {
        try {
            new AzureApiAccess(SUBECRIPTION_ID, validAuth(), AzureApiAccess.DEFAULT_CONNECTION_TIMEOUT,
                    TestUtils.illegalTimeInterval(), AzureApiAccess.DEFAULT_AZURE_SDK_LOG_LEVEL).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("readTimeout"));
        }
    }

}
