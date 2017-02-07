package com.elastisys.scale.cloudpool.google.container.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;

/**
 * Exercises {@link InstanceGroupSnapshot}.
 */
public class TestInstanceGroupSnapshot {

    /**
     * Make sure getters return proper objects.
     */
    @Test
    public void basicSanity() {
        InstanceGroupSnapshot instanceGroupSnapshot = new InstanceGroupSnapshot(metadata(),
                Arrays.asList(instance("i1"), instance("i2")));

        assertThat(instanceGroupSnapshot.getMetadata(), is(metadata()));
        assertThat(instanceGroupSnapshot.getInstances(), is(Arrays.asList(instance("i1"), instance("i2"))));
    }

    /**
     * Only metadata is mandatory, defaults are provided for the other fields.
     */
    @Test
    public void defaults() {
        List<Instance> nullInstances = null;
        InstanceGroupSnapshot instanceGroupSnapshot = new InstanceGroupSnapshot(metadata(), nullInstances);

        // check default values
        assertThat(instanceGroupSnapshot.getInstances(), is(Collections.emptyList()));
    }

    /**
     * {@link InstanceGroupManager} requires metadata to be set.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithoutMetadata() {
        InstanceGroupManager nullMetadata = null;
        new InstanceGroupSnapshot(nullMetadata, Collections.emptyList());
    }

    public Instance instance(String name) {
        return new Instance().setName(name);
    }

    private InstanceGroupManager metadata() {
        return new InstanceGroupManager().setName("my-instance-group");
    }
}
