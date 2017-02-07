package com.elastisys.scale.cloudpool.google.commons.utils;

import static com.google.api.client.util.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Convenience class for parsing out the consituent parts of an instance group
 * URL and for constructing instance group URLs from its constituent parts.
 * <p/>
 * Instance group URLs come in different flavors depending on if the instance
 * group is <a href=
 * "https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups">managed</a>
 * or <a href=
 * "https://cloud.google.com/compute/docs/instance-groups/#unmanaged_instance_groups">unmanaged</a>.
 * A managed instance group can also be hosted in a single zone (zonal) or have
 * its instances spread across multiple zones (regional) (see the <a href=
 * "https://cloud.google.com/compute/docs/instance-groups/#zonal_versus_managed_regional_instance_groups">documentation</a>
 * for more details).
 * <p/>
 * The following are examples of what instance group URLs may look like:
 * <ul>
 * <li>A managed zonal instance group:
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-b/instanceGroupManagers/my-instance-group}</li>
 * <li>A managed regional instance group:
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group}</li>
 * <li>An unmanaged instance group:
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-b/instanceGroups/my-instance-group}</li>
 * </ul>
 *
 * such as
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group}.
 *
 */
public class InstanceGroupUrl {
    private static final String MANAGED_REGIONAL_GROUP_URL_TEMPLATE = "https://www.googleapis.com/compute/v1/projects/{0}/regions/{1}/instanceGroupManagers/{2}";
    private static final String MANAGED_ZONAL_GROUP_URL_TEMPLATE = "https://www.googleapis.com/compute/v1/projects/{0}/zones/{1}/instanceGroupManagers/{2}";
    private static final String UNMANAGED_ZONAL_GROUP_URL_TEMPLATE = "https://www.googleapis.com/compute/v1/projects/{0}/zones/{1}/instanceGroups/{2}";

    /**
     * URL regexp for an instance group URL.
     * <p/>
     * {@code https://www.googleapis.com/compute/v1/projects/<project>/<locationType>/<location>/<groupType>/<groupName>}.
     * <ul>
     * <li>{@code <locationType>} can be {@code zones} or {@code regions}</li>
     * <li>{@code <location>} can be the name of a zone or a region, depending
     * on {@code <locationType>}</li>
     * <li>{@code <groupType>} can be {@code instanceGroups} (for an unmanaged
     * instance group) or {@code instanceGroupManagers} (for a managed instance
     * group)</li>
     * </ul>
     */
    private static final Pattern INSTANCE_GROUP_URL_PATTERN = Pattern.compile(
            "^https://www.googleapis.com/compute/v1/projects/(?<project>[^/]+)/(?<locationType>[^/]+)/(?<location>[^/]+)/(?<groupType>[^/]+)/(?<groupName>[^/]+)$");

    /**
     * The full instance group URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group
     */
    private final String url;

    /** The project under which the instance group exists. */
    private final String project;
    /**
     * <code>true</code> if this is a <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups">managed
     * instance group</a>, <code>false</code> if it is an <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#unmanaged_instance_groups">unmanaged
     * instance group</a>
     */
    private final boolean managed;
    /**
     * <code>true</code> if this is a single-zone (zonal) instance group.
     * <code>false</code> if this is a multi-zone (regional) instance group.
     */
    private final boolean zonal;
    /**
     * The region in which the instance group is located if this is a multi-zone
     * instance group (see {@link #zonal}), <code>null</code> otherwise.
     */
    private final String region;
    /**
     * The zone in which the instance group is located if this is a single-zone
     * instance group (see {@link #zonal}), <code>null</code> otherwise.
     */
    private final String zone;
    /** The short name of the instance group. */
    private final String name;

    /**
     * Creates an {@link InstanceGroupUrl}.
     *
     * @param instanceGroupUrl
     *            A full instance group URL, such as
     *            https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group
     */
    private InstanceGroupUrl(String instanceGroupUrl) {
        Matcher matcher = INSTANCE_GROUP_URL_PATTERN.matcher(instanceGroupUrl);
        checkArgument(matcher.matches(), "illegal instance group URL '%s' does not match expected pattern: %s",
                instanceGroupUrl, INSTANCE_GROUP_URL_PATTERN);

        this.url = instanceGroupUrl;
        this.project = matcher.group("project");
        this.zonal = matcher.group("locationType").equals("zones");
        if (this.zonal) {
            this.zone = matcher.group("location");
            this.region = null;
        } else {
            this.zone = null;
            this.region = matcher.group("location");
        }
        this.managed = matcher.group("groupType").equals("instanceGroupManagers");
        this.name = matcher.group("groupName");
    }

