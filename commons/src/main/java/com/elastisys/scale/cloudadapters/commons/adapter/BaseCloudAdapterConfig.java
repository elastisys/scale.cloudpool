package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;
import java.util.regex.Pattern;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.smtp.ClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpServerSettings;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;

/**
 * Represents a configuration for a {@link BaseCloudAdapter}.
 *
 *
 */
public class BaseCloudAdapterConfig {
	/** Default value for {@link #poolUpdatePeriod}. */
	public static final int DEFAULT_POOL_UPDATE_PERIOD = 60;

	private final ScalingGroupConfig scalingGroup;

	private final ScaleUpConfig scaleUpConfig;
	private final ScaleDownConfig scaleDownConfig;

	/**
	 * Optional configuration that describes how to send email alerts. If left
	 * out, alerts are disabled.
	 */
	private final AlertSettings alerts;

	/**
	 * The time interval (in seconds) between periodical pool size updates. A
	 * pool size update may involve terminating termination-due instances and
	 * placing new spot requests to replace terminated spot requests. Default:
	 * 60.
	 */
	private Integer poolUpdatePeriod = DEFAULT_POOL_UPDATE_PERIOD;

	public BaseCloudAdapterConfig(ScalingGroupConfig scalingGroupConfig,
			ScaleUpConfig scaleUpConfig, ScaleDownConfig scaleDownConfig,
			AlertSettings alertSettings, Integer poolUpdatePeriod) {
		this.scalingGroup = scalingGroupConfig;
		this.scaleUpConfig = scaleUpConfig;
		this.scaleDownConfig = scaleDownConfig;
		this.alerts = alertSettings;
		this.poolUpdatePeriod = poolUpdatePeriod;
	}

	public ScalingGroupConfig getScalingGroup() {
		return this.scalingGroup;
	}

	public ScaleUpConfig getScaleUpConfig() {
		return this.scaleUpConfig;
	}

	public ScaleDownConfig getScaleDownConfig() {
		return this.scaleDownConfig;
	}

	public AlertSettings getAlerts() {
		return this.alerts;
	}

