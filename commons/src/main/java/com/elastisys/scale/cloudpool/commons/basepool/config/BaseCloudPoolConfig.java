package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Represents a configuration for a {@link BaseCloudPool}.
 */
public class BaseCloudPoolConfig {

	/** Default number of maximum pool fetch retries. */
	private static final int DEFAULT_MAX_RETRIES = 3;
	/**
	 * Initial delay (in seconds) to use after first failed pool fetch attempt.
	 */
	private static final long DEFAULT_INITIAL_EXP_BACKOFF_DELAY = 3;
	/** Default {@link PoolFetchConfig}. */
	public static final PoolFetchConfig DEFAULT_POOL_FETCH_CONFIG = new PoolFetchConfig(
			new RetriesConfig(DEFAULT_MAX_RETRIES,
					new TimeInterval(DEFAULT_INITIAL_EXP_BACKOFF_DELAY,
							TimeUnit.SECONDS)),
			new TimeInterval(30L, TimeUnit.SECONDS),
			new TimeInterval(5L, TimeUnit.MINUTES));

	/** Default pool update interval. */
	public static final PoolUpdateConfig DEFAULT_POOL_UPDATE_CONFIG = new PoolUpdateConfig(
			new TimeInterval(60L, TimeUnit.SECONDS));

	/** Configuration for the {@link CloudPoolDriver}. */
	private final CloudPoolConfig cloudPool;

	/** Configuration that describes how to grow the cloud pool. */
	private final ScaleOutConfig scaleOutConfig;

	/** Configuration that describes how to shrink the cloud pool. */
	private final ScaleInConfig scaleInConfig;

	/**
	 * Configuration that describes how to send {@link Alert}s. May be
	 * <code>null</code>.
	 */
	private final AlertersConfig alerts;

	/**
	 * Controls the {@link CloudPool}'s behavior with respect to how often to
	 * attempt fetching of {@link MachinePool} and for how long to mask cloud
	 * API errors.
	 */
	private final PoolFetchConfig poolFetch;

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 */
	private final PoolUpdateConfig poolUpdate;

	/**
	 * @param cloudPoolConfig
	 *            Configuration for the {@link CloudPoolDriver}.
	 * @param scaleOutConfig
	 *            Configuration that describes how to grow the cloud pool.
	 * @param scaleInConfig
	 *            Configuration that describes how to shrink the cloud pool.
	 * @param alertSettings
	 *            Configuration that describes how to send alerts. May be
	 *            <code>null</code>.
	 * @param poolFetchConfig
	 *            Controls the {@link CloudPool}'s behavior with respect to how
	 *            often to attempt fetching of {@link MachinePool} and for how
	 *            long to mask cloud API errors. May be <code>null</code>.
	 *            Default: {@code retries}: 3 retries with 3 second initial
	 *            exponential back-off delay, {@code refreshInterval}: 30
	 *            seconds, {@code reachabilityTimeout}: 5 minutes.
	 * @param poolUpdatePeriodConfig
	 *            The time interval (in seconds) between periodical pool size
	 *            updates. May be <code>null</code>. Default: 60 seconds.
	 */
	public BaseCloudPoolConfig(CloudPoolConfig cloudPoolConfig,
			ScaleOutConfig scaleOutConfig, ScaleInConfig scaleInConfig,
			AlertersConfig alertSettings, PoolFetchConfig poolFetchConfig,
			PoolUpdateConfig poolUpdatePeriodConfig) {
		this.cloudPool = cloudPoolConfig;
		this.scaleOutConfig = scaleOutConfig;
		this.scaleInConfig = scaleInConfig;
		this.alerts = alertSettings;
		this.poolFetch = poolFetchConfig;
		this.poolUpdate = poolUpdatePeriodConfig;
	}

	/**
	 * Configuration for the {@link CloudPoolDriver}.
	 *
	 * @return
	 */
	public CloudPoolConfig getCloudPool() {
		return this.cloudPool;
	}

	/**
	 * Configuration that describes how to grow the cloud pool.
	 *
	 * @return
	 */
	public ScaleOutConfig getScaleOutConfig() {
		return this.scaleOutConfig;
	}

	/**
	 * Configuration that describes how to shrink the cloud pool.
	 *
	 * @return
	 */
	public ScaleInConfig getScaleInConfig() {
		return this.scaleInConfig;
	}

	/**
	 * Configuration that describes how to send email alerts. May be
	 * <code>null</code>.
	 *
	 * @return
	 */
	public AlertersConfig getAlerts() {
		return this.alerts;
	}

	/**
	 * Controls the {@link CloudPool}'s behavior with respect to how often to
	 * attempt fetching of {@link MachinePool} and for how long to mask cloud
	 * API errors.
	 *
	 * @return
	 */
	public PoolFetchConfig getPoolFetch() {
		return Optional.fromNullable(this.poolFetch)
				.or(DEFAULT_POOL_FETCH_CONFIG);
	}

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 *
	 * @return
	 */
	public PoolUpdateConfig getPoolUpdate() {
		return Optional.fromNullable(this.poolUpdate)
				.or(DEFAULT_POOL_UPDATE_CONFIG);
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws IllegalArgumentException
	 */
	public void validate() throws IllegalArgumentException {
		try {
			checkNotNull(this.cloudPool, "missing cloudPool config");
			checkNotNull(this.scaleOutConfig, "missing scaleOutConfig");
			checkNotNull(this.scaleInConfig, "missing scaleInConfig");

			this.cloudPool.validate();
			this.scaleOutConfig.validate();
			this.scaleInConfig.validate();
			if (this.alerts != null) {
				this.alerts.validate();
			}
			getPoolFetch().validate();
			getPoolUpdate().validate();
		} catch (Exception e) {
			// no need to wrap further if already a config exception
			Throwables.propagateIfInstanceOf(e, IllegalArgumentException.class);
			throw new IllegalArgumentException(format(
					"failed to validate configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.cloudPool, this.scaleOutConfig,
				this.scaleInConfig, this.alerts, getPoolFetch(),
				getPoolUpdate());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BaseCloudPoolConfig) {
			BaseCloudPoolConfig that = (BaseCloudPoolConfig) obj;
			return equal(this.cloudPool, that.cloudPool)
					&& equal(this.scaleOutConfig, that.scaleOutConfig)
					&& equal(this.scaleInConfig, that.scaleInConfig)
					&& equal(this.alerts, that.alerts)
					&& equal(this.getPoolFetch(), that.getPoolFetch())
					&& equal(this.getPoolUpdate(), that.getPoolUpdate());
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this));
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
				SmtpClientConfig settings = new SmtpClientConfig(getSmtpHost(),
						getSmtpPort(), getAuthentication(), isUseSsl());
				settings.validate();
			} catch (Exception e) {
				throw new CloudPoolException(
						format("failed to validate mailServerSettings: %s",
								e.getMessage()),
						e);
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
