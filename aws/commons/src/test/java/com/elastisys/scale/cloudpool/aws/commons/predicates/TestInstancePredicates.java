package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.inAnyOfStates;
import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.instancesPresent;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Exercises the {@link InstancePredicates} class.
 */
public class TestInstancePredicates {

    @Test
    public void testHasTag() {
        Predicate<Instance> hasTag = InstancePredicates
                .hasTag(new Tag().withKey("expectedKey").withValue("expectedValue"));

        Map<String, String> noTags = ImmutableMap.of();
        Instance i0 = makeInstance("i-0", "running", noTags);
        Instance i1 = makeInstance("i-1", "running", ImmutableMap.of("k1", "v1"));
        Instance i2 = makeInstance("i-2", "running", ImmutableMap.of("expectedKey", "expectedValue"));
        Instance i3 = makeInstance("i-3", "running", ImmutableMap.of("k1", "v1", //
                "expectedKey", "expectedValue"));

        assertFalse(hasTag.apply(i0));
        assertFalse(hasTag.apply(i1));
        assertTrue(hasTag.apply(i2));
        assertTrue(hasTag.apply(i3));
    }

    @Test
    public void testInAnyOfStates() {
        Predicate<Instance> noAcceptableStates = inAnyOfStates();
        Predicate<Instance> pendingOnly = inAnyOfStates("pending");
        Predicate<Instance> active = inAnyOfStates("pending", "running");
        Predicate<Instance> terminal = inAnyOfStates("shutting-down", "terminated");

        Map<String, String> noTags = ImmutableMap.of();
        Instance pending = makeInstance("i-0", "pending", noTags);
        Instance running = makeInstance("i-1", "running", noTags);
        Instance stopping = makeInstance("i-2", "stopping", noTags);
        Instance terminating = makeInstance("i-2", "shutting-down", noTags);
        Instance terminated = makeInstance("i-3", "terminated", noTags);

        assertFalse(noAcceptableStates.apply(pending));
        assertFalse(noAcceptableStates.apply(running));
        assertFalse(noAcceptableStates.apply(stopping));
        assertFalse(noAcceptableStates.apply(terminating));
        assertFalse(noAcceptableStates.apply(terminated));

        assertTrue(pendingOnly.apply(pending));
        assertFalse(pendingOnly.apply(running));
        assertFalse(pendingOnly.apply(stopping));
        assertFalse(pendingOnly.apply(terminating));
        assertFalse(pendingOnly.apply(terminated));

        assertTrue(active.apply(pending));
        assertTrue(active.apply(running));
        assertFalse(active.apply(stopping));
        assertFalse(active.apply(terminating));
        assertFalse(active.apply(terminated));

        assertFalse(terminal.apply(pending));
        assertFalse(terminal.apply(running));
        assertFalse(terminal.apply(stopping));
        assertTrue(terminal.apply(terminating));
        assertTrue(terminal.apply(terminated));
    }

    @Test(expected = IllegalArgumentException.class)
    public void inAnyOfStatesWithIllegalArgument() {
        inAnyOfStates("bad-state");
    }

