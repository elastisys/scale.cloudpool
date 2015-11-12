package com.elastisys.scale.cloudpool.commons.basepool;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.config.AlertsConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.TimeInterval;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerter;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

/**
 * Verifies that configuring and re-configuring a {@link BaseCloudPool} works as
 * expected.
 */
public class TestBaseCloudPoolConfiguration {

	private static final File STATE_STORAGE_DIR = new File("target/state");
	private static final StateStorage STATE_STORAGE = StateStorage
			.builder(STATE_STORAGE_DIR).build();

	/** Mocked {@link EventBus} to capture events sent by the cloud pool. */
	private final EventBus eventBusMock = mock(EventBus.class);

	private final CloudPoolDriver driverMock = mock(CloudPoolDriver.class);
	/** Object under test. */
	private BaseCloudPool cloudPool;

	@Before
	public void onSetup() throws IOException {
		FileUtils.deleteRecursively(STATE_STORAGE_DIR);
		this.cloudPool = new BaseCloudPool(STATE_STORAGE, this.driverMock,
				this.eventBusMock);
	}

	@Test
	public void testConfigureWithValidConfig() throws CloudPoolException {
		String configFile = "config/valid-cloudpool-config-minimal.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		Optional<JsonObject> config = this.cloudPool.getConfiguration();
		assertTrue(config.isPresent());
		assertEquals(validConfig, config.get());

		BaseCloudPoolConfig actualConfig = JsonUtils.toObject(validConfig,
				BaseCloudPoolConfig.class);
		// ensure that the Login config is passed through to the ScalingGroup
		verify(this.driverMock).configure(actualConfig);

		// check defaults
		BaseCloudPoolConfig conf = this.cloudPool.config();
		assertThat(conf.getAlerts(), is(nullValue()));
		assertThat(conf.getPoolFetch(),
				is(BaseCloudPoolConfig.DEFAULT_POOL_FETCH_CONFIG));
		assertThat(conf.getPoolUpdate(),
				is(BaseCloudPoolConfig.DEFAULT_POOL_UPDATE_CONFIG));
	}

	/**
	 * Configure with a single {@link SmtpAlerter}.
	 */
	@Test
	public void testConfigureWithSmtpAlerts() throws Exception {
		String configFile = "config/valid-cloudpool-config-with-smtp-alerts.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(1));
		assertThat(alertSettings.getHttpAlerters().size(), is(0));
		// verify filled in values
		SmtpAlerterConfig smtpAlerter = alertSettings.getSmtpAlerters().get(0);
		assertThat(smtpAlerter.getSender(), is("noreply@elastisys.com"));
		assertThat(smtpAlerter.getRecipients(),
				is(asList("recipient@elastisys.com")));
		assertThat(smtpAlerter.getSeverityFilter().getFilterExpression(),
				is("INFO|WARN|ERROR|FATAL"));
		assertThat(smtpAlerter.getSmtpClientConfig().getSmtpHost(),
				is("mail.elastisys.com"));
		assertThat(smtpAlerter.getSmtpClientConfig().getSmtpPort(), is(465));
		assertThat(smtpAlerter.getSmtpClientConfig().getAuthentication(),
				is(new SmtpClientAuthentication("user", "secret")));
		assertThat(smtpAlerter.getSmtpClientConfig().isUseSsl(), is(true));
	}

