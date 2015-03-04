package com.elastisys.scale.cloudpool.commons.basepool;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.MailServerSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.smtp.ClientAuthentication;
import com.google.gson.JsonObject;

/**
 * Tests validation of {@link BaseCloudPoolConfig}s.
 */
public class TestBaseCloudPoolConfigValidation {

	@Test
	public void minimalConfig() throws CloudPoolException {
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), null, null).validate();
	}

	@Test
	public void withAlertConfig() throws CloudPoolException {
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alertConfig(), null).validate();
	}

	@Test
	public void withAlertConfigWithDefaultSeverityFilter()
			throws CloudPoolException {
		// no explicity severity filter => default severity filter
		String defaultSeverity = null;
		AlertSettings alertSettings = new AlertSettings("subject",
				Arrays.asList("recipient@dest.com"), "sender@source.com",
				defaultSeverity, mailServer());

		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings, null);
		assertThat(config.getAlerts().getSeverityFilter(),
				is(AlertSettings.DEFAULT_SEVERITY_FILTER));
		config.validate();
	}

	// illegal config: invalid severity filter
	@Test(expected = CloudPoolException.class)
	public void withIllegalAlertsSeverityFilter() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts, "severityFilter", "**");
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /cloudPool
	@Test(expected = CloudPoolException.class)
	public void missingCloudPool() throws CloudPoolException {
		new BaseCloudPoolConfig(null, scaleOutConfig(), scaleInConfig(),
				alertConfig(), null).validate();
	}

	// illegal config: missing /cloudPool/name
	@Test(expected = CloudPoolException.class)
	public void missingCloudPoolName() throws CloudPoolException {
		CloudPoolConfig cloudPoolConfig = cloudPoolConfig();
		setPrivateField(cloudPoolConfig, "name", null);
		new BaseCloudPoolConfig(cloudPoolConfig, scaleOutConfig(),
				scaleInConfig(), null, null).validate();
	}

	// illegal config: missing /cloudPool/driverConfig
	@Test(expected = CloudPoolException.class)
	public void missingCloudPoolDriverConfig() throws CloudPoolException {
		CloudPoolConfig cloudPoolConfig = cloudPoolConfig();
		setPrivateField(cloudPoolConfig, "driverConfig", null);
		new BaseCloudPoolConfig(cloudPoolConfig, scaleOutConfig(),
				scaleInConfig(), null, null).validate();
	}

	// illegal config: missing /scaleOutConfig
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfig() throws CloudPoolException {
		new BaseCloudPoolConfig(cloudPoolConfig(), null, scaleInConfig(),
				alertConfig(), null).validate();
	}

	// illegal config: missing /scaleOutConfig/size
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfigSize() throws CloudPoolException {
		ScaleOutConfig scaleOutConfig = scaleOutConfig();
		setPrivateField(scaleOutConfig, "size", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig,
				scaleInConfig(), alertConfig(), null).validate();
	}

	// illegal config: missing /scaleOutConfig/image
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfigImage() throws CloudPoolException {
		ScaleOutConfig scaleOutConfig = scaleOutConfig();
		setPrivateField(scaleOutConfig, "image", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig,
				scaleInConfig(), alertConfig(), null).validate();
	}

	// illegal config: missing /scaleOutConfig/keyPair
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfigKeyPair() throws CloudPoolException {
		ScaleOutConfig scaleOutConfig = scaleOutConfig();
		setPrivateField(scaleOutConfig, "keyPair", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig,
				scaleInConfig(), alertConfig(), null).validate();
	}

	// illegal config: missing /scaleOutConfig/securityGroups
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfigSecurityGroups() throws CloudPoolException {
		ScaleOutConfig scaleOutConfig = scaleOutConfig();
		setPrivateField(scaleOutConfig, "securityGroups", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig,
				scaleInConfig(), alertConfig(), null).validate();
	}

	// illegal config: missing /scaleOutConfig/bootScript
	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfigBootScript() throws CloudPoolException {
		ScaleOutConfig scaleOutConfig = scaleOutConfig();
		setPrivateField(scaleOutConfig, "bootScript", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig,
				scaleInConfig(), alertConfig(), null).validate();
	}

	// illegal config: missing /alerts/subject
	@Test(expected = CloudPoolException.class)
	public void missingAlertsSubject() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts, "subject", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/recipients
	@Test(expected = CloudPoolException.class)
	public void missingAlertsRecipients() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts, "recipients", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/sender
	@Test(expected = CloudPoolException.class)
	public void missingAlertsSender() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts, "sender", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/mailServer
	@Test(expected = CloudPoolException.class)
	public void missingAlertsMailServer() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts, "mailServer", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/mailServer/smtpHost
	@Test(expected = CloudPoolException.class)
	public void missingAlertsMailServerSmtpHost() throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		setPrivateField(alerts.getMailServer(), "smtpHost", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/mailServer/authentication/userName
	@Test(expected = CloudPoolException.class)
	public void missingAlertsMailServerAuthenticationUsername()
			throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		ClientAuthentication authentication = alerts.getMailServer()
				.getAuthentication();
		setPrivateField(authentication, "userName", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	// illegal config: missing /alerts/mailServer/authentication/password
	@Test(expected = CloudPoolException.class)
	public void missingAlertsMailServerAuthenticationPassword()
			throws CloudPoolException {
		AlertSettings alerts = alertConfig();
		ClientAuthentication authentication = alerts.getMailServer()
				.getAuthentication();
		setPrivateField(authentication, "password", null);
		new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(),
				scaleInConfig(), alerts, null).validate();
	}

	private AlertSettings alertConfig() {
		return new AlertSettings("subject",
				Arrays.asList("recipient@dest.com"), "sender@source.com",
				"ERROR|FATAL", mailServer());
	}

	private MailServerSettings mailServer() {
		return new MailServerSettings("smtpHost", 587, smtpAuth(), true);
	}

	private ClientAuthentication smtpAuth() {
		return new ClientAuthentication("userName", "password");
	}

	private CloudPoolConfig cloudPoolConfig() {
		return new CloudPoolConfig("MyScalingGroup", cloudCredentialsConfig());
	}

	private ScaleOutConfig scaleOutConfig() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"apt-get update -qy && apt-get isntall apache2 -qy");
		return new ScaleOutConfig("size", "image", "keyPair",
				Arrays.asList("securityGroup"), bootScript);
	}

	private ScaleInConfig scaleInConfig() {
		return new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
	}

	private JsonObject cloudCredentialsConfig() {
		return JsonUtils.parseJsonString("{\"userName\": \"johndoe\", "
				+ "\"region\": \"us-east-1\"}");
	}

	/**
	 * Sets a private field on an instance object.
	 *
	 * @param object
	 *            The object.
	 * @param privateFieldName
	 *            The name of the private field to set.
	 * @param valueToSet
	 *            The value to set.
	 * @throws Exception
	 */
	private void setPrivateField(Object object, String privateFieldName,
			Object valueToSet) throws RuntimeException {
		try {
			Field field = object.getClass().getDeclaredField(privateFieldName);
			field.setAccessible(true);
			field.set(object, valueToSet);
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"could not set private field '%s' on object: %s",
					privateFieldName, e.getMessage()));
		}
	}
}
