package com.elastisys.scale.cloudpool.kubernetes;

import static com.elastisys.scale.commons.security.pem.PemUtils.parseX509Cert;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;

import org.apache.commons.codec.Charsets;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.client.KubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.config.ApiServerConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.PodPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link KubernetesCloudPool} with respect to validating and
 * applying different configurations.
 */
public class TestConfigureKubernetesCloudPool {

	private static final Path CONFIG_DIR = Paths.get("src", "test", "resources",
			"config");
	private static final Path SSL_DIR = Paths.get("src", "test", "resources",
			"ssl");

	/** Object under test. */
	private KubernetesCloudPool cloudPool;

	@Before
	public void before() {
		KubernetesClient apiClientMock = mock(KubernetesClient.class);
		this.cloudPool = new KubernetesCloudPool(apiClientMock);

		assertThat(this.cloudPool.getStatus().isConfigured(), is(false));
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));
	}

	@Test
	public void configWithDefaults() throws Exception {
		JsonObject config = loadConfig("config-defaults.json");
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		assertThat(this.cloudPool.config().getAuth().hasClientCert(),
				is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientKey(), is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientToken(),
				is(true));
		assertThat(this.cloudPool.config().getAuth().hasServerCert(),
				is(false));

		assertThat(this.cloudPool.config().getApiServer().getPort(),
				is(ApiServerConfig.DEFAULT_PORT));
		assertThat(this.cloudPool.config().getApiServer().getHost(),
				is(ApiServerConfig.DEFAULT_HOST));
		assertThat(this.cloudPool.config().getPodPool().getNamespace(),
				is(PodPoolConfig.DEFAULT_NAMESPACE));
	}

	/**
	 * Set a configuration specifying client cert auth paths.
	 */
	@Test
	public void setCertAuthByPathConfig() throws Exception {
		JsonObject config = loadConfig("config-cert-paths.json");
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		assertThat(this.cloudPool.config().getAuth().hasClientCert(), is(true));
		assertThat(this.cloudPool.config().getAuth().hasClientKey(), is(true));
		assertThat(this.cloudPool.config().getAuth().hasClientToken(),
				is(false));
		assertThat(this.cloudPool.config().getAuth().hasServerCert(), is(true));
		assertThat(this.cloudPool.config().getAuth().getClientCert(),
				is(loadCert("admin.pem")));
		assertThat(this.cloudPool.config().getAuth().getClientKey(),
				is(loadKey("admin-key.pem")));
		assertThat(this.cloudPool.config().getAuth().getServerCert(),
				is(loadCert("ca.pem")));
	}

	/**
	 * Set a configuration specifying client cert auth as base64-encoded PEM
	 * strings.
	 */
	@Test
	public void setCertAuthByContentConfig() throws Exception {
		JsonObject config = loadConfig("config-cert-content.json");
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		assertThat(this.cloudPool.config().getAuth().hasClientCert(), is(true));
		assertThat(this.cloudPool.config().getAuth().hasClientKey(), is(true));
		assertThat(this.cloudPool.config().getAuth().hasClientToken(),
				is(false));
		assertThat(this.cloudPool.config().getAuth().hasServerCert(), is(true));
		assertThat(this.cloudPool.config().getAuth().getClientCert(),
				is(loadCert("admin.pem")));
		assertThat(this.cloudPool.config().getAuth().getClientKey(),
				is(loadKey("admin-key.pem")));
		assertThat(this.cloudPool.config().getAuth().getServerCert(),
				is(loadCert("ca.pem")));
	}

	/**
	 * Set a configuration specifying client token auth via a file reference.
	 */
	@Test
	public void setTokenAuthByPathConfig() throws Exception {
		JsonObject config = loadConfig("config-token-path.json");
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		assertThat(this.cloudPool.config().getAuth().hasClientCert(),
				is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientKey(), is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientToken(),
				is(true));
		assertThat(this.cloudPool.config().getAuth().hasServerCert(), is(true));

		assertThat(this.cloudPool.config().getAuth().getClientToken(),
				is(loadToken("auth-token.base64")));
		assertThat(this.cloudPool.config().getAuth().getServerCert(),
				is(loadCert("ca.pem")));
	}

	/**
	 * Set a configuration specifying client token auth as a JWT string.
	 */
	@Test
	public void setTokenAuthByContentConfig() throws Exception {
		JsonObject config = loadConfig("config-token-content.json");
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		assertThat(this.cloudPool.config().getAuth().hasClientCert(),
				is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientKey(), is(false));
		assertThat(this.cloudPool.config().getAuth().hasClientToken(),
				is(true));
		assertThat(this.cloudPool.config().getAuth().hasServerCert(), is(true));

		assertThat(this.cloudPool.config().getAuth().getClientToken(),
				is(loadToken("auth-token.base64")));
		assertThat(this.cloudPool.config().getAuth().getServerCert(),
				is(loadCert("ca.pem")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void certSpecifiedBothByPathAndContent() throws Exception {
		JsonObject config = loadConfig(
				"invalid-config-cert-path-and-content.json");
		this.cloudPool.configure(config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void configMissingClientAuth() throws Exception {
		JsonObject config = loadConfig("invalid-config-no-client-auth.json");
		this.cloudPool.configure(config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void configMissingApiServer() throws Exception {
		JsonObject config = loadConfig("invalid-config-missing-apiserver.json");
		this.cloudPool.configure(config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void configMissingReplicationControllerServer() throws Exception {
		JsonObject config = loadConfig("invalid-config-missing-rc.json");
		this.cloudPool.configure(config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void configWithNonExistentKeyPath() {
		JsonObject config = loadConfig(
				"invalid-config-nonexistent-key-path.json");
		this.cloudPool.configure(config);
	}

	/**
	 * Loads a configuration file from the {@link #CONFIG_DIR}.
	 *
	 * @param configFileName
	 * @return
	 */
	private static JsonObject loadConfig(String configFileName) {
		File configFile = configFile(configFileName);
		return JsonUtils.parseJsonFile(configFile).getAsJsonObject();
	}

	private static File configFile(String configFileName) {
		return new File(CONFIG_DIR.toFile(), configFileName);
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
		return Files.toString(sslFile(tokenFileName), Charsets.UTF_8);
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
