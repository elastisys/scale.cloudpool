package com.elastisys.scale.cloudpool.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercise the {@link CloudPoolStatus} class.
 */
public class TestCloudPoolStatus {

    @Test
    public void basicSanity() {
        CloudPoolStatus status1 = new CloudPoolStatus(true, true);
        assertThat(status1.isStarted(), is(true));
        assertThat(status1.isConfigured(), is(true));

        CloudPoolStatus status2 = new CloudPoolStatus(true, false);
        assertThat(status2.isStarted(), is(true));
        assertThat(status2.isConfigured(), is(false));

        CloudPoolStatus status3 = new CloudPoolStatus(false, true);
        assertThat(status3.isStarted(), is(false));
        assertThat(status3.isConfigured(), is(true));

        CloudPoolStatus status4 = new CloudPoolStatus(false, false);
        assertThat(status4.isStarted(), is(false));
        assertThat(status4.isConfigured(), is(false));

        assertEquals(status1, new CloudPoolStatus(true, true));
        assertEquals(status2, new CloudPoolStatus(true, false));
        assertEquals(status3, new CloudPoolStatus(false, true));
        assertEquals(status4, new CloudPoolStatus(false, false));

        assertNotEquals(status1, status2);
        assertNotEquals(status1, status3);
        assertNotEquals(status1, status4);
        assertNotEquals(status2, status3);
        assertNotEquals(status2, status4);
        assertNotEquals(status3, status4);
    }

    @Test
    public void constants() {
        assertThat(CloudPoolStatus.UNCONFIGURED_STOPPED, is(new CloudPoolStatus(false, false)));
        assertThat(CloudPoolStatus.CONFIGURED_STOPPED, is(new CloudPoolStatus(false, true)));
        assertThat(CloudPoolStatus.CONFIGURED_STARTED, is(new CloudPoolStatus(true, true)));
    }
}