	/**
	 * Configure with two {@link SmtpAlerter}.
	 */
	@Test
	public void testConfigureWithDoubleSmtpAlerters() throws Exception {
		String configFile = "config/valid-cloudpool-config-with-two-smtp-alerters.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(2));
		assertThat(alertSettings.getHttpAlerters().size(), is(0));
		// verify filled in values
		SmtpAlerterConfig smtpAlerter1 = alertSettings.getSmtpAlerters().get(0);
		assertThat(smtpAlerter1.getSender(), is("noreply@elastisys.com"));
		assertThat(smtpAlerter1.getRecipients(),
				is(asList("recipient1@elastisys.com")));
		assertThat(smtpAlerter1.getSeverityFilter().getFilterExpression(),
				is("WARN|ERROR"));
		assertThat(smtpAlerter1.getSmtpClientConfig().getSmtpHost(),
				is("mail1.elastisys.com"));
		assertThat(smtpAlerter1.getSmtpClientConfig().getSmtpPort(), is(465));
		assertThat(smtpAlerter1.getSmtpClientConfig().getAuthentication(),
				is(new SmtpClientAuthentication("user1", "secret1")));
		assertThat(smtpAlerter1.getSmtpClientConfig().isUseSsl(), is(true));

		SmtpAlerterConfig smtpAlerter2 = alertSettings.getSmtpAlerters().get(1);
		assertThat(smtpAlerter2.getSender(), is("noreply@elastisys.com"));
		assertThat(smtpAlerter2.getRecipients(),
				is(asList("recipient2@elastisys.com")));
		assertThat(smtpAlerter2.getSeverityFilter().getFilterExpression(),
				is("DEBUG|INFO"));
		assertThat(smtpAlerter2.getSmtpClientConfig().getSmtpHost(),
				is("mail2.elastisys.com"));
		assertThat(smtpAlerter2.getSmtpClientConfig().getSmtpPort(), is(25));
		assertThat(smtpAlerter2.getSmtpClientConfig().getAuthentication(),
				is(new SmtpClientAuthentication("user2", "secret2")));
		assertThat(smtpAlerter2.getSmtpClientConfig().isUseSsl(), is(false));
	}

	/**
	 * Verify that correct default values are used when fields with default
	 * values are left out from the SMTP alerts configuration.
	 */
	@Test
	public void testConfigureWithDefaultSmtpAlertSettings()
			throws CloudPoolException {
		String configFile = "config/valid-cloudpool-config-with-smtp-alerts-using-defaults.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(1));
		assertThat(alertSettings.getHttpAlerters().size(), is(0));
		// verify filled in values
		SmtpAlerterConfig smtpAlerter = alertSettings.getSmtpAlerters().get(0);
		assertThat(smtpAlerter.getSender(), is("noreply@elastisys.com"));
		assertThat(smtpAlerter.getRecipients(),
				is(asList("recipient@elastisys.com")));
		assertThat(smtpAlerter.getSmtpClientConfig().getSmtpHost(),
				is("mail.elastisys.com"));
		// verify default values
		assertThat(smtpAlerter.getSmtpClientConfig().getSmtpPort(), is(25));
		assertThat(smtpAlerter.getSmtpClientConfig().getAuthentication(),
				is(nullValue()));
		assertThat(smtpAlerter.getSmtpClientConfig().isUseSsl(), is(false));
	}

	@Test
	public void testConfigureWithHttpAlerts() {
		String configFile = "config/valid-cloudpool-config-with-http-alerts.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(0));
		assertThat(alertSettings.getHttpAlerters().size(), is(1));
		assertThat(alertSettings.getDuplicateSuppression(),
				is(new TimeInterval(15L, TimeUnit.MINUTES)));
		// verify filled in values
		HttpAlerterConfig httpAlerter1 = alertSettings.getHttpAlerters().get(0);
		assertThat(httpAlerter1.getDestinationUrls(),
				is(asList("https://some.host:443/")));
		assertThat(httpAlerter1.getSeverityFilter().getFilterExpression(),
				is("INFO|WARN|ERROR|FATAL"));
		assertThat(httpAlerter1.getAuth(),
				is(new HttpAuthConfig(new BasicCredentials("user", "secret"),
						null)));
		// defaults
		assertThat(httpAlerter1.getConnectTimeout(),
				is(HttpAlerterConfig.DEFAULT_CONNECTION_TIMEOUT));
		assertThat(httpAlerter1.getSocketTimeout(),
				is(HttpAlerterConfig.DEFAULT_SOCKET_TIMEOUT));
	}

