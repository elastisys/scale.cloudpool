package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Represents a configuration for a {@link BaseCloudPool}.
 */
public class BaseCloudPoolConfig {
	/** Default value for {@link #poolUpdatePeriod}. */
	public static final int DEFAULT_POOL_UPDATE_PERIOD = 60;

	private final CloudPoolConfig cloudPool;

	private final ScaleOutConfig scaleOutConfig;
	private final ScaleInConfig scaleInConfig;

	/**
	 * Optional configuration that describes how to send email alerts. If left
	 * out, alerts are disabled.
	 */
	private final AlertsConfig alerts;

	/**
	 * The time interval (in seconds) between periodical pool size updates. A
	 * pool size update may involve terminating termination-due instances and
	 * placing new spot requests to replace terminated spot requests. Default:
	 * 60.
	 */
	private Integer poolUpdatePeriod = DEFAULT_POOL_UPDATE_PERIOD;

	public BaseCloudPoolConfig(CloudPoolConfig cloudPoolConfig,
			ScaleOutConfig scaleOutConfig, ScaleInConfig scaleInConfig,
			AlertsConfig alertSettings, Integer poolUpdatePeriod) {
		this.cloudPool = cloudPoolConfig;
		this.scaleOutConfig = scaleOutConfig;
		this.scaleInConfig = scaleInConfig;
		this.alerts = alertSettings;
		this.poolUpdatePeriod = poolUpdatePeriod;
	}

	public CloudPoolConfig getCloudPool() {
		return this.cloudPool;
	}

	public ScaleOutConfig getScaleOutConfig() {
		return this.scaleOutConfig;
	}

	public ScaleInConfig getScaleInConfig() {
		return this.scaleInConfig;
	}

	public AlertsConfig getAlerts() {
		return this.alerts;
	}

	public int getPoolUpdatePeriod() {
		return Optional.fromNullable(this.poolUpdatePeriod).or(
				DEFAULT_POOL_UPDATE_PERIOD);
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		try {
			checkNotNull(this.cloudPool, "missing cloudPool config");
			checkNotNull(this.scaleOutConfig, "missing scaleOutConfig");
			checkNotNull(this.scaleInConfig, "missing scaleInConfig");
			checkArgument(getPoolUpdatePeriod() > 0,
					"poolUpdatePeriod must be non-negative");

			this.cloudPool.validate();
			this.scaleOutConfig.validate();
			this.scaleInConfig.validate();
			if (this.alerts != null) {
				this.alerts.validate();
			}
		} catch (Exception e) {
			// no need to wrap further if already a config exception
			Throwables.propagateIfInstanceOf(e, CloudPoolException.class);
			throw new CloudPoolException(format(
					"failed to validate configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.cloudPool, this.scaleOutConfig,
				this.scaleInConfig, this.alerts, this.poolUpdatePeriod);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BaseCloudPoolConfig) {
			BaseCloudPoolConfig that = (BaseCloudPoolConfig) obj;
			return equal(this.cloudPool, that.cloudPool)
					&& equal(this.scaleOutConfig, that.scaleOutConfig)
					&& equal(this.scaleInConfig, that.scaleInConfig)
					&& equal(this.alerts, that.alerts)
					&& equal(this.poolUpdatePeriod, that.poolUpdatePeriod);
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
	}

	/**
	 * Connection settings for the SMTP server through which emails are to be
	 * sent.
	 */
	public static class MailServerSettings {
		private static final int DEFAULT_SMTP_PORT = 25;
		/** SMTP server host name/IP address. */
		private final String smtpHost;
		/** SMTP server port. Default is 25. */
		private final Integer smtpPort;
		/**
		 * Optional username/password to authenticate with SMTP server. If left
		 * out, authentication is disabled.
		 */
		private final SmtpClientAuthentication authentication;
		/**
		 * Enables/disables the use of SSL for SMTP connections. Default is
		 * false (disabled).
		 */
		private final Boolean useSsl;

		public MailServerSettings(String smtpHost, Integer smtpPort,
				SmtpClientAuthentication authentication, boolean useSsl) {
			this.smtpHost = smtpHost;
			this.smtpPort = smtpPort;
			this.authentication = authentication;
			this.useSsl = useSsl;
		}

		public String getSmtpHost() {
			return this.smtpHost;
		}

		public Integer getSmtpPort() {
			return Optional.fromNullable(this.smtpPort).or(DEFAULT_SMTP_PORT);
		}

		public SmtpClientAuthentication getAuthentication() {
			return this.authentication;
		}

		public boolean isUseSsl() {
			return Optional.fromNullable(this.useSsl).or(false);
		}

		/**
		 * Performs basic validation of this configuration.
		 *
		 * @throws CloudPoolException
		 */
		public void validate() throws CloudPoolException {
			try {
				SmtpClientConfig settings = new SmtpClientConfig(
						getSmtpHost(), getSmtpPort(), getAuthentication(),
						isUseSsl());
				settings.validate();
			} catch (Exception e) {
				throw new CloudPoolException(format(
						"failed to validate mailServerSettings: %s",
						e.getMessage()), e);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(getSmtpHost(), getSmtpPort(),
					getAuthentication(), isUseSsl());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MailServerSettings) {
				MailServerSettings that = (MailServerSettings) obj;
				return equal(this.getSmtpHost(), that.getSmtpHost())
						&& equal(this.getSmtpPort(), that.getSmtpPort())
						&& equal(this.getAuthentication(),
								that.getAuthentication())
						&& equal(this.isUseSsl(), that.isUseSsl());
			}
			return false;
		}

		@Override
		public String toString() {
			return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
		}

		/**
		 * Convert to {@link SmtpClientConfig}.
		 *
		 * @return
		 */
		public SmtpClientConfig toSmtpServerSettings() {
			return new SmtpClientConfig(getSmtpHost(), getSmtpPort(),
					getAuthentication(), isUseSsl());
		}
	}

}
