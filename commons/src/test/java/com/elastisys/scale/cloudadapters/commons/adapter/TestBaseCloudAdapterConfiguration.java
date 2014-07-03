package com.elastisys.scale.cloudadapters.commons.adapter;

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

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Verifies that configuring and re-configuring a {@link BaseCloudAdapter} works
 * as expected.
 * 
 * 
 * 
 */
public class TestBaseCloudAdapterConfiguration {

	private final ScalingGroup scalingGroupMock = mock(ScalingGroup.class);
	/** Object under test. */
	private BaseCloudAdapter cloudAdapter;

	@Before
	public void onSetup() {
		this.cloudAdapter = new BaseCloudAdapter(this.scalingGroupMock);
	}

	@Test
	public void testGetConfigurationSchema() {
		Optional<JsonObject> schema = this.cloudAdapter
				.getConfigurationSchema();

		assertTrue(schema.isPresent());
		assertEquals(schema.get(),
				JsonUtils.parseJsonResource("baseadapter-schema.json"));
	}

	@Test
	public void testConfigureWithValidConfig() throws CloudAdapterException {
		String configFile = "config/valid-cloudadapter-config-minimal.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudAdapter.configure(validConfig);

		Optional<JsonObject> config = this.cloudAdapter.getConfiguration();
		assertTrue(config.isPresent());
		assertEquals(validConfig, config.get());

		BaseCloudAdapterConfig actualConfig = JsonUtils.toObject(validConfig,
				BaseCloudAdapterConfig.class);
		// ensure that the Login config is passed through to the ScalingGroup
		verify(this.scalingGroupMock).configure(actualConfig);
	}

	/**
	 * Verify that correct default values are used when fields with default
	 * values are left out from the {@link AlertSettings}.
	 */
	@Test
	public void testConfigureWithDefaultAlertSettings()
			throws CloudAdapterException {
		String configFile = "config/valid-cloudadapter-config-with-default-alert-settings.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudAdapter.configure(validConfig);

		BaseCloudAdapterConfig config = this.cloudAdapter.config();
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
	public void testReConfigure() throws CloudAdapterException {
		// configure
		JsonObject oldConfig = JsonUtils
				.parseJsonResource("config/valid-cloudadapter-config-minimal.json");
		this.cloudAdapter.configure(oldConfig);
		assertEquals(oldConfig, this.cloudAdapter.getConfiguration().get());

		// re-configure
		JsonObject newConfig = JsonUtils
				.parseJsonResource("config/valid-cloudadapter-config-with-alerts.json");
		this.cloudAdapter.configure(newConfig);
		assertEquals(newConfig, this.cloudAdapter.getConfiguration().get());

		assertFalse(oldConfig.equals(newConfig));
	}

	/**
	 * When the cloud adapter is configured, it should only throw exceptions in
	 * case the configuration is invalid (not adhering to schema or obvious
	 * illegal value). Other types of errors, such as a failure to determine
	 * initial pool size should not prevent the adapter from starting. It should
	 * survive and (potentially) alert its owner in case something goes awry,
	 * but is should stay up. As an example, the cloud provider API may
	 * temporarily be down, so it may be worth while for the adapter to keep on
	 * trying.
	 * 
	 * @throws CloudAdapterException
	 */
	@Test
	public void testFailureToDetermineInitialPoolSize()
			throws CloudAdapterException {
		// set up scaling group mock to raise an exception when adapter tries to
		// determine the initial pool size
		when(this.scalingGroupMock.listMachines()).thenThrow(
				new ScalingGroupException("temporary cloud API outage"));

		String configFile = "config/valid-cloudadapter-config-minimal.json";
		JsonObject validConfig = JsonUtils.parseJsonResource(configFile);
		this.cloudAdapter.configure(validConfig);

		assertTrue(this.cloudAdapter.isStarted());
	}

	@Test(expected = CloudAdapterException.class)
	public void testConfigureWithEmptyConfig() throws CloudAdapterException {
		JsonObject emptyConfig = new JsonObject();
		this.cloudAdapter.configure(emptyConfig);
	}

	@Test(expected = CloudAdapterException.class)
	public void testConfigureWithIllegalConfig() throws CloudAdapterException {
		JsonObject illegalConfig = JsonUtils
				.parseJsonResource("config/invalid-cloudadapter-config-missing-scalinggroup.json");
		this.cloudAdapter.configure(illegalConfig);
	}
}
