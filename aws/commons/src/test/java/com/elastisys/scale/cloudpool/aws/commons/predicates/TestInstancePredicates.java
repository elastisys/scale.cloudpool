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
import java.util.function.Predicate;

import org.junit.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
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

        assertFalse(hasTag.test(i0));
        assertFalse(hasTag.test(i1));
        assertTrue(hasTag.test(i2));
        assertTrue(hasTag.test(i3));
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

        assertFalse(noAcceptableStates.test(pending));
        assertFalse(noAcceptableStates.test(running));
        assertFalse(noAcceptableStates.test(stopping));
        assertFalse(noAcceptableStates.test(terminating));
        assertFalse(noAcceptableStates.test(terminated));

        assertTrue(pendingOnly.test(pending));
        assertFalse(pendingOnly.test(running));
        assertFalse(pendingOnly.test(stopping));
        assertFalse(pendingOnly.test(terminating));
        assertFalse(pendingOnly.test(terminated));

        assertTrue(active.test(pending));
        assertTrue(active.test(running));
        assertFalse(active.test(stopping));
        assertFalse(active.test(terminating));
        assertFalse(active.test(terminated));

        assertFalse(terminal.test(pending));
        assertFalse(terminal.test(running));
        assertFalse(terminal.test(stopping));
        assertTrue(terminal.test(terminating));
        assertTrue(terminal.test(terminated));
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
        assertTrue(InstancePredicates.allInAnyOfStates("pending").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("pending").test(instances("pending")));
        assertTrue(InstancePredicates.allInAnyOfStates("pending").test(instances("pending", "pending")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending").test(instances("pending", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("running").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("running").test(instances("running")));
        assertTrue(InstancePredicates.allInAnyOfStates("running").test(instances("running", "running")));
        assertFalse(InstancePredicates.allInAnyOfStates("running").test(instances("running", "pending")));

        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down").test(instances("shutting-down")));
        assertTrue(
                InstancePredicates.allInAnyOfStates("shutting-down").test(instances("shutting-down", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("shutting-down").test(instances("shutting-down", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("stopping").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("stopping").test(instances("stopping")));
        assertTrue(InstancePredicates.allInAnyOfStates("stopping").test(instances("stopping", "stopping")));
        assertFalse(InstancePredicates.allInAnyOfStates("stopping").test(instances("stopping", "pending")));

        assertTrue(InstancePredicates.allInAnyOfStates("stopped").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("stopped").test(instances("stopped")));
        assertTrue(InstancePredicates.allInAnyOfStates("stopped").test(instances("stopped", "stopped")));
        assertFalse(InstancePredicates.allInAnyOfStates("stopped").test(instances("stopped", "running")));

        assertTrue(InstancePredicates.allInAnyOfStates("terminated").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("terminated").test(instances("terminated")));
        assertTrue(InstancePredicates.allInAnyOfStates("terminated").test(instances("terminated", "terminated")));
        assertFalse(InstancePredicates.allInAnyOfStates("terminated").test(instances("terminated", "running")));
    }

    /**
     * Test the {@link InstancePredicates#allInAnyOfStates(String...)} predicate
     * when there are more than one state to match {@link Instance}s against.
     */
    @Test
    public void testAllInAnyOfStatesPredicateWithMultipleMatchingStates() {
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated").test(instances()));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .test(instances("shutting-down", "terminated")));
        assertTrue(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .test(instances("shutting-down", "terminated", "terminated", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("shutting-down", "terminated")
                .test(instances("shutting-down", "terminated", "running", "shutting-down")));

        assertTrue(InstancePredicates.allInAnyOfStates("pending", "running")
                .test(instances("pending", "running", "pending", "running")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .test(instances("pending", "running", "shutting-down")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .test(instances("pending", "running", "pending", "stopped")));
        assertFalse(InstancePredicates.allInAnyOfStates("pending", "running")
                .test(instances("pending", "running", "terminated", "stopped")));
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
        assertTrue(instancesPresent(expectedIds).test(noInstances));
        expectedIds = asList("i-0");
        assertFalse(instancesPresent(expectedIds).test(noInstances));
        assertFalse(instancesPresent(expectedIds).test(asList(i1)));
        assertTrue(instancesPresent(expectedIds).test(asList(i0)));
        assertTrue(instancesPresent(expectedIds).test(asList(i0, i1)));
        assertFalse(instancesPresent(expectedIds).test(asList(i1, i2)));

        expectedIds = asList("i-0", "i-3");
        assertFalse(instancesPresent(expectedIds).test(noInstances));
        assertFalse(instancesPresent(expectedIds).test(asList(i1)));
        assertFalse(instancesPresent(expectedIds).test(asList(i0)));
        assertFalse(instancesPresent(expectedIds).test(asList(i0, i1)));
        assertFalse(instancesPresent(expectedIds).test(asList(i0, i1)));
        assertTrue(instancesPresent(expectedIds).test(asList(i0, i3)));
        assertFalse(instancesPresent(expectedIds).test(asList(i0, i1, i2)));
        assertTrue(instancesPresent(expectedIds).test(asList(i0, i1, i2, i3)));
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
