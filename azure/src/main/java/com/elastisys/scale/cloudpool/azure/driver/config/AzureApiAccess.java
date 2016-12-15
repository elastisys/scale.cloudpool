package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Azure API access credentials and settings
 *
 * @see CloudApiSettings
 */
public class AzureApiAccess {
    static HttpLoggingInterceptor.Level DEFAULT_AZURE_SDK_LOG_LEVEL = Level.NONE;

    static final TimeInterval DEFAULT_CONNECTION_TIMEOUT = new TimeInterval(10L, "seconds");
    static final TimeInterval DEFAULT_READ_TIMEOUT = new TimeInterval(10L, "seconds");

    /**
     * The Azure account subscription that will be billed for resources
     * allocated by this client. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     */
    private final String subscriptionId;

    /** Authentication credentials for this client. */
    private final AzureAuth auth;

    /**
     * Connection timeout to use for API interactions. May be <code>null</code>.
     * Default: 10 seconds.
     */
    private final TimeInterval connectionTimeout;

    /**
     * Read timeout to use for API interactions. May be <code>null</code>.
     * Default: 10 seconds.
     */
    private final TimeInterval readTimeout;

    /**
     * The log level to set for the Azure SDK. One of: NONE, BASIC, HEADERS, or
     * BODY. May be <code>null</code>. Default: NONE.
     */
    private final HttpLoggingInterceptor.Level azureSdkLogLevel;

    /**
     * Creates a new {@link CloudApiSettings}.
     *
     * @param subscriptionId
     *            The Azure account subscription that will be billed for
     *            resources allocated by this client. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param auth
     *            Authentication credentials for this client.
     */
    public AzureApiAccess(String subscriptionId, AzureAuth auth) {
        this(subscriptionId, auth, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_AZURE_SDK_LOG_LEVEL);
    }

    /**
     * Creates a new {@link CloudApiSettings}.
     *
     * @param subscriptionId
     *            The Azure account subscription that will be billed for
     *            resources allocated by this client. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param auth
     *            Authentication credentials for this client.
     * @param connectionTimeout
     *            Connection timeout to use for API interactions. May be
     *            <code>null</code>. Default: 10 seconds.
     * @param readTimeout
     *            Read timeout to use for API interactions. May be
     *            <code>null</code>. Default: 10 seconds.
     * @param azureSdkLogLevel
     *            The log level to set for the Azure SDK. One of: NONE, BASIC,
     *            HEADERS, or BODY. May be <code>null</code>. Default: NONE.
     */
    public AzureApiAccess(String subscriptionId, AzureAuth auth, TimeInterval connectionTimeout,
            TimeInterval readTimeout, Level azureSdkLogLevel) {
        this.subscriptionId = subscriptionId;
        this.auth = auth;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.azureSdkLogLevel = azureSdkLogLevel;
    }

    /**
     * The Azure account subscription that will be billed for resources
     * allocated by this client. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     *
     * @return
     */
    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    /**
     * Authentication credentials for this client.
     *
     * @return
     */
    public AzureAuth getAuth() {
        return this.auth;
    }

    /**
     * Connection timeout to use for API interactions.
     *
     * @return
     */
    public TimeInterval getConnectionTimeout() {
        return Optional.ofNullable(this.connectionTimeout).orElse(DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * Read timeout to use for API interactions.
     *
     * @return
     */
    public TimeInterval getReadTimeout() {
        return Optional.ofNullable(this.readTimeout).orElse(DEFAULT_READ_TIMEOUT);
    }

    /**
     * The log level to set for the Azure SDK. One of: NONE, BASIC, HEADERS, or
     * BODY.
     *
     * @return
     */
    public HttpLoggingInterceptor.Level getAzureSdkLogLevel() {
        return Optional.ofNullable(this.azureSdkLogLevel).orElse(DEFAULT_AZURE_SDK_LOG_LEVEL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.subscriptionId, this.auth, this.azureSdkLogLevel);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzureApiAccess) {
            AzureApiAccess that = (AzureApiAccess) obj;
            return Objects.equals(this.subscriptionId, that.subscriptionId) //
                    && Objects.equals(this.auth, that.auth) //
                    && Objects.equals(this.connectionTimeout, that.connectionTimeout) //
                    && Objects.equals(this.readTimeout, that.readTimeout)
                    && Objects.equals(this.azureSdkLogLevel, that.azureSdkLogLevel);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.subscriptionId != null, "apiAccess: no subscriptionId given");
        checkArgument(this.auth != null, "apiAccess: no auth given");

        try {
            getConnectionTimeout().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("apiAccess: connectionTimeout: " + e.getMessage(), e);
        }
        try {
            getReadTimeout().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("apiAccess: readTimeout: " + e.getMessage(), e);
        }

        try {
            this.auth.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("apiAccess: " + e.getMessage(), e);
        }

    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