	public int getPoolUpdatePeriod() {
		return Optional.fromNullable(this.poolUpdatePeriod).or(
				DEFAULT_POOL_UPDATE_PERIOD);
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudAdapterException
	 */
	public void validate() throws CloudAdapterException {
		try {
			checkNotNull(this.scalingGroup, "missing scalingGroup config");
			checkNotNull(this.scaleUpConfig, "missing scaleUpConfig");
			checkNotNull(this.scaleDownConfig, "missing scaleDownConfig");
			checkArgument(getPoolUpdatePeriod() > 0,
					"poolUpdatePeriod must be non-negative");

			this.scalingGroup.validate();
			this.scaleUpConfig.validate();
			this.scaleDownConfig.validate();
			if (this.alerts != null) {
				this.alerts.validate();
			}
		} catch (Exception e) {
			// no need to wrap further if already a config exception
			Throwables.propagateIfInstanceOf(e, CloudAdapterException.class);
			throw new CloudAdapterException(format(
					"failed to validate configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.scalingGroup, this.scaleUpConfig,
				this.scaleDownConfig, this.alerts, this.poolUpdatePeriod);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BaseCloudAdapterConfig) {
			BaseCloudAdapterConfig that = (BaseCloudAdapterConfig) obj;
			return equal(this.scalingGroup, that.scalingGroup)
					&& equal(this.scaleUpConfig, that.scaleUpConfig)
					&& equal(this.scaleDownConfig, that.scaleDownConfig)
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
	 * Describes how the {@link ScalingGroup} implementation identifies/manages
	 * scaling group members and connects to its cloud provider.
	 *
	 *
	 *
	 */
	public static class ScalingGroupConfig {
		/**
		 * The name of the logical group of servers managed by the ScalingGroup.
		 */
		private final String name;

		/**
		 * {@link ScalingGroup}-specific JSON configuration document, the
		 * contents of which depends on the particular {@link ScalingGroup}
		 * -implementation being used. Typically, a minimum amount of
		 * configuration includes login credentials for connecting to the
		 * particular cloud API endpoint.
		 */
		private final JsonObject config;

		public ScalingGroupConfig(String name, JsonObject config) {
			this.name = name;
			this.config = config;
		}

		public String getName() {
			return this.name;
		}

		public JsonObject getConfig() {
			return this.config;
		}

		/**
		 * Performs basic validation of this configuration.
		 *
		 * @throws CloudAdapterException
		 */
		public void validate() throws CloudAdapterException {
			try {
				checkNotNull(this.name, "missing name");
				checkNotNull(this.config, "missing config");
			} catch (Exception e) {
				throw new CloudAdapterException(format(
						"failed to validate scalingGroup configuration: %s",
						e.getMessage()), e);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.name, this.config);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ScalingGroupConfig) {
				ScalingGroupConfig that = (ScalingGroupConfig) obj;
				return equal(this.name, that.name)
						&& equal(this.config, that.config);
			}
			return false;
		}

		@Override
		public String toString() {
			return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
		}
	}

	/**
	 * Describes how to provision additional servers (on scale-up).
	 *
	 *
	 *
	 */
	public static class ScaleUpConfig {
		/** The name of the server type to launch. For example, m1.medium. */
		private final String size;
		/** The name of the machine image used to boot new servers. */
		private final String image;
		/** The name of the key pair to use for new machine instances. */
		private final String keyPair;
		/** The security group(s) to use for new machine instances. */
		private final List<String> securityGroups;
		/** The script to run after first boot of a new instance. */
		private final List<String> bootScript;

		public ScaleUpConfig(String size, String image, String keyPair,
				List<String> securityGroups, List<String> bootScript) {
			this.size = size;
			this.image = image;
			this.keyPair = keyPair;
			this.securityGroups = securityGroups;
			this.bootScript = bootScript;
		}

		public String getSize() {
			return this.size;
		}

		public String getImage() {
			return this.image;
		}

		public String getKeyPair() {
			return this.keyPair;
		}

		public List<String> getSecurityGroups() {
			return this.securityGroups;
		}

		public List<String> getBootScript() {
			return this.bootScript;
		}

		/**
		 * Performs basic validation of this configuration.
		 *
		 * @throws CloudAdapterException
		 */
		public void validate() throws CloudAdapterException {
			try {
				checkNotNull(this.size, "missing size");
				checkNotNull(this.image, "missing image");
				checkNotNull(this.keyPair, "missing keyPair");
				checkNotNull(this.securityGroups, "missing securityGroups");
				checkNotNull(this.bootScript, "missing bootScript");
			} catch (Exception e) {
				throw new CloudAdapterException(
						format("failed to validate scaleUpConfig: %s",
								e.getMessage()), e);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.size, this.image, this.keyPair,
					this.securityGroups, this.bootScript);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ScaleUpConfig) {
				ScaleUpConfig that = (ScaleUpConfig) obj;
				return equal(this.size, that.size)
						&& equal(this.image, that.image)
						&& equal(this.keyPair, that.keyPair)
						&& equal(this.securityGroups, that.securityGroups)
						&& equal(this.bootScript, that.bootScript);
			}
			return false;
		}

		@Override
		public String toString() {
			return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
		}
	}

	/**
	 * Describes how to decommission servers (on scale-down).
	 *
	 *
	 *
	 */
	public static class ScaleDownConfig {
		/** Policy for selecting which server to terminate. */
		private final VictimSelectionPolicy victimSelectionPolicy;

		/**
		 * How many seconds prior to the next instance hour an acquired machine
		 * instance should be scheduled for termination. This should be set to a
		 * conservative and safe value to prevent the machine from being billed
		 * for an additional hour. A value of zero is used to specify immediate
		 * termination when a scale-down is ordered.
		 */
		private final Integer instanceHourMargin;

		public ScaleDownConfig(VictimSelectionPolicy victimSelectionPolicy,
				int instanceHourMargin) {
			this.victimSelectionPolicy = victimSelectionPolicy;
			this.instanceHourMargin = instanceHourMargin;
		}

		public VictimSelectionPolicy getVictimSelectionPolicy() {
			return this.victimSelectionPolicy;
		}

		public Integer getInstanceHourMargin() {
			return this.instanceHourMargin;
		}

		public void validate() throws CloudAdapterException {
			try {
				checkNotNull(this.victimSelectionPolicy,
						"victim selection policy cannot be null");
				checkArgument(this.instanceHourMargin < 3600,
						"instance hour margin must be <= 3600");

			} catch (Exception e) {
				throw new CloudAdapterException(format(
						"failed to validate scaleDownConfig: %s",
						e.getMessage()), e);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.victimSelectionPolicy,
					this.instanceHourMargin);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ScaleDownConfig) {
				ScaleDownConfig that = (ScaleDownConfig) obj;
				return equal(this.victimSelectionPolicy,
						that.victimSelectionPolicy)
						&& equal(this.instanceHourMargin,
								that.instanceHourMargin);
			}
			return false;
		}

		@Override
		public String toString() {
			return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
		}
	}

	/**
	 * Configuration that describes how to send email alerts.
	 */
	public static class AlertSettings {
		/**
		 * The default severity filter to apply to alert messages. This filter
		 * accepts any severity.
		 */
		public static final String DEFAULT_SEVERITY_FILTER = ".*";

		/** The subject line to use in sent mails (Subject). */
		private final String subject;
		/** The receiver list (a list of recipient email addresses). */
		private final List<String> recipients;
		/** The sender email address to use in sent mails (From). */
		private final String sender;
		/**
		 * The regular expression used to filter alerts. Alerts with a severity
		 * that doesn't match the filter expression are suppressed and not sent.
		 */
		private final String severityFilter;

		/**
		 * Connection settings for the SMTP server through which emails are to
		 * be sent.
		 */
		private final MailServerSettings mailServer;

		/**
		 * Constructs a new {@link AlertSettings} instance.
		 *
		 * @param subject
		 *            The email subject line.
		 * @param recipients
		 *            The email recipients to use in sent mails ({@code To:}).
		 * @param sender
		 *            The email sender to use in sent mails ({@code From:}).
		 * @param severityFilter
		 *            The regular expression used to filter alerts. Alerts with
		 *            a severity that doesn't match the filter expression are
		 *            suppressed and not sent. Set to <code>null</code> to
		 *            accept any severity.
		 * @param mailServer
		 *            Mail server settings.
		 */
		public AlertSettings(String subject, List<String> recipients,
				String sender, String severityFilter,
				MailServerSettings mailServer) {
			this.subject = subject;
			this.recipients = recipients;
			this.sender = sender;
			this.severityFilter = severityFilter;

			this.mailServer = mailServer;
		}

		public String getSubject() {
			return this.subject;
		}

		public List<String> getRecipients() {
			return this.recipients;
		}

		public String getSender() {
			return this.sender;
		}

		public MailServerSettings getMailServer() {
			return this.mailServer;
		}

		public String getSeverityFilter() {
			return Optional.fromNullable(this.severityFilter).or(
					DEFAULT_SEVERITY_FILTER);
		}

		/**
		 * Performs basic validation of this configuration.
		 *
		 * @throws CloudAdapterException
		 */
		public void validate() throws CloudAdapterException {
			try {
				checkNotNull(this.subject, "missing subject");
				checkNotNull(this.recipients, "missing recipients");
				checkNotNull(this.sender, "missing sender");
				checkNotNull(this.mailServer, "missing mailServer");
				validateSeverityFilter(getSeverityFilter());
				this.mailServer.validate();
			} catch (Exception e) {
				throw new CloudAdapterException(
						format("failed to validate alerts config: %s",
								e.getMessage()), e);
			}
		}

		private void validateSeverityFilter(String severityFilter) {
			try {
				Pattern.compile(severityFilter);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"illegal severity filter expression: " + e.getMessage(),
						e);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.subject, this.recipients, this.sender,
					this.mailServer, getSeverityFilter());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof AlertSettings) {
				AlertSettings that = (AlertSettings) obj;
				return equal(this.subject, that.subject)
						&& equal(this.recipients, that.recipients)
						&& equal(this.sender, that.sender)
						&& equal(this.mailServer, that.mailServer)
						&& equal(this.getSeverityFilter(),
								that.getSeverityFilter());
			}
			return false;
		}

