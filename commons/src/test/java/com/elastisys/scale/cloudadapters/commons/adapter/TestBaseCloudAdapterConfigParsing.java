package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.DEFAULT_POOL_UPDATE_PERIOD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.BootTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.RunTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing a JSON-formatted configuration into its Java
 * counterpart ({@link BaseCloudAdapterConfig}) works as expected.
 * 
 */
public class TestBaseCloudAdapterConfigParsing {

	/**
	 * Verifies parsing of a configuration without all optional elements.
	 * 
	 * @throws CloudAdapterException
	 */
	@Test
	public void parseCompleteConfig() throws CloudAdapterException {
		String jsonConf = "config/valid-cloudadapter-config-complete.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		// verify scalingGroup config
		assertThat(config.getScalingGroup().getName(),
				is(gets(jsonConf, "scalingGroup/name")));
		assertThat(config.getScalingGroup().getConfig(),
				is(get(jsonConf, "scalingGroup/config")));

		// verify scaleUpConfig
		assertThat(config.getScaleUpConfig().getSize(),
				is(gets(jsonConf, "scaleUpConfig/size")));
		assertThat(config.getScaleUpConfig().getImage(),
				is(gets(jsonConf, "scaleUpConfig/image")));
		assertThat(config.getScaleUpConfig().getKeyPair(),
				is(gets(jsonConf, "scaleUpConfig/keyPair")));
		assertThat(config.getScaleUpConfig().getSecurityGroups(),
				is(geta(jsonConf, "scaleUpConfig/securityGroups")));
		assertThat(config.getScaleUpConfig().getBootScript(),
				is(geta(jsonConf, "scaleUpConfig/bootScript")));

		// verify scaleDownConfig
		assertThat(config.getScaleDownConfig().getVictimSelectionPolicy()
				.name(),
				is(gets(jsonConf, "scaleDownConfig/victimSelectionPolicy")));
		assertThat(config.getScaleDownConfig().getInstanceHourMargin(),
				is(geti(jsonConf, "scaleDownConfig/instanceHourMargin")));

		// verify liveness config
		assertThat(config.getLiveness().getLoginUser(),
				is(gets(jsonConf, "liveness/loginUser")));
		assertThat(config.getLiveness().getLoginKey(),
				is(gets(jsonConf, "liveness/loginKey")));
		// verify that boot-time liveness check config was correctly parsed
		BootTimeLivenessCheck bootTimeCheck = config.getLiveness()
				.getBootTimeCheck();
		assertThat(bootTimeCheck.getCommand(),
				is(gets(jsonConf, "liveness/bootTimeCheck/command")));
		assertThat(bootTimeCheck.getMaxRetries(),
				is(geti(jsonConf, "liveness/bootTimeCheck/maxRetries")));
		assertThat(bootTimeCheck.getRetryDelay(),
				is(geti(jsonConf, "liveness/bootTimeCheck/retryDelay")));
		// verify that run-time liveness check config was correctly parsed
		RunTimeLivenessCheck runTimeCheck = config.getLiveness()
				.getRunTimeCheck();
		assertThat(runTimeCheck.getPeriod(),
				is(geti(jsonConf, "liveness/runTimeCheck/period")));
		assertThat(runTimeCheck.getCommand(),
				is(gets(jsonConf, "liveness/runTimeCheck/command")));
		assertThat(runTimeCheck.getMaxRetries(),
				is(geti(jsonConf, "liveness/runTimeCheck/maxRetries")));
		assertThat(runTimeCheck.getRetryDelay(),
				is(geti(jsonConf, "liveness/runTimeCheck/retryDelay")));

		// verify alert settings were configured
		AlertSettings alerts = config.getAlerts();
		assertThat(alerts.getRecipients().size(), is(1));
		assertThat(alerts.getRecipients(),
				is(geta(jsonConf, "alerts/recipients")));
		assertThat(alerts.getSender(), is(gets(jsonConf, "alerts/sender")));
		assertThat(alerts.getMailServer().getSmtpHost(),
				is(gets(jsonConf, "alerts/mailServer/smtpHost")));
		assertThat(alerts.getMailServer().getSmtpPort(),
				is(geti(jsonConf, "alerts/mailServer/smtpPort")));
		assertThat(alerts.getMailServer().getAuthentication().getUserName(),
				is(gets(jsonConf, "alerts/mailServer/authentication/userName")));
		assertThat(alerts.getMailServer().getAuthentication().getPassword(),
				is(gets(jsonConf, "alerts/mailServer/authentication/password")));

		// verify that poolUpdatePeriod is given default value
		assertThat(config.getPoolUpdatePeriod(), is(180));

		config.validate();
	}

