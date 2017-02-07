package com.elastisys.scale.cloudpool.google.commons.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.commons.utils.ZoneUtils;

/**
 * Exercise {@link ZoneUtils}.
 */
public class TestZoneUtils {

    /** Sample zone URL. */
    private static final String ZONE_URL = "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-b";
    /** Sample zone name. */
    private static final String ZONE_NAME = "europe-west1-b";

    /**
     * It should be possible to parse out zone name from a zone URL.
     */
    @Test
    public void parseZoneName() {
        assertThat(ZoneUtils.zoneName(ZONE_URL), is(ZONE_NAME));
    }

    /**
     * It should be possible to parse out region name from a zone URL.
     */
    @Test
    public void parseRegionName() {
        assertThat(ZoneUtils.regionName(ZONE_NAME), is("europe-west1"));
    }

    /**
     * It should be possible to parse out region name from a zone URL.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseIllegalRegionName() {
        // zone missing zone-qualifier (for example, "us-east1-b")
        ZoneUtils.regionName("us-east1");
    }

}
