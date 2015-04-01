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

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Verifies that configuring and re-configuring a {@link BaseCloudPool} works as
 * expected.
 */
public class TestBaseCloudPoolConfiguration {

	private final CloudPoolDriver driverMock = mock(CloudPoolDriver.class);
	/** Object under test. */
	private BaseCloudPool cloudPool;

	@Before
	public void onSetup() {
		this.cloudPool = new BaseCloudPool(this.driverMock);
	}

	@Test
	public void testConfigureWithValidConfig() throws CloudPoolException {
		String configFile = "config/valid-cloudpool-config-minimal.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudPool.configure(validConfig);

		Optional<JsonObject> config = this.cloudPool.getConfiguration();
		assertTrue(config.isPresent());
		assertEquals(validConfig, config.get());

		BaseCloudPoolConfig actualConfig = JsonUtils.toObject(validConfig,
				BaseCloudPoolConfig.class);
		// ensure that the Login config is passed through to the ScalingGroup
		verify(this.driverMock).configure(actualConfig);
	}

	@Test
	public void testConfigureWithSmtpAlerts() throws Exception {
		// TODO
	}

	@Test
	public void testConfigureWithHttpAlerts() {
		// TODO
	}

	@Test
	public void testConfigureWithSmtpAndHttpAlerts() {
		// TODO
	}

	/**
	 * Verify that correct default values are used when fields with default
	 * values are left out from the {@link AlertSettings}.
	 */
	@Test
	public void testConfigureWithDefaultAlertSettings()
			throws CloudPoolException {
		String configFile = "config/valid-cloudpool-config-with-default-alert-settings.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudPool.configure(validConfig);

		BaseCloudPoolConfig config = this.cloudPool.config();
		AlertSettings alertSettings = config.getAlerts();
		// verify filled in values
		assertThat(alertSettings.getSender(), is("noreply@elastisys.com"));
		assertThat(alertSettings.getRecipients(),
				is(asList("recipient@elastisys.com")));
		assertThat(alertSettings.getMailServer().getSmtpHost(),
				is("mail.elastisys.com"));
		// verify default values
		assertThat(alertSettings.getMailServer().getSmtpPort(), is(25));
		assertThat(alertSettings.getMailServer().getAuthentication(),
				is(nullValue()));
		assertThat(alertSettings.getMailServer().isUseSsl(), is(false));
	}

	@Test
	public void testReConfigure() throws CloudPoolException {
		// configure
		JsonObject oldConfig = JsonUtils
				.parseJsonResource("config/valid-cloudpool-config-minimal.json");
		this.cloudPool.configure(oldConfig);
		assertEquals(oldConfig, this.cloudPool.getConfiguration().get());

		// re-configure
		JsonObject newConfig = JsonUtils
				.parseJsonResource("config/valid-cloudpool-config-with-alerts.json");
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

		String configFile = "config/valid-cloudpool-config-minimal.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudPool.configure(validConfig);

		assertTrue(this.cloudPool.isStarted());
	}

	@Test(expected = CloudPoolException.class)
	public void testConfigureWithEmptyConfig() throws CloudPoolException {
		JsonObject emptyConfig = new JsonObject();
		this.cloudPool.configure(emptyConfig);
	}

	@Test(expected = CloudPoolException.class)
	public void testConfigureWithIllegalConfig() throws CloudPoolException {
		JsonObject illegalConfig = JsonUtils
				.parseJsonResource("config/invalid-cloudpool-config-missing-cloudpool.json");
		this.cloudPool.configure(illegalConfig);
	}
}