	/**
	 * Verifies parsing of a configuration without all optional elements.
	 * 
	 * @throws CloudAdapterException
	 */
	@Test
	public void parseMinimalConfig() throws CloudAdapterException {
		String jsonConf = "config/valid-cloudadapter-config-minimal.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		// verify scalingGroup config
		assertThat(config.getScalingGroup().getName(),
				is(gets(jsonConf, "scalingGroup/name")));
		assertThat(config.getScalingGroup().getConfig(),
				is(get(jsonConf, "scalingGroup/config")));

		// verify scaleUpConfig
		assertThat(config.getScaleUpConfig().getSize(),
				is(gets(jsonConf, "scaleUpConfig/size")));
		assertThat(config.getScaleUpConfig().getImage(),
				is(gets(jsonConf, "scaleUpConfig/image")));
		assertThat(config.getScaleUpConfig().getKeyPair(),
				is(gets(jsonConf, "scaleUpConfig/keyPair")));
		assertThat(config.getScaleUpConfig().getSecurityGroups(),
				is(geta(jsonConf, "scaleUpConfig/securityGroups")));
		assertThat(config.getScaleUpConfig().getBootScript(),
				is(geta(jsonConf, "scaleUpConfig/bootScript")));

		// verify scaleDownConfig
		assertThat(config.getScaleDownConfig().getVictimSelectionPolicy()
				.name(),
				is(gets(jsonConf, "scaleDownConfig/victimSelectionPolicy")));
		assertThat(config.getScaleDownConfig().getInstanceHourMargin(),
				is(geti(jsonConf, "scaleDownConfig/instanceHourMargin")));

		// verify that no liveness config were set
		assertThat(config.getLiveness(), is(nullValue()));

		// verify that no alert settings were configured
		assertThat(config.getAlerts(), is(nullValue()));

		// verify poolUpdatePeriod config
		assertThat(config.getPoolUpdatePeriod(), is(DEFAULT_POOL_UPDATE_PERIOD));

		config.validate();
	}

	/**
	 * Verifies that the {@code config} element in the
	 * {@link ScalingGroupConfig} is correctly represented.
	 */
	@Test
	public void verifyScalingGroupConfigParsing() throws CloudAdapterException {
		String jsonConf = "config/valid-cloudadapter-config-minimal.json";
		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);
		// verify CloudClient login config
		JsonObject scalingGroupConfig = config.getScalingGroup().getConfig();
		assertThat(scalingGroupConfig.get("keystoneEndpoint").getAsString(),
				is("http://openstack.nova.com:5000/v2.0"));
		assertThat(scalingGroupConfig.get("region").getAsString(),
				is("RegionOne"));
		assertThat(scalingGroupConfig.get("tenantName").getAsString(),
				is("tenant"));
		assertThat(scalingGroupConfig.get("userName").getAsString(),
				is("clouduser"));
		assertThat(scalingGroupConfig.get("password").getAsString(),
				is("cloudpass"));

