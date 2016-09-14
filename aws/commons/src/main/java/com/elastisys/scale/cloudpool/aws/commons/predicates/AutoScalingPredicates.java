package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static com.google.common.base.Preconditions.checkArgument;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.google.common.base.Predicate;

/**
 * A factory class for {@link Predicate}s relating to the Amazon Auto Scaling
 * API.
 */
public class AutoScalingPredicates {

    private AutoScalingPredicates() {
        throw new UnsupportedOperationException("cannot instantiate");
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any EC2
     * {@link Instance} with a given instance identifier.
     *
     * @param instanceId
     *            The instance identifier to match {@link Instance}s against.
     * @return
     */
    public static Predicate<AutoScalingGroup> autoScalingGroupSize(int expectedSize) {
        return new AutoScalingGroupSizePredicate(expectedSize);
    }

    /**
     * A {@link Predicate} that returns <code>true</code> for an
     * {@link AutoScalingGroup} that has an expected size.
     */
    public static class AutoScalingGroupSizePredicate implements Predicate<AutoScalingGroup> {
        private final int expectedSize;

        public AutoScalingGroupSizePredicate(int expectedSize) {
            checkArgument(expectedSize >= 0, "expected group size cannot be negative");
            this.expectedSize = expectedSize;
        }

        @Override
        public boolean apply(AutoScalingGroup autoScalingGroup) {
            return autoScalingGroup.getInstances().size() == this.expectedSize;
        }
    }
}
