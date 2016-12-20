package com.elastisys.scale.cloudpool.gce.driver.config;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.gce.driver.GcePoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * {@link GcePoolDriver}-specific server provisioning template, which tracks the
 * instance group to manage.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class ProvisioningTemplate {
    /**
     * The name of an Instance Group whose size is to be managed by the cloud
     * pool. Required.
     */
    private final String instanceGroup;

    /**
     * The name of the project under which the instance group was created.
     * Required.
     */
    private final String project;
    /**
     * The region where the Instance Group is located, in case the instance
     * group is regional/multi-zone. If the instance group is a zonal one,
     * specify {@link #zone} instead.
     */
    private final String region;
    /**
     * The zone where the Instance Group is located, in case the instance group
     * is zonal/single-zone. If the instance group is a multi-zone one, specify
     * {@link #region} instead.
     */
    private final String zone;

    /**
     * Creates a new {@link ProvisioningTemplate}.
     *
     * @param instanceGroup
     *            The name of an Instance Group whose size is to be managed by
     *            the cloud pool. Required.
     * @param project
     *            The name of the project under which the instance group was
     *            created. Required.
     * @param region
     *            The region where the Instance Group is located, in case the
     *            instance group is regional/multi-zone. If the instance group
     *            is a zonal one, specify {@link #zone} instead.
     * @param zone
     *            The zone where the Instance Group is located, in case the
     *            instance group is zonal/single-zone. If the instance group is
     *            a multi-zone one, specify {@link #region} instead.
     */
    public ProvisioningTemplate(String instanceGroup, String project, String region, String zone) {
        this.instanceGroup = instanceGroup;
        this.project = project;
        this.region = region;
        this.zone = zone;
    }

    /**
     * The name of an Instance Group whose size is to be managed by the cloud
     * pool.
     *
     * @return
     */
    public String getInstanceGroup() {
        return this.instanceGroup;
    }

    /**
     * The name of the project under which the instance group was created.
     *
     * @return
     */
    public String getProject() {
        return this.project;
    }

    /**
     * The region where the Instance Group is located, in case the instance
     * group is regional/multi-zone. Otherwise null.
     *
     * @return
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * The zone where the Instance Group is located, in case the instance group
     * is zonal/single-zone. Otherwise null.
     *
     * @return
     */
    public String getZone() {
        return this.zone;
    }

    /**
     * Returns <code>true</code> if the configured instance group is a
     * zonal/single-zone one. Returns <code>false</code> if it is a
     * regional/multi-zone instance group.
     *
     * @return
     */
    public boolean isSingleZoneGroup() {
        return this.zone != null;
    }

    /**
     * Validates that this {@link ProvisioningTemplate} contains all mandatory
     * field.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.instanceGroup != null, "provisioningTemplate: no instanceGroup given");
        checkArgument(this.project != null, "provisioningTemplate: no project given");
        checkArgument(this.region != null || this.zone != null,
                "provisioningTemplate: either region (for a multi-zone instance group) or zone (for a single-zone instance group) must be given");
        checkArgument(this.region != null ^ this.zone != null,
                "provisioningTemplate: only one of region (for a multi-zone instance group) and zone (for a single-zone instance group) may be specified");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.instanceGroup);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningTemplate) {
            ProvisioningTemplate that = (ProvisioningTemplate) obj;
            return Objects.equals(this.instanceGroup, that.instanceGroup)//
                    && Objects.equals(this.project, that.project)//
                    && Objects.equals(this.region, that.region) //
                    && Objects.equals(this.zone, that.zone);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
