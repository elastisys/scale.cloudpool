package com.elastisys.scale.cloudpool.google.commons.api.compute.client;

import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.MultiZoneInstanceGroupClient;
import com.google.api.services.compute.Compute;

/**
 * Exercises {@link MultiZoneInstanceGroupClient}.
 */
public class TestMultiZoneInstanceGroupClient {
    private static final String ZONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project1/zones/europe-west1-b/instanceGroupManagers/my-instance-group1";
    private static final String REGIONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project2/regions/europe-west1/instanceGroupManagers/my-instance-group2";
    private static final String UNMANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project3/zones/europe-west1-b/instanceGroups/my-instance-group3";

    private final Compute client = mock(Compute.class);

    /**
     * The client should work against a multi-zone instance group.
     */
    @Test
    public void createFromRegionalInstanceGroupUrl() {
        new MultiZoneInstanceGroupClient(this.client, REGIONAL_MANAGED_INSTANCE_GROUP);
    }

    /**
     * The client must operate against a multi-zone instance group.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createFromZonalInstanceGroupUrl() {
        new MultiZoneInstanceGroupClient(this.client, ZONAL_MANAGED_INSTANCE_GROUP);
    }

    /**
     * The client must operate against a managed instance group.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createFromUnmanagedInstanceGroupUrl() {
        new MultiZoneInstanceGroupClient(this.client, UNMANAGED_INSTANCE_GROUP);
    }

}
