package com.elastisys.scale.cloudpool.aws.commons.functions;

import com.amazonaws.services.autoscaling.model.Instance;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * A factory class for {@link Function}s relating to the Amazon Auto Scaling
 * API.
 *
 * 
 *
 */
public class AwsAutoScalingFunctions {

	/**
	 * Returns a {@link Function} that for a given Auto Scaling {@link Instance}
	 * input returns its identity.
	 *
	 * @return
	 */
	public static Function<Instance, String> toAutoScalingInstanceId() {
		return new ToAutoScalingInstanceId();
	}

	/**
	 * A {@link Function} that for a given Auto Scaling {@link Instance} input
	 * returns the name of its identifier.
	 *
	 * 
	 */
	public static class ToAutoScalingInstanceId implements
			Function<Instance, String> {
		@Override
		public String apply(Instance instance) {
			Preconditions.checkNotNull(instance, "null instance");
			return instance.getInstanceId();
		}
	}

}
