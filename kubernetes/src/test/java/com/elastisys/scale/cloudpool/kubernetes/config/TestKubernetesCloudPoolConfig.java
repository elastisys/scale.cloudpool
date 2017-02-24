package com.elastisys.scale.cloudpool.kubernetes.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.google.gson.JsonElement;

/**
 * Exercise {@link KubernetesCloudPoolConfig}.
 */
public class TestKubernetesCloudPoolConfig {
    /** Path to client cert. */
    private static final String CLIENT_CERT_PATH = "src/test/resources/ssl/admin.pem";
    /** Path to client key. */
    private static final String CLIENT_KEY_PATH = "src/test/resources/ssl/admin-key.pem";

    /** Sample API server URL. */
    private static final String API_SERVER_URL = "https://server:1234";
    /** Sample authentication config. */
    private static final AuthConfig AUTH = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
            .build();

    /** Sample {@link PodPoolConfig}. */
    private static final String NAMESPACE = "my-ns";
    private static final String DEPLOYMENT = "nginx-deployment";
    private static final PodPoolConfig POD_POOL = new PodPoolConfig(NAMESPACE, null, null, DEPLOYMENT);

    /** Sample update interval. */
    private static final TimeInterval UPDATE_INTERVAL = TimeInterval.seconds(15);

    /** Sample alert settings */
    private static final AlertersConfig ALERTS = alertSettings();

    /**
     * Should be possible to give explicit values for all parameters.
     */
    @Test
    public void complete() {
        KubernetesCloudPoolConfig config = new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH, POD_POOL,
                UPDATE_INTERVAL, ALERTS);
        config.validate();

        assertThat(config.getApiServerUrl(), is(API_SERVER_URL));
        assertThat(config.getAuth(), is(AUTH));
        assertThat(config.getPodPool(), is(POD_POOL));
        assertThat(config.getUpdateInterval(), is(UPDATE_INTERVAL));
        assertThat(config.getAlerts(), is(ALERTS));
    }

    /**
     * Defaults exist for certain parameters. It should be possible to leave out
     * {@code podPool.namespace}, {@code updateInterval}, and {@code alerts}.
     */
    @Test
    public void defaults() {
        String nullNamespace = null;
        TimeInterval nullUpdateInterval = null;
        AlertersConfig nullAlerts = null;
        KubernetesCloudPoolConfig config = new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH,
                new PodPoolConfig(nullNamespace, null, null, DEPLOYMENT), nullUpdateInterval, nullAlerts);
        config.validate();

        assertThat(config.getPodPool().getNamespace(), is(PodPoolConfig.DEFAULT_NAMESPACE));
        assertThat(config.getUpdateInterval(), is(KubernetesCloudPoolConfig.DEFAULT_UPDATE_INTERVAL));
        assertThat(config.getAlerts(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMissingApiServerUrl() {
        new KubernetesCloudPoolConfig(null, AUTH, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMalformedApiServerUrl() {
        String invalidApiServerUrl = "tcp://host:443";
        new KubernetesCloudPoolConfig(invalidApiServerUrl, AUTH, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMissingAuth() {
        new KubernetesCloudPoolConfig(API_SERVER_URL, null, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    /**
     * Make sure {@link AuthConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withInvalidAuth() {
        AuthConfig authMissingClientKey = AuthConfig.builder().cert(CLIENT_CERT_PATH).build();
        new KubernetesCloudPoolConfig(API_SERVER_URL, authMissingClientKey, POD_POOL, UPDATE_INTERVAL, ALERTS)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMissingPodPool() {
        new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH, null, UPDATE_INTERVAL, ALERTS).validate();
    }

    /**
     * Make sure {@link PodPoolConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withInvalidPodPool() {
        // no replicationController/replicaSet/deployment specified
        PodPoolConfig noApiObjectSpecified = new PodPoolConfig(NAMESPACE, null, null, null);
        new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH, noApiObjectSpecified, UPDATE_INTERVAL, ALERTS).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withInvalidUpdateInterval() {
        TimeInterval invalidUpdateInterval = TimeInterval.seconds(0);
        new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH, POD_POOL, invalidUpdateInterval, ALERTS).validate();
    }

    /**
     * Make sure {@link AlertersConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalAlerts() {
        new KubernetesCloudPoolConfig(API_SERVER_URL, AUTH, POD_POOL, UPDATE_INTERVAL, invalidAlertSettings())
                .validate();
    }

    private static AlertersConfig alertSettings() {
        return new AlertersConfig(Arrays.asList(smtpAlerter()), null);
    }

    private static AlertersConfig invalidAlertSettings() {
        return new AlertersConfig(Arrays.asList(invalidSmtpAlerter()), null);
    }

    private static SmtpAlerterConfig smtpAlerter() {
        return new SmtpAlerterConfig(Arrays.asList("recipient@org.com"), "sender@foo.com", "subject", "ERROR",
                smtpClientConfig());
    }

    private static SmtpAlerterConfig invalidSmtpAlerter() {
        // produce invalid SmtpAlerterConfig without recipients field
        JsonElement settings = JsonUtils.toJson(smtpAlerter());
        settings.getAsJsonObject().remove("recipients");
        return JsonUtils.toObject(settings, SmtpAlerterConfig.class);
    }

    private static SmtpClientConfig smtpClientConfig() {
        return new SmtpClientConfig("some.mail.host", 25, new SmtpClientAuthentication("userName", "password"));
    }
}
