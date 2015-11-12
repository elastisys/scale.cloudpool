package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.filtering.FilteringAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.gson.annotations.SerializedName;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to send
 * {@link Alert} messages.
 *
 * @see BaseCloudPoolConfig
 */
public class AlertsConfig {
	/** Default duplicate suppression. */
	public static final TimeInterval DEFAULT_DUPLICATE_SUPPRESSION = new TimeInterval(
			5L, TimeUnit.MINUTES);

	/** A list of configured SMTP email {@link Alerter}s. */
	@SerializedName("smtp")
	private final List<SmtpAlerterConfig> smtpAlerters;

	/** A list of HTTP(S) webhook {@link Alerter}s. */
	@SerializedName("http")
	private final List<HttpAlerterConfig> httpAlerters;

	/**
	 * Duration of time to suppress duplicate {@link Alert}s from being re-sent.
	 * Two {@link Alert}s are considered equal if they share topic, message and
	 * metadata tags (see {@link FilteringAlerter#DEFAULT_IDENTITY_FUNCTION}).
	 */
	private final TimeInterval duplicateSuppression;

	/**
	 * Constructs a new {@link AlertsConfig} instance with default duplicate
	 * suppression.
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
		this(smtpAlerters, httpAlerters, null);
	}

	/**
	 * Constructs a new {@link AlertsConfig} instance.
	 *
	 * @param smtpAlerters
	 *            A list of configured SMTP email {@link Alerter}s. A
	 *            <code>null</code> value is equivalent to an empty list.
	 * @param httpAlerters
	 *            A list of HTTP(S) webhook {@link Alerter}s. A
	 *            <code>null</code> value is equivalent to an empty list.
	 * @param duplicateSuppression
	 *            Duration of time to suppress duplicate {@link Alert}s from
	 *            being re-sent. Two {@link Alert}s are considered equal if they
	 *            share topic, message and metadata tags (see
	 *            {@link FilteringAlerter#DEFAULT_IDENTITY_FUNCTION}). May be
	 *            <code>null</code>. Default: 5 minutes.
	 */
	public AlertsConfig(List<SmtpAlerterConfig> smtpAlerters,
			List<HttpAlerterConfig> httpAlerters,
			TimeInterval duplicateSuppression) {
		this.smtpAlerters = smtpAlerters;
		this.httpAlerters = httpAlerters;
		this.duplicateSuppression = duplicateSuppression;
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
	 * Duration of time to suppress duplicate {@link Alert}s from being re-sent.
	 * Two {@link Alert}s are considered equal if they share topic, message and
	 * metadata tags (see {@link FilteringAlerter#DEFAULT_IDENTITY_FUNCTION}).
	 *
	 * @return
	 */
	public TimeInterval getDuplicateSuppression() {
		return Optional.fromNullable(this.duplicateSuppression)
				.or(DEFAULT_DUPLICATE_SUPPRESSION);
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		for (SmtpAlerterConfig smtpAlerterConfig : getSmtpAlerters()) {
			smtpAlerterConfig.validate();
		}
		for (HttpAlerterConfig httpAlerterConfig : getHttpAlerters()) {
			httpAlerterConfig.validate();
		}
		getDuplicateSuppression().validate();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.getSmtpAlerters(), this.getHttpAlerters(),
				this.getDuplicateSuppression());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AlertsConfig) {
			AlertsConfig that = (AlertsConfig) obj;
			return equal(this.getSmtpAlerters(), that.getSmtpAlerters())
					&& equal(this.getHttpAlerters(), that.getHttpAlerters())
					&& equal(this.getDuplicateSuppression(),
							that.getDuplicateSuppression());
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this));
	}

}