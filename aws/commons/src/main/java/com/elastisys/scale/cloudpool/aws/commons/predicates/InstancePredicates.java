package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions.toInstanceId;
import static com.google.common.collect.Collections2.transform;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * {@link Predicate}s that apply to EC2 {@link Instance}s.
 *
 *
 *
 */
public class InstancePredicates {

    /** The range of permissible states an {@link Instance} can be in. */
    private static final ImmutableList<String> VALID_STATES = ImmutableList.of(InstanceStateName.Pending.toString(),
            InstanceStateName.Running.toString(), InstanceStateName.ShuttingDown.toString(),
            InstanceStateName.Stopping.toString(), InstanceStateName.Stopped.toString(),
            InstanceStateName.Terminated.toString());

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any EC2
     * {@link Instance} with a given {@link Tag} set.
     *
     * @param requiredTag
     *            Tag that need to be set on matching {@link Instance}s.
     * @return
     */
    public static Predicate<Instance> hasTag(Tag requiredTag) {
        return new HasTag(requiredTag);
    }

    /**
     * A {@link Predicate} that returns <code>true</code> for any EC2
     * {@link Instance} with a given {@link Tag} set.
     *
     *
     */
    public static class HasTag implements Predicate<Instance> {
        private final Tag requiredTag;

        /**
         * Creates a new {@link HasTag} predicate.
         *
         * @param requiredTag
         *            Tag that needs to be set on matching {@link Instance}s.
         */
        public HasTag(Tag requiredTag) {
            Preconditions.checkNotNull(requiredTag, "requiredTag is null");
            this.requiredTag = requiredTag;
        }

        @Override
        public boolean apply(Instance instance) {
            if (instance == null) {
                return false;
            }
            return instance.getTags().contains(this.requiredTag);
        }
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link Instance} in one of an acceptable set of states.
     *
     * @param acceptableStates
     *            The set of acceptable states.
     * @return
     */
    public static Predicate<Instance> inAnyOfStates(String... acceptableStates) {
        return new InStatePredicate(Arrays.asList(acceptableStates));
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> if every
     * instance in a collection of instances is in a given set of
     * {@link InstanceState}s.
     *
     * @param states
     *            The acceptable states.
     * @return
     */
    public static Predicate<List<Instance>> allInAnyOfStates(final String... states) {
        // validate states
        for (String state : states) {
            Preconditions.checkArgument(VALID_STATES.contains(state), "unrecognized spot instance request state '%s'",
                    state);
        }
        final List<String> expectedStates = ImmutableList.copyOf(states);
        return new Predicate<List<Instance>>() {
            @Override
            public boolean apply(List<Instance> instances) {
                for (Instance instance : instances) {
                    if (!expectedStates.contains(instance.getState().getName())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Predicates that determines if an {@link Instance} is in any of a set of
     * acceptable states.
     */
    public static class InStatePredicate implements Predicate<Instance> {

        /**
         * The collection of instance states we are waiting for the instance to
         * reach.
         */
        private final Collection<String> acceptableStates;

        /**
         * Constructs a new {@link InStatePredicate}.
         *
         * @param acceptableStates
         *            The collection of instance states we are waiting for the
         *            instance to reach.
         */
        public InStatePredicate(Collection<String> acceptableStates) {
            for (String state : acceptableStates) {
                Preconditions.checkArgument(VALID_STATES.contains(state), "unrecognized instance state '%s'", state);
            }

            this.acceptableStates = acceptableStates;
        }

        @Override
        public boolean apply(Instance state) {
            return this.acceptableStates.contains(state.getState().getName());
        }
    }

    /**
     * Returns a {@link Predicate} that determines if a list of instances
     * contain a set of expected member instances
     *
     * @param expectedInstanceIds
     *            The instance identifiers that are expected.
     *
     * @return
     */
    public static Predicate<List<Instance>> instancesPresent(Collection<String> expectedInstanceIds) {
        return new InstancesPresentPredicate(expectedInstanceIds);
    }

    /**
     * Predicate that determines if a list of instances contain a set of
     * expected member instances.
     */
    public static class InstancesPresentPredicate implements Predicate<List<Instance>> {

        /**
         * The set of instance identifiers that are expected to be present.
         */
        private final ImmutableSet<String> expectedIds;

        /**
         * Creates a new instance.
         *
         * @param acceptableStates
         *            The instance identifiers that are expected.
         */
        public InstancesPresentPredicate(final Collection<String> expectedInstancesIds) {
            this.expectedIds = ImmutableSet.copyOf(expectedInstancesIds);
        }

        @Override
        public boolean apply(List<Instance> instances) {
            Collection<String> actualIds = transform(instances, toInstanceId());
            return actualIds.containsAll(this.expectedIds);
        }
    }
}
