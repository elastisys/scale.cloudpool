package com.elastisys.scale.cloudpool.kubernetes;

import static com.elastisys.scale.commons.security.pem.PemUtils.parseX509Cert;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientConfig;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.PodPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.DeploymentPodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.ReplicaSetPodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.ReplicationControllerPodPool;
import com.elastisys.scale.cloudpool.kubernetes.testutils.AuthUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link KubernetesCloudPool} with a mocked
 * {@link ApiServerClient}.
 */
public class TestKubernetesCloudPoolConfiguration {
    /** Sample Kubernetes namespace. */
    private static final String NAMESPACE = "my-ns";

    /** Path to client cert. */
    private static final String CLIENT_CERT_PATH = "src/test/resources/ssl/admin.pem";
    /** Path to client key. */
    private static final String CLIENT_KEY_PATH = "src/test/resources/ssl/admin-key.pem";
    /** Path to client token. */
    private static final String CLIENT_TOKEN_PATH = "src/test/resources/ssl/auth-token";
    /** Path to server/CA cert. */
    private static final String SERVER_CERT_PATH = "src/test/resources/ssl/ca.pem";
    /** Path to a kubeconfig.yaml with cert/key paths. */
    private static final String KUBECONFIG_PATH = "src/test/resources/kubeconfig/kubeconfig.yaml";
    /** Path to a kubeconfig.yaml with embedded cert/keys. */
    private static final String KUBECONFIG_EMBEDDED_PATH = "src/test/resources/kubeconfig/kubeconfig-embedded.yaml";

    private static final String API_SERVER_URL = "https://kube.api:443";

    private static final AuthConfig CERT_AUTH_BY_PATH = AuthConfig.builder().certPath(CLIENT_CERT_PATH)
            .keyPath(CLIENT_KEY_PATH).build();
    private static final AuthConfig CERT_AUTH_BY_CONTENT = AuthConfig.builder()
            .certData(AuthUtils.loadBase64(CLIENT_CERT_PATH)).keyData(AuthUtils.loadBase64(CLIENT_KEY_PATH)).build();
    private static final AuthConfig TOKEN_AUTH_BY_PATH = AuthConfig.builder().tokenPath(CLIENT_TOKEN_PATH)
            .serverCertData(AuthUtils.loadBase64(SERVER_CERT_PATH)).build();
    private static final AuthConfig TOKEN_AUTH_BY_CONTENT = AuthConfig.builder()
            .tokenData(AuthUtils.loadString(CLIENT_TOKEN_PATH)).serverCertPath(SERVER_CERT_PATH).build();

    /** Sample {@link PodPoolConfig} for a ReplicationController. */
    private static final PodPoolConfig RC_POD_POOL = new PodPoolConfig(NAMESPACE, "nginx-rc", null, null);
    /** Sample {@link PodPoolConfig} for a ReplicaSet. */
    private static final PodPoolConfig RS_POD_POOL = new PodPoolConfig(NAMESPACE, null, "nginx-rs", null);
    /** Sample {@link PodPoolConfig} for a Deployment. */
    private static final PodPoolConfig DEP_POD_POOL = new PodPoolConfig(NAMESPACE, null, null, "nginx-deployment");
    /** Sample update interval. */
    private static final TimeInterval UPDATE_INTERVAL = TimeInterval.seconds(10);

    private static final Path SSL_DIR = Paths.get("src", "test", "resources", "ssl");

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    /**
     * Mocked {@link ApiServerClient} to which configurations should be passed.
     */
    private ApiServerClient mockApiServerClient;

    /** Object under test. */
    private KubernetesCloudPool cloudPool;

    @Before
    public void before() {
        this.mockApiServerClient = mock(ApiServerClient.class);
        this.cloudPool = new KubernetesCloudPool(this.mockApiServerClient, this.executor);

        assertThat(this.cloudPool.getConfiguration().isPresent(), is(false));
        assertThat(this.cloudPool.getStatus().isConfigured(), is(false));
        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
    }

