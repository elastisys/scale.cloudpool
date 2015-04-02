package com.elastisys.scale.cloudpool.commons.basepool.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Exercise {@link AlertsConfig}.
 */
public class TestAlertsConfig {

	/**
	 * Make sure arguments are stored correctly.
	 */
	@Test
	public void basicSanity() {
		SmtpAlerterConfig emailAlerter1 = smtpAlerterConfig(
				"user1@elastisys.com", "ERROR");
		SmtpAlerterConfig emailAlerter2 = smtpAlerterConfig(
				"user2@elastisys.com", "FATAL");
		HttpAlerterConfig httpAlerter1 = new HttpAlerterConfig(
				Arrays.asList("http://host1/"), "INFO|WARN", null);
		HttpAlerterConfig httpAlerter2 = new HttpAlerterConfig(
				Arrays.asList("https://host2/"), "ERROR", new HttpAuthConfig(
						new BasicCredentials("user", "pass"), null));

		AlertsConfig config = new AlertsConfig(asList(emailAlerter1,
				emailAlerter2), asList(httpAlerter1, httpAlerter2));
		config.validate();
		assertThat(config.getSmtpAlerters(),
				is(asList(emailAlerter1, emailAlerter2)));
		assertThat(config.getHttpAlerters(),
				is(asList(httpAlerter1, httpAlerter2)));
	}

	/**
	 * <code>null</code> arguments should be allowed and should default to empty
	 * list.
	 */
	@Test
	public void createWithNulls() {
		AlertsConfig config = new AlertsConfig(null, null);
		config.validate();
		List<HttpAlerterConfig> emptyHttpAlerters = Collections.emptyList();
		List<SmtpAlerterConfig> emptySmtpAlerters = Collections.emptyList();
		assertThat(config.getHttpAlerters(), is(emptyHttpAlerters));
		assertThat(config.getSmtpAlerters(), is(emptySmtpAlerters));
	}

	/**
	 * Make sure that {@link AlertsConfig#validate()} calls validate on
	 * configured alerters.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void validation() {
		SmtpAlerterConfig emailAlerter1 = smtpAlerterConfig(
				"user1@elastisys.com", "ERROR");
		// bad URL: should not pass validation
		HttpAlerterConfig httpAlerter1 = new HttpAlerterConfig(
				Arrays.asList("tcp://host1/"), "INFO|WARN", null);

		new AlertsConfig(Arrays.asList(emailAlerter1),
				Arrays.asList(httpAlerter1)).validate();
	}

	private SmtpAlerterConfig smtpAlerterConfig(String recipient,
			String severityFilter) {
		return new SmtpAlerterConfig(Arrays.asList(recipient),
				"sender@elastisys.com", "subject", severityFilter,
				smtpClientConfig());
	}

	private SmtpClientConfig smtpClientConfig() {
		return new SmtpClientConfig("some.mail.host", 25, null);
	}

}
