package com.elastisys.scale.cloudpool.commons.basepool.alerts;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.basepool.config.AlertsConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.TimeInterval;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.filtering.FilteringAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerter;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;

/**
 * Exercise the {@link AlertHandler} class.
 */
public class TestAlertHandler {

	private EventBus mockEventBus = mock(EventBus.class);

	/** Object under test. */
	private AlertHandler alertHandler;

	@Before
	public void beforeTestMethod() {
		this.alertHandler = new AlertHandler(this.mockEventBus);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithoutEventbus() {
		new AlertHandler(null);
	}

	/**
	 * <code>null</code> values are interpreted as "none".
	 */
	@Test
	public void registerNullAlertConfig() {
		this.alertHandler.registerAlerters(null, null);
		assertThat(this.alertHandler.alerters(), is(alerters()));
	}

	@Test
	public void registerEmptyAlerterConfig() {
		this.alertHandler.registerAlerters(alertConfig(null, null, null), null);
		assertThat(this.alertHandler.alerters(), is(alerters()));
	}

	@Test
	public void registerHttpAlerterConfigWithoutStandardTags() {
		List<SmtpAlerterConfig> smtpAlerters = null;
		HttpAlerterConfig http1Config = httpConfig("http://hook", ".*");
		List<HttpAlerterConfig> httpAlerters = Arrays.asList(http1Config);
		Map<String, JsonElement> standardTags = ImmutableMap.of();

		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);

		this.alertHandler.registerAlerters(alertConfig, standardTags);
		assertThat(this.alertHandler.alerters(),
				is(alerters(filtered(http(http1Config, standardTags),
						alertConfig.getDuplicateSuppression()))));
	}

	@Test
	public void registerHttpAlerterConfigWithStandardTags() {
		List<SmtpAlerterConfig> smtpAlerters = null;
		HttpAlerterConfig http1Config = httpConfig("http://hook", ".*");
		List<HttpAlerterConfig> httpAlerters = Arrays.asList(http1Config);
		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);

