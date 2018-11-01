package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestCluster.CA_CERT_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientConfig;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
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

    /** Sample kubeconfig */
    private static final String KUBECONFIG_PATH = "src/test/resources/kubeconfig/kubeconfig.yaml";

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
     * {@link KubernetesCloudPoolConfig} client settings can be created either
     * from a {@code kubeconfig} file or from a {@code apiServerUrl} and
     * {@code auth}. Here we test with {@code kubeconfig}.
     */
    @Test
    public void buildFromKubeConfig() throws Exception {
        KubernetesCloudPoolConfig config = new KubernetesCloudPoolConfig(KUBECONFIG_PATH, null, null, POD_POOL,
                UPDATE_INTERVAL, ALERTS);
        config.validate();

        assertThat(config.getKubeConfigPath(), is(KUBECONFIG_PATH));
        assertThat(config.getApiServerUrl(), is(nullValue()));
        assertThat(config.getAuth(), is(nullValue()));
        assertThat(config.getPodPool(), is(POD_POOL));
        assertThat(config.getUpdateInterval(), is(UPDATE_INTERVAL));
        assertThat(config.getAlerts(), is(ALERTS));

        // verify that an expected ClientConfig is produced
        ClientConfig expectedClientConfig = new ClientConfig("https://192.168.99.104:8443", ClientCredentials.builder()
                .certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).serverCertPath(CA_CERT_PATH).build());
        ClientConfig clientConfig = config.getClientConfig();
        assertThat(clientConfig, is(expectedClientConfig));
    }

    /**
     * {@link KubernetesCloudPoolConfig} client settings can be created either
     * from a {@code kubeconfig} file or from a {@code apiServerUrl} and
     * {@code auth}. Here we test building from {@code apiServerUrl} and
     * {@code auth}.
     */
    @Test
    public void buildFromApiServerUrlAndAuth() throws Exception {
        KubernetesCloudPoolConfig config = new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH, POD_POOL,
                UPDATE_INTERVAL, ALERTS);
        config.validate();

        assertThat(config.getApiServerUrl(), is(API_SERVER_URL));
        assertThat(config.getAuth(), is(AUTH));
        assertThat(config.getPodPool(), is(POD_POOL));
        assertThat(config.getUpdateInterval(), is(UPDATE_INTERVAL));
        assertThat(config.getAlerts(), is(ALERTS));

        // verify that an expected ClientConfig is produced
        ClientConfig expectedClientConfig = new ClientConfig(API_SERVER_URL,
                ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).build());
        ClientConfig clientConfig = config.getClientConfig();
        assertThat(clientConfig, is(expectedClientConfig));
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
        KubernetesCloudPoolConfig config = new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH,
                new PodPoolConfig(nullNamespace, null, null, DEPLOYMENT), nullUpdateInterval, nullAlerts);
        config.validate();

        assertThat(config.getPodPool().getNamespace(), is(PodPoolConfig.DEFAULT_NAMESPACE));
        assertThat(config.getUpdateInterval(), is(KubernetesCloudPoolConfig.DEFAULT_UPDATE_INTERVAL));
        assertThat(config.getAlerts(), is(nullValue()));
    }

    /**
     * Missing {@code kubeconfig} and no {@code apiServerUrl} specified.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withMissingApiServerUrl() {
        new KubernetesCloudPoolConfig(null, null, AUTH, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMalformedApiServerUrl() {
        String invalidApiServerUrl = "tcp://host:443";
        new KubernetesCloudPoolConfig(null, invalidApiServerUrl, AUTH, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    /**
     * Missing {@code kubeconfig} and no {@code auth} specified.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withMissingAuth() {
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, null, POD_POOL, UPDATE_INTERVAL, ALERTS).validate();
    }

    /**
     * Make sure {@link AuthConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withInvalidAuth() {
        AuthConfig authMissingClientKey = AuthConfig.builder().certData(CLIENT_CERT_PATH).build();
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, authMissingClientKey, POD_POOL, UPDATE_INTERVAL, ALERTS)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withMissingPodPool() {
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH, null, UPDATE_INTERVAL, ALERTS).validate();
    }

    /**
     * Make sure {@link PodPoolConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withInvalidPodPool() {
        // no replicationController/replicaSet/deployment specified
        PodPoolConfig noApiObjectSpecified = new PodPoolConfig(NAMESPACE, null, null, null);
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH, noApiObjectSpecified, UPDATE_INTERVAL, ALERTS)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withInvalidUpdateInterval() {
        TimeInterval invalidUpdateInterval = TimeInterval.seconds(0);
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH, POD_POOL, invalidUpdateInterval, ALERTS).validate();
    }

    /**
     * Make sure {@link AlertersConfig} gets validated as part of
     * {@link KubernetesCloudPoolConfig} validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalAlerts() {
        new KubernetesCloudPoolConfig(null, API_SERVER_URL, AUTH, POD_POOL, UPDATE_INTERVAL, invalidAlertSettings())
                .validate();
    }

    /**
     * It is ambiguous to specify both a {@code kubeconfig} file and
     * {@code apiServerUrl} settings.
     */
    @Test
    public void kubeconfigMutuallyExclusiveWithApiServerUrl() {
        try {
            new KubernetesCloudPoolConfig(KUBECONFIG_PATH, API_SERVER_URL, null, POD_POOL, null, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("config: kubeConfigPath is mutually exclusive with apiServerUrl"));
        }
    }

    /**
     * It is ambiguous to specify both a {@code kubeconfig} file and
     * {@code auth} settings.
     */
    @Test
    public void kubeconfigMutuallyExclusiveWithAuth() {
        try {
            new KubernetesCloudPoolConfig(KUBECONFIG_PATH, null, AUTH, POD_POOL, null, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("config: kubeConfigPath is mutually exclusive with auth"));
        }
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
