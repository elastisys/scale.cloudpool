package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.gson.JsonObject;

/**
 * Exercise {@link BaseCloudPoolConfig}.
 */
public class TestBaseCloudPoolConfig {

	@Test
	public void basicSanity() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings(), poolFetch(),
				poolUpdate());
		config.validate();
		assertThat(config.getCloudPool(), is(cloudPoolConfig()));
		assertThat(config.getScaleOutConfig(), is(scaleOutConfig()));
		assertThat(config.getScaleInConfig(), is(scaleInConfig()));
		assertThat(config.getAlerts(), is(alertSettings()));
		assertThat(config.getPoolFetch(), is(poolFetch()));
		assertThat(config.getPoolUpdate(), is(poolUpdate()));
	}

	/**
	 * It is okay to not specify alerts.
	 */
	@Test
	public void missingAlertSettings() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), null, poolFetch(),
				poolUpdate());
		config.validate();
		assertThat(config.getAlerts(), is(nullValue()));
	}

	/**
	 * It is okay to not specify poolFetch.
	 */
	@Test
	public void missingPoolFetch() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings(), null,
				poolUpdate());
		config.validate();
		assertThat(config.getPoolFetch(),
				is(BaseCloudPoolConfig.DEFAULT_POOL_FETCH_CONFIG));
	}

	/**
	 * It is okay to not specify poolUpdate.
	 */
	@Test
	public void missingPoolUpdate() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings(), poolFetch(),
				null);
		config.validate();
		assertThat(config.getPoolUpdate(),
				is(BaseCloudPoolConfig.DEFAULT_POOL_UPDATE_CONFIG));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingCloudPool() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(null,
				scaleOutConfig(), scaleInConfig(), alertSettings(), poolFetch(),
				poolUpdate());
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingScaleOutConfig() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				null, scaleInConfig(), alertSettings(), poolFetch(),
				poolUpdate());
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingScaleInConfig() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), null, alertSettings(), poolFetch(),
				poolUpdate());
		config.validate();
	}

	private CloudPoolConfig cloudPoolConfig() {
		return new CloudPoolConfig("MyScalingGroup", cloudCredentialsConfig());
	}

	private JsonObject cloudCredentialsConfig() {
		return JsonUtils.parseJsonString(
				"{\"userName\": \"johndoe\", " + "\"region\": \"us-east-1\"}")
				.getAsJsonObject();
	}

	private ScaleOutConfig scaleOutConfig() {
		String encodedUserData = Base64Utils.toBase64("#!/bin/bash",
				"sudo apt-get update -qy", "sudo apt-get install -qy apache2");
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("web"), encodedUserData);
		return scaleUpConfig;
	}

	private ScaleInConfig scaleInConfig() {
		return new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				500);
	}

	private AlertsConfig alertSettings() {
		return new AlertsConfig(Arrays.asList(smtpAlerter()),
				Arrays.asList(httpAlerter()));
	}

	private SmtpAlerterConfig smtpAlerter() {
		return new SmtpAlerterConfig(Arrays.asList("recipient@org.com"),
				"sender@elastisys.com", "subject", "INFO|ERROR",
				smtpClientConfig());
	}

	private SmtpClientConfig smtpClientConfig() {
		return new SmtpClientConfig("some.mail.host", 25, smtpAuth());
	}

	private SmtpClientAuthentication smtpAuth() {
		return new SmtpClientAuthentication("userName", "password");
	}

	private HttpAlerterConfig httpAlerter() {
		List<String> urls = Arrays.asList("https://some.host/");
		String severityFilter = "INFO|WARN|ERROR";
		HttpAuthConfig auth = new HttpAuthConfig(
				new BasicCredentials("user", "secret"), null);
		int connectionTimeout = 1000;
		int socketTimeout = 1000;

		return new HttpAlerterConfig(urls, severityFilter, auth,
				connectionTimeout, socketTimeout);
	}

	private PoolFetchConfig poolFetch() {
		RetriesConfig retriesConfig = new RetriesConfig(3,
				new TimeInterval(2L, TimeUnit.SECONDS));
		TimeInterval refreshInterval = new TimeInterval(20L, TimeUnit.SECONDS);
		TimeInterval reachabilityTimeout = new TimeInterval(2L,
				TimeUnit.MINUTES);
		return new PoolFetchConfig(retriesConfig, refreshInterval,
				reachabilityTimeout);
	}

	private PoolUpdateConfig poolUpdate() {
		return new PoolUpdateConfig(new TimeInterval(1L, TimeUnit.MINUTES));
	}

}
