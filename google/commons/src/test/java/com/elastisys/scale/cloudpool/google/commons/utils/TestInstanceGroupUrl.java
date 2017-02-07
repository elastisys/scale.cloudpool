package com.elastisys.scale.cloudpool.google.commons.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises {@link InstanceGroupUrl}.
 */
public class TestInstanceGroupUrl {
    private static final String ZONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project1/zones/europe-west1-b/instanceGroupManagers/my-instance-group1";
    private static final String REGIONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project2/regions/europe-west1/instanceGroupManagers/my-instance-group2";
    private static final String UNMANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project3/zones/europe-west1-b/instanceGroups/my-instance-group3";

    /**
     * Parse out the constituent parts of a zonal managed instance group URL.
     */
    @Test
    public void parseZonalManagedInstanceGroup() {
        InstanceGroupUrl groupUrl = InstanceGroupUrl.parse(ZONAL_MANAGED_INSTANCE_GROUP);
        assertThat(groupUrl.getProject(), is("my-project1"));
        assertThat(groupUrl.isZonal(), is(true));
        assertThat(groupUrl.getZone(), is("europe-west1-b"));
        assertThat(groupUrl.isManaged(), is(true));
        assertThat(groupUrl.getName(), is("my-instance-group1"));
    }

    /**
     * Parse out the constituent parts of a regional managed instance group URL.
     */
    @Test
    public void parseRegionalManagedInstanceGroup() {
        InstanceGroupUrl groupUrl = InstanceGroupUrl.parse(REGIONAL_MANAGED_INSTANCE_GROUP);
        assertThat(groupUrl.getProject(), is("my-project2"));
        assertThat(groupUrl.isZonal(), is(false));
        assertThat(groupUrl.getRegion(), is("europe-west1"));
        assertThat(groupUrl.isManaged(), is(true));
        assertThat(groupUrl.getName(), is("my-instance-group2"));
    }

    /**
     * Parse out the constituent parts of an unmanaged instance group URL.
     */
    @Test
    public void parseUnmanagedInstanceGroup() {
        InstanceGroupUrl groupUrl = InstanceGroupUrl.parse(UNMANAGED_INSTANCE_GROUP);
        assertThat(groupUrl.getProject(), is("my-project3"));
        assertThat(groupUrl.isZonal(), is(true));
        assertThat(groupUrl.getZone(), is("europe-west1-b"));
        assertThat(groupUrl.isManaged(), is(false));
        assertThat(groupUrl.getName(), is("my-instance-group3"));
    }

    /**
     * It should be an error to attempt to get the zone for a regional instance
     * group.
     */
    @Test(expected = IllegalStateException.class)
    public void getZoneFromRegionalInstanceGroupShouldFail() {
        InstanceGroupUrl groupUrl = InstanceGroupUrl.parse(REGIONAL_MANAGED_INSTANCE_GROUP);
        groupUrl.getZone();
    }

    /**
     * It should be an error to attempt to get the region for a zonal instance
     * group.
     */
    @Test(expected = IllegalStateException.class)
    public void getRegionFromZonalInstanceGroupShouldFail() {
        InstanceGroupUrl groupUrl = InstanceGroupUrl.parse(ZONAL_MANAGED_INSTANCE_GROUP);
        groupUrl.getRegion();
    }

    @Test
    public void constructZonalManagedInstanceGroupUrl() {
        InstanceGroupUrl constructedUrl = InstanceGroupUrl.managedZonal("my-project1", "europe-west1-b", "my-instance-group1");
        assertThat(constructedUrl.getUrl(),
                is(ZONAL_MANAGED_INSTANCE_GROUP));
    }

    @Test
    public void constructRegionalManagedInstanceGroupUrl() {
        assertThat(InstanceGroupUrl.managedRegional("my-project2", "europe-west1", "my-instance-group2").getUrl(),
                is(REGIONAL_MANAGED_INSTANCE_GROUP));
    }

    @Test
    public void constructUnmanagedInstanceGroupUrl() {
        assertThat(InstanceGroupUrl.unmanagedZonal("my-project3", "europe-west1-b", "my-instance-group3").getUrl(),
                is(UNMANAGED_INSTANCE_GROUP));
    }

}