		@Override
		public String toString() {
			return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
		}
	}

	/**
	 * Connection settings for the SMTP server through which emails are to be
	 * sent.
	 *
	 *
	 *
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
		private final ClientAuthentication authentication;
		/**
		 * Enables/disables the use of SSL for SMTP connections. Default is
		 * false (disabled).
		 */
		private final Boolean useSsl;

		public MailServerSettings(String smtpHost, Integer smtpPort,
				ClientAuthentication authentication, boolean useSsl) {
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

		public ClientAuthentication getAuthentication() {
			return this.authentication;
		}

		public boolean isUseSsl() {
			return Optional.fromNullable(this.useSsl).or(false);
		}

		/**
		 * Performs basic validation of this configuration.
		 *
		 * @throws CloudAdapterException
		 */
		public void validate() throws CloudAdapterException {
			try {
				SmtpServerSettings settings = new SmtpServerSettings(
						getSmtpHost(), getSmtpPort(), getAuthentication(),
						isUseSsl());
				settings.validate();
			} catch (Exception e) {
				throw new CloudAdapterException(format(
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
		 * Convert to {@link SmtpServerSettings}.
		 *
		 * @return
		 */
		public SmtpServerSettings toSmtpServerSettings() {
			return new SmtpServerSettings(getSmtpHost(), getSmtpPort(),
					getAuthentication(), isUseSsl());
		}
	}

}
