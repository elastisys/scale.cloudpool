package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.azure.driver.AzurePoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Cloud API settings for an {@link AzurePoolDriver}.
 *
 * @see BaseCloudPoolConfig#getCloudApiSettings()
 *
 */
public class CloudApiSettings {
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
     * Creates a new {@link CloudApiSettings}.
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
    public CloudApiSettings(AzureApiAccess apiAccess, String resourceGroup, String region) {
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
    public Region getRegion() {
        return Region.findByLabelOrName(this.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiAccess, this.resourceGroup, this.region);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudApiSettings) {
            CloudApiSettings that = (CloudApiSettings) obj;
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

        checkArgument(this.getRegion() != null, "driverConfig: region '%s' not recognized", this.region);

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