    /**
     * When configured to manage a ReplicationController, a
     * {@link ReplicationControllerPodPool} should be created.
     */
    @Test
    public void configureToManageReplicationController() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, CERT_AUTH_BY_PATH,
                RC_POD_POOL, UPDATE_INTERVAL, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        // verify that config gets passed along to ApiServerClient
        ClientConfig expectedClientConfig = new ClientConfig(API_SERVER_URL,
                ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).build());
        verify(this.mockApiServerClient).configure(expectedClientConfig);
        // verify that a ReplicationController PodPool gets created.
        assertThat(this.cloudPool.podPool(), is(instanceOf(ReplicationControllerPodPool.class)));
    }

    /**
     * When configured to manage a ReplicaSet, a {@link ReplicaSetPodPool}
     * should be created.
     */
    @Test
    public void configureToManageReplicaSet() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, CERT_AUTH_BY_CONTENT,
                RS_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        // verify that config gets passed along to ApiServerClient
        ClientConfig expectedClientConfig = new ClientConfig(API_SERVER_URL,
                ClientCredentials.builder().certData(AuthUtils.loadBase64(CLIENT_CERT_PATH))
                        .keyData(AuthUtils.loadBase64(CLIENT_KEY_PATH)).build());
        verify(this.mockApiServerClient).configure(expectedClientConfig);
        // verify that a ReplicaSet PodPool gets created.
        assertThat(this.cloudPool.podPool(), is(instanceOf(ReplicaSetPodPool.class)));
    }

    /**
     * When configured to manage a Deployment, a {@link DeploymentPodPoolPool}
     * should be created.
     */
    @Test
    public void configureToManageDeployment() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, TOKEN_AUTH_BY_CONTENT,
                DEP_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        // verify that config gets passed along to ApiServerClient
        ClientConfig expectedClientConfig = new ClientConfig(API_SERVER_URL, ClientCredentials.builder()
                .tokenData(AuthUtils.loadString(CLIENT_TOKEN_PATH)).serverCertPath(SERVER_CERT_PATH).build());
        verify(this.mockApiServerClient).configure(expectedClientConfig);
        // verify that a DeploymentPodPool PodPool gets created.
        assertThat(this.cloudPool.podPool(), is(instanceOf(DeploymentPodPool.class)));
    }

    /**
     * Defaults should be provided for optional parameters
     * {@code podPool.namespace} and {@code updateInterval}
     */
    @Test
    public void configureWithDefaults() throws Exception {
        String nullNamespace = null;
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, TOKEN_AUTH_BY_CONTENT,
                new PodPoolConfig(nullNamespace, "my-rc", null, null), null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);

        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        assertThat(this.cloudPool.config().getPodPool().getNamespace(), is(PodPoolConfig.DEFAULT_NAMESPACE));
        assertThat(this.cloudPool.config().getUpdateInterval(), is(KubernetesCloudPoolConfig.DEFAULT_UPDATE_INTERVAL));
    }

    /**
     * Set a configuration specifying client cert auth via file system paths.
     */
    @Test
    public void configureWithCertAuthByPath() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, CERT_AUTH_BY_PATH,
                RC_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientCredentials credentials = this.cloudPool.config().getClientConfig().getCredentials();
        assertThat(credentials.hasCert(), is(true));
        assertThat(credentials.hasKey(), is(true));
        assertThat(credentials.hasToken(), is(false));
        assertThat(credentials.hasServerCert(), is(false));
        assertThat(credentials.getCert(), is(loadCert("admin.pem")));
        assertThat(credentials.getKey(), is(loadKey("admin-key.pem")));
    }

    /**
     * Set a configuration specifying client cert auth as base64-encoded PEM
     * strings.
     */
    @Test
    public void configureWithCertAuthByContent() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, CERT_AUTH_BY_CONTENT,
                RC_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);

        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientCredentials credentials = this.cloudPool.config().getClientConfig().getCredentials();
        assertThat(credentials.hasCert(), is(true));
        assertThat(credentials.hasKey(), is(true));
        assertThat(credentials.hasToken(), is(false));
        assertThat(credentials.hasServerCert(), is(false));
        assertThat(credentials.getCert(), is(loadCert("admin.pem")));
        assertThat(credentials.getKey(), is(loadKey("admin-key.pem")));
    }

    /**
     * Set a configuration specifying client token auth via a file system path.
     */
    @Test
    public void configureTokenAuthByPath() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, TOKEN_AUTH_BY_PATH,
                RC_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientCredentials credentials = this.cloudPool.config().getClientConfig().getCredentials();
        assertThat(credentials.hasCert(), is(false));
        assertThat(credentials.hasKey(), is(false));
        assertThat(credentials.hasToken(), is(true));
        assertThat(credentials.hasServerCert(), is(true));
        assertThat(credentials.getToken(), is(loadToken("auth-token")));
        assertThat(credentials.getServerCert(), is(loadCert("ca.pem")));
    }

    /**
     * Set a configuration specifying client token auth as a base64-encoded JWT.
     */
    @Test
    public void configureTokenAuthByContent() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(null, API_SERVER_URL, TOKEN_AUTH_BY_CONTENT,
                RC_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientCredentials credentials = this.cloudPool.config().getClientConfig().getCredentials();
        assertThat(credentials.hasCert(), is(false));
        assertThat(credentials.hasKey(), is(false));
        assertThat(credentials.hasToken(), is(true));
        assertThat(credentials.hasServerCert(), is(true));
        assertThat(credentials.getToken(), is(loadToken("auth-token")));
        assertThat(credentials.getServerCert(), is(loadCert("ca.pem")));
    }

    /**
     * Configure client authentication from a {@code kubeconfig} file with
     * cert/key path references (i.e. cert/keys are not embedded into the
     * kubeconfig file).
     */
    @Test
    public void configureWithKubeconfigWithPathReferences() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(KUBECONFIG_PATH, null, null, RC_POD_POOL, null,
                null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientConfig clientConfig = this.cloudPool.config().getClientConfig();
        assertThat(clientConfig.getApiServerUrl(), is("https://192.168.99.104:8443"));
        ClientCredentials credentials = clientConfig.getCredentials();
        assertThat(credentials.hasCert(), is(true));
        assertThat(credentials.hasKey(), is(true));
        assertThat(credentials.hasToken(), is(false));
        assertThat(credentials.hasServerCert(), is(true));
        assertThat(credentials.getCert(), is(loadCert("admin.pem")));
        assertThat(credentials.getKey(), is(loadKey("admin-key.pem")));
        assertThat(credentials.getServerCert(), is(loadCert("ca.pem")));
    }

    /**
     * Configure client authentication from a {@code kubeconfig} file with
     * embedded cert/keys.
     */
    @Test
    public void configureWithKubeconfigWithEmbeddedCerts() throws Exception {
        KubernetesCloudPoolConfig conf = new KubernetesCloudPoolConfig(KUBECONFIG_EMBEDDED_PATH, null, null,
                RC_POD_POOL, null, null);
        JsonObject config = JsonUtils.toJson(conf).getAsJsonObject();
        this.cloudPool.configure(config);
        assertThat(this.cloudPool.getConfiguration().get(), is(config));

        ClientConfig clientConfig = this.cloudPool.config().getClientConfig();
        assertThat(clientConfig.getApiServerUrl(), is("https://192.168.99.104:8443"));
        ClientCredentials credentials = clientConfig.getCredentials();
        assertThat(credentials.hasCert(), is(true));
        assertThat(credentials.hasKey(), is(true));
        assertThat(credentials.hasToken(), is(false));
        assertThat(credentials.hasServerCert(), is(true));
        assertThat(credentials.getCert(), is(loadCert("admin.pem")));
        assertThat(credentials.getKey(), is(loadKey("admin-key.pem")));
        assertThat(credentials.getServerCert(), is(loadCert("ca.pem")));
    }

    /**
     * Load a certificate from the {@link #SSL_DIR}.
     *
     * @param certFileName
     * @return
     */
    private Certificate loadCert(String certFileName) throws Exception {
        return parseX509Cert(sslFile(certFileName));
    }

    /**
     * Loads a key from the {@link #SSL_DIR}.
     *
     * @param keyFileName
     * @return
     * @throws Exception
     */
    private RSAPrivateKey loadKey(String keyFileName) throws Exception {
        return PemUtils.parseRsaPrivateKey(sslFile(keyFileName));
    }

    private String loadToken(String tokenFileName) throws Exception {
        return IoUtils.toString(sslFile(tokenFileName), StandardCharsets.UTF_8);
    }

    /**
     * Returns one of the {@link File}s in the {@link #SSL_DIR} test resource
     * directory.
     *
     * @param fileName
     * @return
     */
    private static File sslFile(String fileName) {
        return new File(SSL_DIR.toFile(), fileName);
    }

}
