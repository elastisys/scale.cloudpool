package com.elastisys.scale.cloudadapters.aws.commons.predicates;

import com.amazonaws.services.ec2.model.Instance;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * A factory class for {@link Predicate}s relating to the Amazon API.
 *
 * 
 */
public class AwsEc2Predicates {

	private AwsEc2Predicates() {
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
	public static Predicate<? super Instance> ec2InstanceIdEquals(
			String instanceId) {
		return new Ec2InstanceIdEqual(instanceId);
	}

	/**
	 * A {@link Predicate} that returns <code>true</code> for any EC2
	 * {@link Instance} with a given instance identifier.
	 *
	 * 
	 */
	public static class Ec2InstanceIdEqual implements Predicate<Instance> {
		private final String instanceId;

		/**
		 * Constructs a new {@link Ec2InstanceIdEqual} {@link Predicate}.
		 *
		 * @param instanceId
		 *            The instance identifier to match {@link Instance}s
		 *            against.
		 */
		public Ec2InstanceIdEqual(String instanceId) {
			Preconditions.checkNotNull(instanceId, "instanceId is null");
			this.instanceId = instanceId;
		}

		@Override
		public boolean apply(Instance instance) {
			if (instance == null) {
				return false;
			}
			return Objects.equal(this.instanceId, instance.getInstanceId());
		}
	}

}