	@Test
	public void testConfigureWithTwoHttpAlerters() {
		String configFile = "config/valid-cloudpool-config-with-two-http-alerters.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(0));
		assertThat(alertSettings.getHttpAlerters().size(), is(2));
		// verify filled in values
		HttpAlerterConfig httpAlerter1 = alertSettings.getHttpAlerters().get(0);
		assertThat(httpAlerter1.getDestinationUrls(),
				is(asList("https://some.host1:443/")));
		assertThat(httpAlerter1.getSeverityFilter().getFilterExpression(),
				is("ERROR|FATAL"));
		assertThat(httpAlerter1.getAuth(),
				is(new HttpAuthConfig(new BasicCredentials("user1", "secret1"),
						null)));

		HttpAlerterConfig httpAlerter2 = alertSettings.getHttpAlerters().get(1);
		assertThat(httpAlerter2.getDestinationUrls(),
				is(asList("https://some.host2:443/")));
		assertThat(httpAlerter2.getSeverityFilter().getFilterExpression(),
				is("INFO|WARN"));
		CertificateCredentials certificateCredentials = new CertificateCredentials(
				"src/test/resources/security/client_keystore.p12", "secret");
		assertThat(httpAlerter2.getAuth(),
				is(new HttpAuthConfig(null, certificateCredentials)));

	}

	@Test
	public void testConfigureWithSmtpAndHttpAlerts() {
		String configFile = "config/valid-cloudpool-config-with-http-and-smtp-alerts.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertsConfig alertSettings = config.getAlerts();
		assertThat(alertSettings.getSmtpAlerters().size(), is(1));
		assertThat(alertSettings.getHttpAlerters().size(), is(1));
		// verify filled in values
		SmtpAlerterConfig smtpAlerter = alertSettings.getSmtpAlerters().get(0);
		assertThat(smtpAlerter.getSender(), is("noreply@elastisys.com"));
		assertThat(smtpAlerter.getRecipients(),
				is(asList("recipient@elastisys.com")));
		assertThat(smtpAlerter.getSmtpClientConfig().getSmtpHost(),
				is("mail.elastisys.com"));

		HttpAlerterConfig httpAlerter = alertSettings.getHttpAlerters().get(0);
		assertThat(httpAlerter.getDestinationUrls(),
				is(asList("https://some.host:443/")));
		assertThat(httpAlerter.getSeverityFilter().getFilterExpression(),
				is("INFO|WARN|ERROR|FATAL"));
		assertThat(httpAlerter.getAuth(),
				is(new HttpAuthConfig(new BasicCredentials("user", "secret"),
						null)));
	}

