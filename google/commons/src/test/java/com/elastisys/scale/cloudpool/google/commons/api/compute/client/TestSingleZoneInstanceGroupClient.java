package com.elastisys.scale.cloudpool.google.commons.api.compute.client;

import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.SingleZoneInstanceGroupClient;
import com.google.api.services.compute.Compute;

/**
 * Exercises {@link SingleZoneInstanceGroupClient}.
 */
public class TestSingleZoneInstanceGroupClient {
    private static final String ZONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project1/zones/europe-west1-b/instanceGroupManagers/my-instance-group1";
    private static final String REGIONAL_MANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project2/regions/europe-west1/instanceGroupManagers/my-instance-group2";
    private static final String UNMANAGED_INSTANCE_GROUP = "https://www.googleapis.com/compute/v1/projects/my-project3/zones/europe-west1-b/instanceGroups/my-instance-group3";

    private final Compute client = mock(Compute.class);

    /**
     * The client should work against a single-zone instance group.
     */
    @Test
    public void createFromZonalInstanceGroupUrl() {
        new SingleZoneInstanceGroupClient(this.client, ZONAL_MANAGED_INSTANCE_GROUP);
    }

    /**
     * The client must operate against a single-zone instance group.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createFromRegionalInstanceGroupUrl() {
        new SingleZoneInstanceGroupClient(this.client, REGIONAL_MANAGED_INSTANCE_GROUP);
    }

    /**
     * The client must operate against a managed instance group.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createFromUnmanagedInstanceGroupUrl() {
        new SingleZoneInstanceGroupClient(this.client, UNMANAGED_INSTANCE_GROUP);
    }

}
