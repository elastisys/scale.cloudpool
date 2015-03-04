package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions.toInstanceId;
import static com.google.common.collect.Collections2.transform;

import java.util.Collection;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * {@link Predicate}s that apply to EC2 {@link Instance}s.
 *
 *
 *
 */
public class InstancePredicates {

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
	public static Predicate<Instance> instanceStateIn(
			Collection<String> acceptableStates) {
		return new InstanceStatePredicate(acceptableStates);
	}

	/**
	 * Predicates that determines if an {@link Instance} is in any of a set of
	 * acceptable states.
	 */
	public static class InstanceStatePredicate implements Predicate<Instance> {

		/**
		 * The collection of instance states we are waiting for the instance to
		 * reach.
		 */
		private final Collection<String> acceptableStates;

		/**
		 * Constructs a new {@link InstanceStatePredicate}.
		 *
		 * @param acceptableStates
		 *            The collection of instance states we are waiting for the
		 *            instance to reach.
		 */
		public InstanceStatePredicate(Collection<String> acceptableStates) {
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
	public static Predicate<List<Instance>> instancesPresent(
			Collection<String> expectedInstanceIds) {
		return new InstancesPresentPredicate(expectedInstanceIds);
	}

	/**
	 * Predicate that determines if a list of instances contain a set of
	 * expected member instances.
	 */
	public static class InstancesPresentPredicate implements
			Predicate<List<Instance>> {

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
		public InstancesPresentPredicate(
				final Collection<String> expectedInstancesIds) {
			this.expectedIds = ImmutableSet.copyOf(expectedInstancesIds);
		}

		@Override
		public boolean apply(List<Instance> instances) {
			Collection<String> actualIds = transform(instances, toInstanceId());
			return actualIds.containsAll(this.expectedIds);
		}
	}
}
