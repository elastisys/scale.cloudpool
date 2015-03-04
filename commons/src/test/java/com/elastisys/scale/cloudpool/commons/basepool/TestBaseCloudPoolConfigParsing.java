package com.elastisys.scale.cloudpool.commons.basepool;

import static com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.DEFAULT_POOL_UPDATE_PERIOD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing a JSON-formatted configuration into its Java
 * counterpart ({@link BaseCloudPoolConfig}) works as expected.
 */
public class TestBaseCloudPoolConfigParsing {

	/**
	 * Verifies parsing of a configuration without all optional elements.
	 *
	 * @throws CloudPoolException
	 */
	@Test
	public void parseCompleteConfig() throws CloudPoolException {
		String jsonConf = "config/valid-cloudpool-config-complete.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		// verify cloudPool config
		assertThat(config.getCloudPool().getName(),
				is(gets(jsonConf, "cloudPool/name")));
		assertThat(config.getCloudPool().getDriverConfig(),
				is(get(jsonConf, "cloudPool/driverConfig")));

		// verify scaleOutConfig
		assertThat(config.getScaleOutConfig().getSize(),
				is(gets(jsonConf, "scaleOutConfig/size")));
		assertThat(config.getScaleOutConfig().getImage(),
				is(gets(jsonConf, "scaleOutConfig/image")));
		assertThat(config.getScaleOutConfig().getKeyPair(),
				is(gets(jsonConf, "scaleOutConfig/keyPair")));
		assertThat(config.getScaleOutConfig().getSecurityGroups(),
				is(geta(jsonConf, "scaleOutConfig/securityGroups")));
		assertThat(config.getScaleOutConfig().getBootScript(),
				is(geta(jsonConf, "scaleOutConfig/bootScript")));

		// verify scaleInConfig
		assertThat(config.getScaleInConfig().getVictimSelectionPolicy().name(),
				is(gets(jsonConf, "scaleInConfig/victimSelectionPolicy")));
		assertThat(config.getScaleInConfig().getInstanceHourMargin(),
				is(geti(jsonConf, "scaleInConfig/instanceHourMargin")));

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
	 * @throws CloudPoolException
	 */
	@Test
	public void parseMinimalConfig() throws CloudPoolException {
		String jsonConf = "config/valid-cloudpool-config-minimal.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		// verify cloudPool config
		assertThat(config.getCloudPool().getName(),
				is(gets(jsonConf, "cloudPool/name")));
		assertThat(config.getCloudPool().getDriverConfig(),
				is(get(jsonConf, "cloudPool/driverConfig")));

		// verify scaleOutConfig
		assertThat(config.getScaleOutConfig().getSize(),
				is(gets(jsonConf, "scaleOutConfig/size")));
		assertThat(config.getScaleOutConfig().getImage(),
				is(gets(jsonConf, "scaleOutConfig/image")));
		assertThat(config.getScaleOutConfig().getKeyPair(),
				is(gets(jsonConf, "scaleOutConfig/keyPair")));
		assertThat(config.getScaleOutConfig().getSecurityGroups(),
				is(geta(jsonConf, "scaleOutConfig/securityGroups")));
		assertThat(config.getScaleOutConfig().getBootScript(),
				is(geta(jsonConf, "scaleOutConfig/bootScript")));

		// verify scaleInConfig
		assertThat(config.getScaleInConfig().getVictimSelectionPolicy().name(),
				is(gets(jsonConf, "scaleInConfig/victimSelectionPolicy")));
		assertThat(config.getScaleInConfig().getInstanceHourMargin(),
				is(geti(jsonConf, "scaleInConfig/instanceHourMargin")));

		// verify that no alert settings were configured
		assertThat(config.getAlerts(), is(nullValue()));

		// verify poolUpdatePeriod config
		assertThat(config.getPoolUpdatePeriod(), is(DEFAULT_POOL_UPDATE_PERIOD));

		config.validate();
	}

	/**
	 * Verifies that the {@code config} element in the {@link CloudPoolConfig}
	 * is correctly represented.
	 */
	@Test
	public void verifyScalingGroupConfigParsing() throws CloudPoolException {
		String jsonConf = "config/valid-cloudpool-config-minimal.json";
		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);
		// verify CloudClient login config
		JsonObject driverConfig = config.getCloudPool().getDriverConfig();
		assertThat(driverConfig.get("keystoneEndpoint").getAsString(),
				is("http://openstack.nova.com:5000/v2.0"));
		assertThat(driverConfig.get("region").getAsString(), is("RegionOne"));
		assertThat(driverConfig.get("tenantName").getAsString(), is("tenant"));
		assertThat(driverConfig.get("userName").getAsString(), is("clouduser"));
		assertThat(driverConfig.get("password").getAsString(), is("cloudpass"));

		config.validate();
	}

	/**
	 * Verifies parsing of a config with non-default poolUpdatePeriod.
	 *
	 * @throws CloudPoolException
	 */
	@Test
	public void verifyPoolUpdatePeriodParsing() throws CloudPoolException {
		String jsonConf = "config/valid-cloudpool-config-with-poolupdateperiod.json";
		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

		assertThat(config.getPoolUpdatePeriod(), is(180));

		config.validate();
	}

	/**
	 * Verifies that a JSON configuration with the (optional)
	 * {@link AlertSettings} is correctly parsed into its Java counterpart.
	 */
	@Test
	public void parseJsonConfigWithAlerts() throws Exception {
		String jsonConf = "config/valid-cloudpool-config-with-alerts.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

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
	@Test(expected = CloudPoolException.class)
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
	@Test(expected = CloudPoolException.class)
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
	@Test(expected = CloudPoolException.class)
	public void parseIllegalConfigMissingScaleInConfig()
			throws CloudPoolException {
		String jsonConf = "config/invalid-cloudpool-config-missing-scaleinconfig.json";

		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonResource(jsonConf),
				BaseCloudPoolConfig.class);

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
