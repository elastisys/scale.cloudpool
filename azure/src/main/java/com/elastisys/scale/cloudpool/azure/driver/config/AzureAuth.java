package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;

/**
 * Authentication credentials for the cloud pool application.
 * <p/>
 * It captures credentials for a <a href=
 * "https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md">service
 * principal registration</a> that needs to have been set up for the cloudpool
 * application. A service principal registration grants the cloudpool
 * application rights to operate on behalf of a certain Azure account.
 * <p/>
 *
 */
public class AzureAuth {
    /** Default Azure environment to authenticate against. */
    public static final AzureEnvironment DEFAULT_ENVIRONMENT = AzureEnvironment.AZURE;

    /**
     * The active directory client id for this application. May also be referred
     * to as {@code appId}. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     */
    private final String clientId;
    /**
     * The domain or tenant id containing this application. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     */
    private final String domain;
    /**
     * The authentication secret (password) for this application. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     */
    private final String secret;

    /**
     * The particular Azure environment (see {@link AzureEnvironment}) to
     * authenticate against. One of: {@code AZURE}, {@code AZURE_CHINA},
     * {@code AZURE_US_GOVERNMENT}, {@code AZURE_GERMANY}. May be {@code null}.
     * Default: {@code AZURE}, the world-wide public Azure cloud.
     */
    private final String environment;

    /**
     * Creates authentication credentials for the cloud pool application, using
     * the default (global) Azure environment.
     *
     * @param clientId
     *            The active directory client id for this application. May also
     *            be referred to as {@code appId}. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param domain
     *            The domain or tenant id containing this application. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param secret
     *            The authentication secret (password) for this application.
     *            Format: {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     */
    public AzureAuth(String clientId, String domain, String secret) {
        this(clientId, domain, secret, null);
    }

    /**
     * Creates authentication credentials for the cloud pool application.
     *
     * @param clientId
     *            The active directory client id for this application. May also
     *            be referred to as {@code appId}. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param domain
     *            The domain or tenant id containing this application. Format:
     *            {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param secret
     *            The authentication secret (password) for this application.
     *            Format: {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     * @param environment
     *            The particular Azure environment (see
     *            {@link AzureEnvironment}) to authenticate against. One of:
     *            {@code AZURE}, {@code AZURE_CHINA},
     *            {@code AZURE_US_GOVERNMENT}, {@code AZURE_GERMANY}. May be
     *            {@code null}. Default: {@code AZURE}, the world-wide public
     *            Azure cloud.
     */
    public AzureAuth(String clientId, String domain, String secret, String environment) {
        this.clientId = clientId;
        this.domain = domain;
        this.secret = secret;
        this.environment = environment;
    }

    /**
     * The active directory client id for this application. May also be referred
     * to as {@code appId}. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     *
     * @return
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * The domain or tenant id containing this application. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     *
     * @return
     */
    public String getDomain() {
        return this.domain;
    }

    /**
     * The authentication secret (password) for this application. Format:
     * {@code XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}.
     *
     * @return
     */
    public String getSecret() {
        return this.secret;
    }

    /**
     * The particular Azure environment (see {@link AzureEnvironment}) to
     * authenticate against. One of: {@code AZURE}, {@code AZURE_CHINA},
     * {@code AZURE_US_GOVERNMENT}, {@code AZURE_GERMANY}.
     *
     * @return
     */
    public AzureEnvironment getEnvironment() {
        return this.environment == null ? DEFAULT_ENVIRONMENT : parseEnvironment(this.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.clientId, this.domain, this.secret, this.environment);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzureAuth) {
            AzureAuth other = (AzureAuth) obj;
            return Objects.equals(this.clientId, other.clientId) && Objects.equals(this.domain, other.domain)
                    && Objects.equals(this.secret, other.secret) && Objects.equals(this.environment, other.environment);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.clientId != null, "auth: no clientId given");
        checkArgument(this.domain != null, "auth: no domain given");
        checkArgument(this.secret != null, "auth: no secret given");
        if (this.environment != null) {
            parseEnvironment(this.environment);
        }
    }

    static AzureEnvironment parseEnvironment(String environmentName) {
        switch (environmentName) {
        case "AZURE":
            return AzureEnvironment.AZURE;
        case "AZURE_CHINA":
            return AzureEnvironment.AZURE_CHINA;
        case "AZURE_US_GOVERNMENT":
            return AzureEnvironment.AZURE_US_GOVERNMENT;
        case "AZURE_GERMANY":
            return AzureEnvironment.AZURE_GERMANY;
        default:
            throw new IllegalArgumentException("auth: unrecognized Azure environment: " + environmentName);
        }
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Creates {@link AzureTokenCredentials} for authentication with the Azure
     * Java SDK from this {@link AzureAuth}.
     *
     * @return
     */
    public AzureTokenCredentials toTokenCredentials() {
        return new ApplicationTokenCredentials(getClientId(), getDomain(), getSecret(), getEnvironment());
    }
}
