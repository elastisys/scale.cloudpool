package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.Objects;
import java.util.Optional;
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
import com.google.gson.JsonObject;

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
                    new TimeInterval(DEFAULT_INITIAL_EXP_BACKOFF_DELAY, TimeUnit.SECONDS)),
            new TimeInterval(30L, TimeUnit.SECONDS), new TimeInterval(5L, TimeUnit.MINUTES));

    /** Default pool update interval. */
    public static final PoolUpdateConfig DEFAULT_POOL_UPDATE_CONFIG = new PoolUpdateConfig(
            new TimeInterval(60L, TimeUnit.SECONDS));

    /** Default scale-in policy is to terminate the newest machines first. */
    public static final ScaleInConfig DEFAULT_SCALE_IN_CONFIG = new ScaleInConfig(
            ScaleInConfig.DEFAULT_VICTIM_SELECTION_POLICY);

    /**
     * The logical name of the managed group of machines. The exact way of
     * identifying pool members may differ between {@link CloudPoolDriver}s, but
     * machine tags could, for example, be used to mark pool membership.
     * Required.
     */
    private final String name;

    /**
     * API access credentials and settings required to communicate with the
     * targeted cloud. The structure of this document is cloud-specific. This
     * document is passed as-is to the {@link CloudPoolDriver}. Required.
     */
    private final JsonObject cloudApiSettings;

    /**
     * Describes how to provision additional servers (on scale-out). The
     * appearance of this document is cloud-specific. This document is passed
     * as-is to the {@link CloudPoolDriver}. Requried.
     */
    private final JsonObject provisioningTemplate;

    /**
     * Configuration that describes how to shrink the cloud pool. May be
     * <code>null</code>. Default scale-in policy is to immediately terminate
     * newest instance.
     */
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
     * Creates a {@link BaseCloudPoolConfig}.
     *
     * @param name
     *            The logical name of the managed group of machines. The exact
     *            way of identifying pool members may differ between
     *            {@link CloudPoolDriver}s, but machine tags could, for example,
     *            be used to mark pool membership. Required.
     * @param cloudApiSettings
     *            API access credentials and settings required to communicate
     *            with the targeted cloud. The structure of this document is
     *            cloud-specific. This document is passed as-is to the
     *            {@link CloudPoolDriver}. Required.
     * @param provisioningTemplate
     *            Describes how to provision additional servers (on scale-out).
     *            The appearance of this document is cloud-specific. This
     *            document is passed as-is to the {@link CloudPoolDriver}.
     *            Required.
     * @param scaleInConfig
     *            Configuration that describes how to shrink the cloud pool. May
     *            be <code>null</code>. Default scale-in policy is to
     *            immediately terminate newest instance.
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
    public BaseCloudPoolConfig(String name, JsonObject cloudApiSettings, JsonObject provisioningTemplate,
            ScaleInConfig scaleInConfig, AlertersConfig alertSettings, PoolFetchConfig poolFetchConfig,
            PoolUpdateConfig poolUpdatePeriodConfig) {
        this.name = name;
        this.cloudApiSettings = cloudApiSettings;
        this.provisioningTemplate = provisioningTemplate;
        this.scaleInConfig = scaleInConfig;
        this.alerts = alertSettings;
        this.poolFetch = poolFetchConfig;
        this.poolUpdate = poolUpdatePeriodConfig;
    }

    /**
     * The logical name of the managed group of machines. The exact way of
     * identifying pool members may differ between {@link CloudPoolDriver}s, but
     * machine tags could, for example, be used to mark pool membership.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * API access credentials and settings required to communicate with the
     * targeted cloud. The structure of this document is cloud-specific. This
     * document is passed as-is to the {@link CloudPoolDriver}.
     *
     * @return
     */
    public JsonObject getCloudApiSettings() {
        return this.cloudApiSettings;
    }

    /**
     * Describes how to provision additional servers (on scale-out). The
     * appearance of this document is cloud-specific. This document is passed
     * as-is to the {@link CloudPoolDriver}.
     *
     * @return
     */
    public JsonObject getProvisioningTemplate() {
        return this.provisioningTemplate;
    }

    /**
     * Configuration that describes how to shrink the cloud pool.
     *
     * @return
     */
    public ScaleInConfig getScaleInConfig() {
        return Optional.ofNullable(this.scaleInConfig).orElse(DEFAULT_SCALE_IN_CONFIG);
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
        return Optional.ofNullable(this.poolFetch).orElse(DEFAULT_POOL_FETCH_CONFIG);
    }

    /**
     * The time interval (in seconds) between periodical pool size updates.
     *
     * @return
     */
    public PoolUpdateConfig getPoolUpdate() {
        return Optional.ofNullable(this.poolUpdate).orElse(DEFAULT_POOL_UPDATE_CONFIG);
    }

    /**
     * Performs basic validation of this configuration.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.name != null, "missing name");
            checkArgument(this.cloudApiSettings != null, "missing cloudApiSettings");
            checkArgument(this.provisioningTemplate != null, "missing provisioningTemplate");

            if (this.scaleInConfig != null) {
                this.scaleInConfig.validate();
            }
            if (this.alerts != null) {
                this.alerts.validate();
            }
            getPoolFetch().validate();
            getPoolUpdate().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(format("failed to validate cloudpool configuration: %s", e.getMessage()),
                    e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.cloudApiSettings, this.provisioningTemplate, getScaleInConfig(),
                this.alerts, getPoolFetch(), getPoolUpdate());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseCloudPoolConfig) {
            BaseCloudPoolConfig that = (BaseCloudPoolConfig) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.cloudApiSettings, that.cloudApiSettings) //
                    && Objects.equals(this.provisioningTemplate, that.provisioningTemplate) //
                    && Objects.equals(getScaleInConfig(), that.getScaleInConfig()) //
                    && Objects.equals(this.alerts, that.alerts) //
                    && Objects.equals(getPoolFetch(), that.getPoolFetch()) //
                    && Objects.equals(getPoolUpdate(), that.getPoolUpdate());
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

        public MailServerSettings(String smtpHost, Integer smtpPort, SmtpClientAuthentication authentication,
                boolean useSsl) {
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.authentication = authentication;
            this.useSsl = useSsl;
        }

        public String getSmtpHost() {
            return this.smtpHost;
        }

        public Integer getSmtpPort() {
            return Optional.ofNullable(this.smtpPort).orElse(DEFAULT_SMTP_PORT);
        }

        public SmtpClientAuthentication getAuthentication() {
            return this.authentication;
        }

        public boolean isUseSsl() {
            return Optional.ofNullable(this.useSsl).orElse(false);
        }

        /**
         * Performs basic validation of this configuration.
         *
         * @throws CloudPoolException
         */
        public void validate() throws CloudPoolException {
            try {
                SmtpClientConfig settings = new SmtpClientConfig(getSmtpHost(), getSmtpPort(), getAuthentication(),
                        isUseSsl());
                settings.validate();
            } catch (Exception e) {
                throw new CloudPoolException(format("failed to validate mailServerSettings: %s", e.getMessage()), e);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(getSmtpHost(), getSmtpPort(), getAuthentication(), isUseSsl());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MailServerSettings) {
                MailServerSettings that = (MailServerSettings) obj;
                return Objects.equals(getSmtpHost(), that.getSmtpHost())
                        && Objects.equals(getSmtpPort(), that.getSmtpPort())
                        && Objects.equals(getAuthentication(), that.getAuthentication())
                        && Objects.equals(isUseSsl(), that.isUseSsl());
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
            return new SmtpClientConfig(getSmtpHost(), getSmtpPort(), getAuthentication(), isUseSsl());
        }
    }

}
