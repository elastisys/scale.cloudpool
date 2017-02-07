package com.elastisys.scale.cloudpool.google.compute.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.compute.driver.config.ProvisioningTemplate;

/**
 * Exercises the {@link ProvisioningTemplate}.
 */
public class TestProvisioningTemplate {

    /** Sample instance group name. */
    private static final String INSTANCE_GROUP = "my-instancegroup";
    /** Sample project name. */
    private static final String PROJECT = "my-project";
    /** Sample zone. */
    private static final String ZONE = "europe-west1-d";
    /** Sample region. */
    private static final String REGION = "europe-west1";

    /**
     * It should be possible to specify a single-zone instance group.
     */
    @Test
    public void zonalInstanceGroup() {
        ProvisioningTemplate template = new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, null, ZONE);
        template.validate();

        assertThat(template.getInstanceGroup(), is(INSTANCE_GROUP));
        assertThat(template.getProject(), is(PROJECT));
        assertThat(template.isSingleZoneGroup(), is(true));
        assertThat(template.getZone(), is(ZONE));
        assertThat(template.getRegion(), is(nullValue()));
    }

    /**
     * It should be possible to specify a multi-zone instance group.
     */
    @Test
    public void regionalInstanceGroup() {
        ProvisioningTemplate template = new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, REGION, null);
        template.validate();

        assertThat(template.getInstanceGroup(), is(INSTANCE_GROUP));
        assertThat(template.getProject(), is(PROJECT));
        assertThat(template.isSingleZoneGroup(), is(false));
        assertThat(template.getZone(), is(nullValue()));
        assertThat(template.getRegion(), is(REGION));
    }

    /**
     * It is an error to neither specify a region nor a zone for the instance
     * group.
     */
    @Test
    public void neitherRegionalNorZonalInstanceGroup() {
        String region = null;
        String zone = null;
        try {
            new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, region, zone).validate();
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(
                    "either region (for a multi-zone instance group) or zone (for a single-zone instance group) must be given"));
        }
    }

    /**
     * An instance group cannot be both regional and zonal.
     */
    @Test
    public void regionalAndZonalInstanceGroup() {
        try {
            new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, REGION, ZONE).validate();
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(
                    "only one of region (for a multi-zone instance group) and zone (for a single-zone instance group) may be specified"));
        }
    }

    /**
     * instanceGroup name must be specified
     */
    @Test
    public void missingInstanceGroup() {
        try {
            new ProvisioningTemplate(null, PROJECT, REGION, null).validate();
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("instanceGroup"));
        }
    }

    /**
     * project name must be specified
     */
    @Test
    public void missingProject() {
        try {
            new ProvisioningTemplate(INSTANCE_GROUP, null, REGION, null).validate();
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("project"));
        }
    }

}