		this.alertHandler.registerAlerters(alertConfig, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(filtered(http(http1Config, standardTags()),
						alertConfig.getDuplicateSuppression()))));
	}

	@Test
	public void registerSmtpAlerterConfigWithoutStandardTags() {
		SmtpAlerterConfig smtp1Conf = smtpConfig("john@doe.com", ".*");
		List<SmtpAlerterConfig> smtpAlerters = Arrays.asList(smtp1Conf);
		List<HttpAlerterConfig> httpAlerters = null;
		Map<String, JsonElement> standardTags = ImmutableMap.of();
		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);

		this.alertHandler.registerAlerters(alertConfig, standardTags);
		assertThat(this.alertHandler.alerters(),
				is(alerters(filtered(smtp(smtp1Conf, standardTags),
						alertConfig.getDuplicateSuppression()))));
	}

	@Test
	public void registerSmtpAlerterConfigWithStandardTags() {
		SmtpAlerterConfig smtp1Conf = smtpConfig("john@doe.com", ".*");
		List<SmtpAlerterConfig> smtpAlerters = Arrays.asList(smtp1Conf);
		List<HttpAlerterConfig> httpAlerters = null;
		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);

		this.alertHandler.registerAlerters(alertConfig, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(filtered(smtp(smtp1Conf, standardTags()),
						alertConfig.getDuplicateSuppression()))));
	}

	@Test
	public void registerHttpAndSmtpAlerterConfig() {
		SmtpAlerterConfig smtp1Conf = smtpConfig("john@doe.com", ".*");
		List<SmtpAlerterConfig> smtpAlerters = Arrays.asList(smtp1Conf);
		HttpAlerterConfig http1Conf = httpConfig("http://hook", ".*");
		List<HttpAlerterConfig> httpAlerters = Arrays.asList(http1Conf);
		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);

		this.alertHandler.registerAlerters(alertConfig, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(
						filtered(smtp(smtp1Conf, standardTags()),
								alertConfig.getDuplicateSuppression()),
						filtered(http(http1Conf, standardTags()),
								alertConfig.getDuplicateSuppression()))));
	}

	/**
	 * Should be possible to call register several times to add more
	 * {@link Alerter}s.
	 */
	@Test
	public void registerAdditionalAlerters() {
		// register an additional smtp alerter
		SmtpAlerterConfig smtp1Conf = smtpConfig("john@doe.com", ".*");
		List<SmtpAlerterConfig> smtpAlerters = Arrays.asList(smtp1Conf);
		AlertsConfig alert1Config = alertConfig(smtpAlerters, null,
				new TimeInterval(10L, TimeUnit.MINUTES));
		this.alertHandler.registerAlerters(alert1Config, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(filtered(smtp(smtp1Conf, standardTags()),
						alert1Config.getDuplicateSuppression()))));

		// register an additional http alerter
		HttpAlerterConfig http1Conf = httpConfig("http://hook", ".*");
		List<HttpAlerterConfig> httpAlerters = Arrays.asList(http1Conf);
		AlertsConfig alert2Config = alertConfig(null, httpAlerters,
				new TimeInterval(18L, TimeUnit.MINUTES));
		this.alertHandler.registerAlerters(alert2Config, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(
						filtered(smtp(smtp1Conf, standardTags()),
								alert1Config.getDuplicateSuppression()),
						filtered(http(http1Conf, standardTags()),
								alert2Config.getDuplicateSuppression()))));

	}

	@Test
	public void unregister() {
		// register
		SmtpAlerterConfig smtp1Conf = smtpConfig("john@doe.com", ".*");
		List<SmtpAlerterConfig> smtpAlerters = Arrays.asList(smtp1Conf);
		HttpAlerterConfig http1Conf = httpConfig("http://hook", ".*");
		List<HttpAlerterConfig> httpAlerters = Arrays.asList(http1Conf);
		AlertsConfig alertConfig = alertConfig(smtpAlerters, httpAlerters,
				null);
		this.alertHandler.registerAlerters(alertConfig, standardTags());
		assertThat(this.alertHandler.alerters(),
				is(alerters(
						filtered(smtp(smtp1Conf, standardTags()),
								alertConfig.getDuplicateSuppression()),
						filtered(http(http1Conf, standardTags()),
								alertConfig.getDuplicateSuppression()))));

		// unregister
		this.alertHandler.unregisterAlerters();
		assertThat(this.alertHandler.alerters(), is(alerters()));
	}

	private Map<String, JsonElement> standardTags() {
		return ImmutableMap.of("ip", JsonUtils.toJson("1.2.3.4"), //
				"poolName", JsonUtils.toJson("cloudpool"));

	}

	private List<Alerter> alerters(Alerter... alerters) {
		if (alerters == null) {
			return ImmutableList.of();
		}
		return ImmutableList.copyOf(alerters);
	}

	private AlertsConfig alertConfig(List<SmtpAlerterConfig> emailAlerters,
			List<HttpAlerterConfig> httpAlerters,
			TimeInterval duplicateSuppression) {
		return new AlertsConfig(emailAlerters, httpAlerters,
				duplicateSuppression);
	}

	private Alerter filtered(Alerter alerter,
			TimeInterval duplicateSuppression) {
		return new FilteringAlerter(alerter, duplicateSuppression.getTime(),
				duplicateSuppression.getUnit());
	}

	private Alerter http(HttpAlerterConfig httpAlerterConfig,
			Map<String, JsonElement> standardTags) {
		return new HttpAlerter(httpAlerterConfig, standardTags);
	}

	private Alerter smtp(SmtpAlerterConfig smtpAlerterConfig,
			Map<String, JsonElement> standardTags) {
		return new SmtpAlerter(smtpAlerterConfig, standardTags);
	}

	private SmtpAlerterConfig smtpConfig(String recipient,
			String severityFilter) {
		return new SmtpAlerterConfig(Arrays.asList(recipient),
				"sender@elastisys.com", "subject", severityFilter,
				smtpClientConfig());
	}

	private SmtpClientConfig smtpClientConfig() {
		return new SmtpClientConfig("some.mail.host", 25, null);
	}

	private HttpAlerterConfig httpConfig(String url, String severityFilter) {
		return new HttpAlerterConfig(Arrays.asList(url), severityFilter,
				new HttpAuthConfig(new BasicCredentials("user", "pass"), null));
	}

}
