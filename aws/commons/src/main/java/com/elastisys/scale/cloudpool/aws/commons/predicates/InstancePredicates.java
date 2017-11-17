package com.elastisys.scale.cloudpool.aws.commons.predicates;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Preconditions;

/**
 * {@link Predicate}s that apply to EC2 {@link Instance}s.
 */
public class InstancePredicates {

    /** The range of permissible states an {@link Instance} can be in. */
    private static final List<String> VALID_STATES = Arrays.asList(InstanceStateName.Pending.toString(),
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
        public boolean test(Instance instance) {
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
        List<String> expectedStates = Arrays.asList(states);
        return instances -> {
            for (Instance instance : instances) {
                if (!expectedStates.contains(instance.getState().getName())) {
                    return false;
                }
            }
            return true;
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
        public boolean test(Instance state) {
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
        private final Set<String> expectedIds;

        /**
         * Creates a new instance.
         *
         * @param acceptableStates
         *            The instance identifiers that are expected.
         */
        public InstancesPresentPredicate(Collection<String> expectedInstancesIds) {
            this.expectedIds = new HashSet<>(expectedInstancesIds);
        }

        @Override
        public boolean test(List<Instance> instances) {
            Collection<String> actualIds = instances.stream().map(Instance::getInstanceId).collect(Collectors.toList());
            return actualIds.containsAll(this.expectedIds);
        }
    }
}