	@Test
	public void testConfigureWithPoolFetch() {
		String configFile = "config/valid-cloudpool-config-with-pool-fetch.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		assertThat(config.getPoolFetch().getRetries(), is(
				new RetriesConfig(3, new TimeInterval(0L, TimeUnit.SECONDS))));
		assertThat(config.getPoolFetch().getRefreshInterval(),
				is(new TimeInterval(10L, TimeUnit.SECONDS)));
		assertThat(config.getPoolFetch().getReachabilityTimeout(),
				is(new TimeInterval(5L, TimeUnit.MINUTES)));
	}

	@Test
	public void testConfigureWithPoolUpdate() {
		String configFile = "config/valid-cloudpool-config-with-pool-update.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		assertThat(config.getPoolUpdate().getUpdateInterval(),
				is(new TimeInterval(2L, TimeUnit.MINUTES)));
	}

	/**
	 * Configuring a stopped {@link BaseCloudPool} should leave it in that
	 * state.
	 */
	@Test
	public void testConfigureStopped() {
		assertThat(this.cloudPool.isStarted(), is(false));

		// configure
		JsonObject config = JsonUtils
				.parseJsonResource("config/valid-cloudpool-config-minimal.json")
				.getAsJsonObject();
		this.cloudPool.configure(config);
		assertEquals(config, this.cloudPool.getConfiguration().get());

		// should still be stopped
		assertThat(this.cloudPool.isStarted(), is(false));
	}

	/**
	 * Configuring a stared {@link BaseCloudPool} should leave it in that state.
	 */
	@Test
	public void testConfigureStarted() {
		// configure
		JsonObject config = JsonUtils
				.parseJsonResource("config/valid-cloudpool-config-minimal.json")
				.getAsJsonObject();
		this.cloudPool.configure(config);
		this.cloudPool.start();

		assertThat(this.cloudPool.isStarted(), is(true));
		assertEquals(config, this.cloudPool.getConfiguration().get());

		// re-configure
		JsonObject newConfig = JsonUtils
				.parseJsonResource(
						"config/valid-cloudpool-config-with-alerts.json")
				.getAsJsonObject();
		this.cloudPool.configure(newConfig);
		assertEquals(newConfig, this.cloudPool.getConfiguration().get());

		// should still be started
		assertThat(this.cloudPool.isStarted(), is(true));
		assertEquals(newConfig, this.cloudPool.getConfiguration().get());
	}

	@Test
	public void testReConfigure() throws CloudPoolException {
		// configure
		JsonObject oldConfig = JsonUtils
				.parseJsonResource("config/valid-cloudpool-config-minimal.json")
				.getAsJsonObject();
		this.cloudPool.configure(oldConfig);
		assertEquals(oldConfig, this.cloudPool.getConfiguration().get());

		// re-configure
		JsonObject newConfig = JsonUtils
				.parseJsonResource(
						"config/valid-cloudpool-config-with-alerts.json")
				.getAsJsonObject();
		this.cloudPool.configure(newConfig);
		assertEquals(newConfig, this.cloudPool.getConfiguration().get());

		assertFalse(oldConfig.equals(newConfig));
	}

	/**
	 * When the cloud pool is configured, it should only throw exceptions in
	 * case the configuration is invalid (not adhering to schema or obvious
	 * illegal value). Other types of errors, such as a failure to determine
	 * initial pool size should not prevent the pool from starting. It should
	 * survive and (potentially) alert its owner in case something goes awry,
	 * but is should stay up. As an example, the cloud provider API may
	 * temporarily be down, so it may be worth while for the pool to keep on
	 * trying.
	 *
	 * @throws CloudPoolException
	 */
	@Test
	public void testFailureToDetermineInitialPoolSize()
			throws CloudPoolException {
		// set up scaling group mock to raise an exception when pool tries to
		// determine the initial pool size
		when(this.driverMock.listMachines()).thenThrow(
				new CloudPoolDriverException("temporary cloud API outage"));

		String configFile = "config/valid-cloudpool-config-with-pool-fetch.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile)
				.getAsJsonObject();
		this.cloudPool.configure(validConfig);
		this.cloudPool.start();

		assertTrue(this.cloudPool.isStarted());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConfigureWithEmptyConfig() throws CloudPoolException {
		JsonObject emptyConfig = new JsonObject();
		this.cloudPool.configure(emptyConfig);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConfigureWithIllegalConfig() throws CloudPoolException {
		JsonObject illegalConfig = JsonUtils
				.parseJsonResource(
						"config/invalid-cloudpool-config-missing-cloudpool.json")
				.getAsJsonObject();
		this.cloudPool.configure(illegalConfig);
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void parseIllegalConfigMissingScaleOutConfig()
			throws CloudPoolException {
		String jsonConf = "config/invalid-cloudpool-config-missing-scaleoutconfig.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		config.validate();
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void parseIllegalConfigMissingScalingGroup()
			throws CloudPoolException {
		String jsonConf = "config/invalid-cloudpool-config-missing-cloudpool.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		config.validate();
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void parseIllegalConfigMissingScaleInConfig()
			throws CloudPoolException {
		String jsonConf = "config/invalid-cloudpool-config-missing-scaleinconfig.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		config.validate();
	}
}
