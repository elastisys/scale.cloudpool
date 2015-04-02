package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;

import java.util.Collections;
import java.util.List;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to send
 * {@link Alert} messages.
 *
 * @see BaseCloudPoolConfig
 */
public class AlertsConfig {

	/** A list of configured SMTP email {@link Alerter}s. */
	@SerializedName("smtp")
	private final List<SmtpAlerterConfig> smtpAlerters;

	/** A list of HTTP(S) webhook {@link Alerter}s. */
	@SerializedName("http")
	private final List<HttpAlerterConfig> httpAlerters;

	/**
	 * Constructs a new {@link AlertsConfig} instance.
	 *
	 * @param smtpAlerters
	 *            A list of configured SMTP email {@link Alerter}s. A
	 *            <code>null</code> value is equivalent to an empty list.
	 * @param httpAlerters
	 *            A list of HTTP(S) webhook {@link Alerter}s. A
	 *            <code>null</code> value is equivalent to an empty list.
	 */
	public AlertsConfig(List<SmtpAlerterConfig> smtpAlerters,
			List<HttpAlerterConfig> httpAlerters) {
		this.smtpAlerters = smtpAlerters;
		this.httpAlerters = httpAlerters;
	}

	/**
	 * Returns the configured SMTP email {@link Alerter}s.
	 *
	 * @return
	 */
	public List<SmtpAlerterConfig> getSmtpAlerters() {
		if (this.smtpAlerters == null) {
			return Collections.emptyList();
		}
		return this.smtpAlerters;
	}

	/**
	 * Returns the configured HTTP(S) webhook {@link Alerter}s.
	 *
	 * @return
	 */
	public List<HttpAlerterConfig> getHttpAlerters() {
		if (this.httpAlerters == null) {
			return Collections.emptyList();
		}
		return this.httpAlerters;
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		try {
			for (SmtpAlerterConfig smtpAlerterConfig : getSmtpAlerters()) {
				smtpAlerterConfig.validate();
			}
			for (HttpAlerterConfig httpAlerterConfig : getHttpAlerters()) {
				httpAlerterConfig.validate();
			}
		} catch (Exception e) {
			throw new CloudPoolException(format(
					"failed to validate alerts config: %s", e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.getSmtpAlerters(), this.getHttpAlerters());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AlertsConfig) {
			AlertsConfig that = (AlertsConfig) obj;
			return equal(this.getSmtpAlerters(), that.getSmtpAlerters())
					&& equal(this.getHttpAlerters(), that.getHttpAlerters());
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
	}
}