package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.google.gson.JsonObject;

/**
 * Exercise {@link BaseCloudPoolConfig}.
 */
public class TestBaseCloudPoolConfig {

    /**
     * Make sure all fields can be assigned explicit values.
     */
    @Test
    public void basicSanity() {
        BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), provisioningTemplate(),
                scaleInConfig(), alertSettings(), poolFetch(), poolUpdate());
        config.validate();
        assertThat(config.getName(), is(name()));
        assertThat(config.getCloudApiSettings(), is(cloudApiSettings()));
        assertThat(config.getProvisioningTemplate(), is(provisioningTemplate()));
        assertThat(config.getScaleInConfig(), is(scaleInConfig()));
        assertThat(config.getAlerts(), is(alertSettings()));
        assertThat(config.getPoolFetch(), is(poolFetch()));
        assertThat(config.getPoolUpdate(), is(poolUpdate()));
    }

    /**
     * It is okay to not specify scaleInConfig.
     */
    @Test
    public void missingScaleInConfig() {
        BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), provisioningTemplate(), null,
                alertSettings(), poolFetch(), poolUpdate());
        config.validate();

        assertThat(config.getScaleInConfig(), is(BaseCloudPoolConfig.DEFAULT_SCALE_IN_CONFIG));
    }

    /**
     * It is okay to not specify alerts.
     */
    @Test
    public void missingAlertSettings() {
        BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), provisioningTemplate(),
                scaleInConfig(), null, poolFetch(), poolUpdate());
        config.validate();
        assertThat(config.getAlerts(), is(nullValue()));
    }

    /**
     * It is okay to not specify poolFetch.
     */
    @Test
    public void missingPoolFetch() {
        BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), provisioningTemplate(),
                scaleInConfig(), alertSettings(), null, poolUpdate());
        config.validate();
        assertThat(config.getPoolFetch(), is(BaseCloudPoolConfig.DEFAULT_POOL_FETCH_CONFIG));
    }

    /**
     * It is okay to not specify poolUpdate.
     */
    @Test
    public void missingPoolUpdate() {
        BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), provisioningTemplate(),
                scaleInConfig(), alertSettings(), poolFetch(), null);
        config.validate();
        assertThat(config.getPoolUpdate(), is(BaseCloudPoolConfig.DEFAULT_POOL_UPDATE_CONFIG));
    }

    /**
     * name of pool is required.
     */
    @Test
    public void missingName() {
        try {
            BaseCloudPoolConfig config = new BaseCloudPoolConfig(null, cloudApiSettings(), provisioningTemplate(),
                    scaleInConfig(), alertSettings(), poolFetch(), poolUpdate());
            config.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }
    }

    /**
     * cloudApiSettings is required.
     */
    @Test
    public void missingCloudApiSettings() {
        try {
            BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), null, provisioningTemplate(), scaleInConfig(),
                    alertSettings(), poolFetch(), poolUpdate());
            config.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cloudApiSettings"));
        }
    }

    /**
     * provisioningTemplate is required.
     */
    @Test
    public void missingProvisioningTemplate() {
        try {
            BaseCloudPoolConfig config = new BaseCloudPoolConfig(name(), cloudApiSettings(), null, scaleInConfig(),
                    alertSettings(), poolFetch(), poolUpdate());
            config.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("provisioningTemplate"));
        }
    }

    /**
     * Sample pool name.
     *
     * @return
     */
    private String name() {
        return "webserver-pool";
    }

    /**
     * Sample {@link BaseCloudPoolConfig#getCloudApiSettings()}.
     *
     * @return
     */
    private JsonObject cloudApiSettings() {
        return JsonUtils.parseJsonString("{\"apiUser\": \"foo\", " + "\"apiPassword\": \"secret\"}").getAsJsonObject();
    }

    /**
     * Sample {@link BaseCloudPoolConfig#getCloudApiSettings()}.
     *
     * @return
     */
    private JsonObject provisioningTemplate() {
        return JsonUtils.parseJsonString("{\"size\": \"medium\", " + "\"image\": \"ubuntu-16.04\"}").getAsJsonObject();
    }

    private ScaleInConfig scaleInConfig() {
        return new ScaleInConfig(VictimSelectionPolicy.OLDEST);
    }

    private AlertersConfig alertSettings() {
        return new AlertersConfig(Arrays.asList(smtpAlerter()), Arrays.asList(httpAlerter()));
    }

    private SmtpAlerterConfig smtpAlerter() {
        return new SmtpAlerterConfig(Arrays.asList("recipient@org.com"), "sender@elastisys.com", "subject",
                "INFO|ERROR", smtpClientConfig());
    }

    private SmtpClientConfig smtpClientConfig() {
        return new SmtpClientConfig("some.mail.host", 25, smtpAuth());
    }

    private SmtpClientAuthentication smtpAuth() {
        return new SmtpClientAuthentication("userName", "password");
    }

    private HttpAlerterConfig httpAlerter() {
        List<String> urls = Arrays.asList("https://some.host/");
        String severityFilter = "INFO|WARN|ERROR";
        HttpAuthConfig auth = new HttpAuthConfig(new BasicCredentials("user", "secret"), null);
        int connectionTimeout = 1000;
        int socketTimeout = 1000;

        return new HttpAlerterConfig(urls, severityFilter, auth, connectionTimeout, socketTimeout);
    }

    private PoolFetchConfig poolFetch() {
        RetriesConfig retriesConfig = new RetriesConfig(3, new TimeInterval(2L, TimeUnit.SECONDS));
        TimeInterval refreshInterval = new TimeInterval(20L, TimeUnit.SECONDS);
        TimeInterval reachabilityTimeout = new TimeInterval(2L, TimeUnit.MINUTES);
        return new PoolFetchConfig(retriesConfig, refreshInterval, reachabilityTimeout);
    }

    private PoolUpdateConfig poolUpdate() {
        return new PoolUpdateConfig(new TimeInterval(1L, TimeUnit.MINUTES));
    }

}
