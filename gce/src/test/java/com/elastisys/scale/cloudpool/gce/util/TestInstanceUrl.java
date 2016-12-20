package com.elastisys.scale.cloudpool.gce.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link InstanceUrl} class.
 *
 */
public class TestInstanceUrl {

    /** Sample instance URL. */
    private static final String INSTANCE_URL = "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0";

    @Test
    public void parseProject() {
        assertThat(new InstanceUrl(INSTANCE_URL).getProject(), is("my-project"));
    }

    @Test
    public void parseZone() {
        assertThat(new InstanceUrl(INSTANCE_URL).getZone(), is("europe-west1-d"));
    }

    @Test
    public void parseName() {
        assertThat(new InstanceUrl(INSTANCE_URL).getName(), is("webserver-s4s0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInstanceUrlMissingProject() {
        new InstanceUrl("https://www.googleapis.com/compute/v1/projects/zones/europe-west1-d/instances/webserver-s4s0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInstanceUrlMissingZone() {
        new InstanceUrl("https://www.googleapis.com/compute/v1/projects/my-project/zones/instances/webserver-s4s0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInstanceUrlMissingInstanceName() {
        new InstanceUrl("https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances");
    }

}