    /**
     * Creates an {@link InstanceGroupUrl}.
     *
     * @param instanceGroupUrl
     *            A full instance group URL, such as
     *            https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group
     * @return
     */
    public static InstanceGroupUrl parse(String instanceGroupUrl) {
        return new InstanceGroupUrl(instanceGroupUrl);
    }

    /**
     * Constructs an {@link InstanceGroupUrl} for a multi-zone (regional)
     * <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups">managed</a>
     * instance group.
     *
     * @param project
     *            The project under which the instance group exists.
     * @param region
     *            The region in which the instance group is located.
     * @param name
     *            The short name of the instance group.
     * @return
     */
    public static InstanceGroupUrl managedRegional(String project, String region, String name) {
        checkArgument(project != null, "project cannot be null");
        checkArgument(region != null, "region cannot be null");
        checkArgument(name != null, "name cannot be null");

        return new InstanceGroupUrl(MessageFormat.format(MANAGED_REGIONAL_GROUP_URL_TEMPLATE, project, region, name));
    }

    /**
     * Constructs an {@link InstanceGroupUrl} for a single-zone (zonal) <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups">managed</a>
     * instance group.
     *
     * @param project
     *            The project under which the instance group exists.
     * @param zone
     *            The zone in which the instance group is located
     * @param name
     *            The short name of the instance group.
     * @return
     */
    public static InstanceGroupUrl managedZonal(String project, String zone, String name) {
        checkArgument(project != null, "project cannot be null");
        checkArgument(zone != null, "zone cannot be null");
        checkArgument(name != null, "name cannot be null");

        return new InstanceGroupUrl(MessageFormat.format(MANAGED_ZONAL_GROUP_URL_TEMPLATE, project, zone, name));
    }

    /**
     * Constructs an {@link InstanceGroupUrl} for a single-zone (zonal) <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#unmanaged_instance_groups">unmanaged</a>
     * instance group.
     *
     * @param project
     *            The project under which the instance group exists.
     * @param zone
     *            The zone in which the instance group is located
     * @param name
     *            The short name of the instance group.
     * @return
     */
    public static InstanceGroupUrl unmanagedZonal(String project, String zone, String name) {
        checkArgument(project != null, "project cannot be null");
        checkArgument(zone != null, "zone cannot be null");
        checkArgument(name != null, "name cannot be null");

        return new InstanceGroupUrl(MessageFormat.format(UNMANAGED_ZONAL_GROUP_URL_TEMPLATE, project, zone, name));
    }

    /**
     * The full instance group URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group
     *
     * @return
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * The project under which the instance group exists.
     *
     * @return
     */
    public String getProject() {
        return this.project;
    }

    /**
     * <code>true</code> if this is a single-zone (zonal) instance group.
     * <code>false</code> if this is a multi-zone (regional) instance group.
     *
     * @return
     */
    public boolean isZonal() {
        return this.zonal;
    }

    /**
     * Returns the region in which the instance group is located if this is a
     * multi-zone instance group (see {@link #zonal}) or throws an
     * {@link IllegalStateException} if this is a zonal instance group.
     *
     * @return
     * @throws IllegalStateException
     */
    public String getRegion() throws IllegalStateException {
        checkState(!isZonal(), "cannot get region for a zonal instance group URL");
        return this.region;
    }

    /**
     * Returns the zone in which the instance group is located if this is a
     * single-zone instance group (see {@link #zonal}) or throws an
     * {@link IllegalStateException} if this is a regional instance group.
     *
     * @return
     * @throws IllegalStateException
     */
    public String getZone() throws IllegalStateException {
        checkState(isZonal(), "cannot get zone for a regional instance group URL");
        return this.zone;
    }

    /**
     * <code>true</code> if this is a <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups">managed
     * instance group</a>, <code>false</code> if it is an <a href=
     * "https://cloud.google.com/compute/docs/instance-groups/#unmanaged_instance_groups">unmanaged
     * instance group</a>
     *
     * @return
     */
    public boolean isManaged() {
        return this.managed;
    }

    /**
     * The short name of the instance group.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.url);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstanceGroupUrl) {
            InstanceGroupUrl that = (InstanceGroupUrl) obj;
            return Objects.equals(this.url, that.url);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

}