		config.validate();
	}

	/**
	 * Verifies parsing of a config with non-default poolUpdatePeriod.
	 * 
	 * @throws CloudAdapterException
	 */
	@Test
	public void verifyPoolUpdatePeriodParsing() throws CloudAdapterException {
		String jsonConf = "config/valid-cloudadapter-config-with-poolupdateperiod.json";
		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		assertThat(config.getPoolUpdatePeriod(), is(180));

		config.validate();
	}

	/**
	 * Verifies that a JSON configuration with the (optional)
	 * {@link AlertSettings} is correctly parsed into its Java counterpart.
	 */
	@Test
	public void parseJsonConfigWithAlerts() throws Exception {
		String jsonConf = "config/valid-cloudadapter-config-with-alerts.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		// verify that alert settings were identified
		AlertSettings alerts = config.getAlerts();
		assertThat(alerts, is(not(nullValue())));
		assertThat(alerts.getRecipients().size(), is(1));
		assertThat(alerts.getRecipients().get(0),
				is(get(jsonConf, "alerts/recipients").getAsJsonArray().get(0)
						.getAsString()));
		assertThat(alerts.getSender(), is(gets(jsonConf, "alerts/sender")));
		assertThat(alerts.getMailServer().getSmtpHost(),
				is(gets(jsonConf, "alerts/mailServer/smtpHost")));
		assertThat(alerts.getMailServer().getSmtpPort(),
				is(geti(jsonConf, "alerts/mailServer/smtpPort")));
		assertThat(alerts.getMailServer().getAuthentication().getUserName(),
				is(gets(jsonConf, "alerts/mailServer/authentication/userName")));
		assertThat(alerts.getMailServer().getAuthentication().getPassword(),
				is(gets(jsonConf, "alerts/mailServer/authentication/password")));

		config.validate();
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = CloudAdapterException.class)
	public void parseIllegalConfigMissingScaleUpConfig()
			throws CloudAdapterException {
		String jsonConf = "config/invalid-cloudadapter-config-missing-scaleupconfig.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		config.validate();
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = CloudAdapterException.class)
	public void parseIllegalConfigMissingScalingGroup()
			throws CloudAdapterException {
		String jsonConf = "config/invalid-cloudadapter-config-missing-scalinggroup.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		config.validate();
	}

	/**
	 * Validation of configuration missing required element should fail.
	 */
	@Test(expected = CloudAdapterException.class)
	public void parseIllegalConfigMissingScaleDownConfig()
			throws CloudAdapterException {
		String jsonConf = "config/invalid-cloudadapter-config-missing-scaledownconfig.json";

		BaseCloudAdapterConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudAdapterConfig.class);

		config.validate();
	}

	/**
	 * Returns the element at {@code path} as a {@link String}.
	 * 
	 * @param jsonConf
	 * @param path
	 * @return
	 */
	private String gets(String jsonConf, String path) {
		return get(jsonConf, path).getAsString();
	}

	/**
	 * Returns the element at {@code path} as an integer.
	 * 
	 * @param jsonConf
	 * @param path
	 * @return
	 */
	private int geti(String jsonConf, String path) {
		return get(jsonConf, path).getAsInt();
	}

	/**
	 * Returns the array element at {@code path} as a {@link List} of string.
	 * 
	 * @param jsonConf
	 * @param path
	 * @return
	 */
	private List<String> geta(String jsonConf, String path) {
		List<String> list = Lists.newArrayList();
		JsonArray jsonArray = get(jsonConf, path).getAsJsonArray();
		for (int i = 0; i < jsonArray.size(); i++) {
			list.add(jsonArray.get(i).getAsString());
		}
		return list;
	}

	/**
	 * Returns the element at {@code path}.
	 * 
	 * @param configResource
	 * @param path
	 * @return
	 */
	private JsonElement get(String configResource, String path) {
		String[] keys = path.split("/");
		JsonElement cursor = JsonUtils.parseJsonResource(configResource);
		for (String key : keys) {
			if (!cursor.isJsonObject()) {
				throw new IllegalArgumentException(cursor.getAsString()
						+ " is not a json object and "
						+ "therefore has no child element " + key);
			}
			JsonObject cursorObject = cursor.getAsJsonObject();
			if (!cursorObject.has(key)) {
				throw new IllegalArgumentException("path " + path
						+ " ended: no child element " + key);
			}
			cursor = cursorObject.get(key);
		}
		return cursor;
	}

}
