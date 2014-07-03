package com.elastisys.scale.cloudadapters.aws.commons.predicates;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

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
	public static Predicate<? super Instance> hasTag(Tag requiredTag) {
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

}
