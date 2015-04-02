package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.google.gson.JsonObject;

/**
 * Exercise {@link BaseCloudPoolConfig}.
 */
public class TestBaseCloudPoolConfig {

	@Test
	public void basicSanity() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings(),
				poolUpdatePeriod());
		config.validate();
		assertThat(config.getCloudPool(), is(cloudPoolConfig()));
		assertThat(config.getScaleOutConfig(), is(scaleOutConfig()));
		assertThat(config.getScaleInConfig(), is(scaleInConfig()));
		assertThat(config.getAlerts(), is(alertSettings()));
		assertThat(config.getPoolUpdatePeriod(), is(poolUpdatePeriod()));
	}

	/**
	 * It is okay to not specify alerts.
	 */
	@Test
	public void missingAlertSettings() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), null, poolUpdatePeriod());
		config.validate();
		assertThat(config.getAlerts(), is(nullValue()));
	}

	/**
	 * It is okay to not specify a poolUpdatePeriod.
	 */
	@Test
	public void missingPoolUpdatePeriod() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), scaleInConfig(), alertSettings(), null);
		config.validate();
		assertThat(config.getPoolUpdatePeriod(),
				is(BaseCloudPoolConfig.DEFAULT_POOL_UPDATE_PERIOD));
	}

	@Test(expected = CloudPoolException.class)
	public void missingCloudPool() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(null,
				scaleOutConfig(), scaleInConfig(), alertSettings(),
				poolUpdatePeriod());
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingScaleOutConfig() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				null, scaleInConfig(), alertSettings(), poolUpdatePeriod());
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingScaleInConfig() {
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig(),
				scaleOutConfig(), null, alertSettings(), poolUpdatePeriod());
		config.validate();
	}

	private CloudPoolConfig cloudPoolConfig() {
		return new CloudPoolConfig("MyScalingGroup", cloudCredentialsConfig());
	}

	private JsonObject cloudCredentialsConfig() {
		return JsonUtils.parseJsonString("{\"userName\": \"johndoe\", "
				+ "\"region\": \"us-east-1\"}");
	}

	private ScaleOutConfig scaleOutConfig() {
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get install apache2"));
		return scaleUpConfig;
	}

	private ScaleInConfig scaleInConfig() {
		return new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 500);
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
		HttpAuthConfig auth = new HttpAuthConfig(new BasicCredentials("user",
				"secret"), null);
		int connectionTimeout = 1000;
		int socketTimeout = 1000;

		return new HttpAlerterConfig(urls, severityFilter, auth,
				connectionTimeout, socketTimeout);
	}

	private int poolUpdatePeriod() {
		return 15;
	}
}
