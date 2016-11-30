package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.azure.driver.AzurePoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;

import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Configuration object for an {@link AzurePoolDriver}, which declares how to
 * authenticate with the Azure API and what resource group and region pool
 * assets are located in.
 * <p/>
 * Configuration for this Azure client.
 *
 */
public class AzurePoolDriverConfig {
    static HttpLoggingInterceptor.Level DEFAULT_AZURE_SDK_LOG_LEVEL = Level.NONE;

    /** Azure API access credentials and settings. */
    private final AzureApiAccess apiAccess;

    /**
     * The name of the <a href=
     * "https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview">resource
     * group</a> that contains referenced resources (virtual machines, networks,
     * security groups, etc).
     * <p/>
     * <i>Note: this resource group needs to exist already.</i>
     */
    private final String resourceGroup;

    /**
     * The region where pool VMs and referenced assets (networks, security
     * groups, images, etc) are located in. For example, {@code northeurope}.
     */
    private final String region;

    /**
     * Creates a new {@link AzurePoolDriverConfig}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The name of the <a href=
     *            "https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview">resource
     *            group</a> that contains referenced resources (virtual
     *            machines, networks, security groups, etc).
     *            <p/>
     *            <i>Note: this resource group needs to exist already.</i>
     * @param region
     *            The region where pool VMs and referenced assets (networks,
     *            security groups, images, etc) are located in.
     */
    public AzurePoolDriverConfig(AzureApiAccess apiAccess, String resourceGroup, String region) {
        this.apiAccess = apiAccess;
        this.resourceGroup = resourceGroup;
        this.region = region;
    }

    /**
     * Azure API access credentials and settings.
     *
     * @return
     */
    public AzureApiAccess getApiAccess() {
        return this.apiAccess;
    }

    /**
     * The name of the <a href=
     * "https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview">resource
     * group</a> that contains referenced resources (virtual machines, networks,
     * security groups, etc).
     *
     * @return
     */
    public String getResourceGroup() {
        return this.resourceGroup;
    }

    /**
     * The region where pool VMs and referenced assets (networks, security
     * groups, images, etc) are located in.
     *
     * @return
     */
    public String getRegion() {
        return this.region;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiAccess, this.resourceGroup, this.region);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzurePoolDriverConfig) {
            AzurePoolDriverConfig that = (AzurePoolDriverConfig) obj;
            return Objects.equals(this.apiAccess, that.apiAccess)
                    && Objects.equals(this.resourceGroup, that.resourceGroup)
                    && Objects.equals(this.region, that.region);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.apiAccess != null, "driverConfig: no apiAccess given");
        checkArgument(this.resourceGroup != null, "driverConfig: no resourceGroup given");
        checkArgument(this.region != null, "driverConfig: no region given");

        try {
            this.apiAccess.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("driverConfig: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
