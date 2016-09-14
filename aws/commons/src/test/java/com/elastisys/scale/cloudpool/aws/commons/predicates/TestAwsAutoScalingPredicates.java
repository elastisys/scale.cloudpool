package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * Exercises {@link AutoScalingPredicates}.
 */
public class TestAwsAutoScalingPredicates {

    /**
     * Exercise {@link AutoScalingPredicates#autoScalingGroupSize(int)}
     */
    @Test
    public void testAutoScalingGroupSizePredicate() {
        Predicate<AutoScalingGroup> groupSizePredicate = AutoScalingPredicates.autoScalingGroupSize(10);

        assertFalse(groupSizePredicate.apply(makeGroup(0)));
        assertFalse(groupSizePredicate.apply(makeGroup(1)));
        assertFalse(groupSizePredicate.apply(makeGroup(5)));
        assertTrue(groupSizePredicate.apply(makeGroup(10)));
        assertFalse(groupSizePredicate.apply(makeGroup(11)));
        assertFalse(groupSizePredicate.apply(makeGroup(50)));
    }

    private AutoScalingGroup makeGroup(int members) {
        List<Instance> groupMembers = Lists.newArrayList();
        for (int i = 0; i < members; i++) {
            groupMembers.add(new Instance().withInstanceId("i-" + i));
        }

        return new AutoScalingGroup().withInstances(groupMembers);
    }
}