    /**
     * Test the {@link InstancePredicates#allInAnyOfStates(String...)} predicate
     * when there is only a single state to match {@link Instance}s against.
     */
    @Test
    public void testAllInAnyOfStatesPredicateWithSingleMatchingState() {
        // make sure all valid states are recognized
        assertTrue(InstancePredicates.allInAnyOfStates("pending").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("pending").apply(instances("pending")));
        assertTrue(InstancePredicates.allInAnyOfStates("pending").apply(instances("pending", "pending")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending").apply(instances("pending", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("running").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("running").apply(instances("running")));
        assertTrue(InstancePredicates.allInAnyOfStates("running").apply(instances("running", "running")));
        assertFalse(InstancePredicates.allInAnyOfStates("running").apply(instances("running", "pending")));

        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down").apply(instances("shutting-down")));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down")
                .apply(instances("shutting-down", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("shutting-down").apply(instances("shutting-down", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("stopping").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("stopping").apply(instances("stopping")));
        assertTrue(InstancePredicates.allInAnyOfStates("stopping").apply(instances("stopping", "stopping")));
        assertFalse(InstancePredicates.allInAnyOfStates("stopping").apply(instances("stopping", "pending")));

        assertTrue(InstancePredicates.allInAnyOfStates("stopped").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("stopped").apply(instances("stopped")));
        assertTrue(InstancePredicates.allInAnyOfStates("stopped").apply(instances("stopped", "stopped")));
        assertFalse(InstancePredicates.allInAnyOfStates("stopped").apply(instances("stopped", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("terminated").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("terminated").apply(instances("terminated")));
        assertTrue(InstancePredicates.allInAnyOfStates("terminated").apply(instances("terminated", "terminated")));
        assertFalse(InstancePredicates.allInAnyOfStates("terminated").apply(instances("terminated", "running")));
    }

    /**
     * Test the {@link InstancePredicates#allInAnyOfStates(String...)} predicate
     * when there are more than one state to match {@link Instance}s against.
     */
    @Test
    public void testAllInAnyOfStatesPredicateWithMultipleMatchingStates() {
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated").apply(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .apply(instances("shutting-down", "terminated")));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .apply(instances("shutting-down", "terminated", "terminated", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .apply(instances("shutting-down", "terminated", "running", "shutting-down")));

        assertTrue(InstancePredicates.allInAnyOfStates("pending", "running")
                .apply(instances("pending", "running", "pending", "running")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .apply(instances("pending", "running", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .apply(instances("pending", "running", "pending", "stopped")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .apply(instances("pending", "running", "terminated", "stopped")));
    }

    @Test
    public void testInstancesPresent() {
        Map<String, String> noTags = ImmutableMap.of();
        Instance i0 = makeInstance("i-0", "pending", noTags);
        Instance i1 = makeInstance("i-1", "running", noTags);
        Instance i2 = makeInstance("i-2", "running", noTags);
        Instance i3 = makeInstance("i-3", "shutting-down", noTags);

        List<Instance> noInstances = Arrays.asList();

        List<String> expectedIds = Arrays.asList();
        assertTrue(instancesPresent(expectedIds).apply(noInstances));
        expectedIds = asList("i-0");
        assertFalse(instancesPresent(expectedIds).apply(noInstances));
        assertFalse(instancesPresent(expectedIds).apply(asList(i1)));
        assertTrue(instancesPresent(expectedIds).apply(asList(i0)));
        assertTrue(instancesPresent(expectedIds).apply(asList(i0, i1)));
        assertFalse(instancesPresent(expectedIds).apply(asList(i1, i2)));

        expectedIds = asList("i-0", "i-3");
        assertFalse(instancesPresent(expectedIds).apply(noInstances));
        assertFalse(instancesPresent(expectedIds).apply(asList(i1)));
        assertFalse(instancesPresent(expectedIds).apply(asList(i0)));
        assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1)));
        assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1)));
        assertTrue(instancesPresent(expectedIds).apply(asList(i0, i3)));
        assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1, i2)));
        assertTrue(instancesPresent(expectedIds).apply(asList(i0, i1, i2, i3)));
    }

    private Instance makeInstance(String id, String state, Map<String, String> tags) {
        List<Tag> tagList = Lists.newArrayList();
        for (Entry<String, String> tagEntry : tags.entrySet()) {
            tagList.add(new Tag().withKey(tagEntry.getKey()).withValue(tagEntry.getValue()));
        }

        return new Instance().withInstanceId(id).withState(new InstanceState().withName(state)).withTags(tagList);
    }

    private List<Instance> instances(String... states) {
        List<Instance> instances = Lists.newArrayList();
        int index = 1;
        for (String state : states) {
            instances.add(new Instance().withInstanceId("i-" + index++).withState(new InstanceState().withName(state)));
        }
        return instances;
    }
}
