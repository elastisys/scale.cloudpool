package com.elastisys.scale.cloudpool.aws.commons.functions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * A factory class for {@link Function}s relating to the Amazon API.
 * 
 * 
 * 
 */
public class AwsEc2Functions {

	/**
	 * Returns a {@link Function} that for a given EC2 {@link Instance} input
	 * returns its identity.
	 * 
	 * @return
	 */
	public static Function<Instance, String> toInstanceId() {
		return new ToInstanceId();
	}

	/**
	 * Returns a {@link Function} that for a given {@link Instance} calculates
	 * the remaining time (in seconds) of the instance's last started billing
	 * hour.
	 * 
	 * @param timeSource
	 * @return
	 */
	public static Function<Instance, Long> remainingBillingHourTime() {
		return new RemainingBillingHourTime();
	}

	/**
	 * A {@link Function} that for a given {@link Instance} input returns its
	 * identity.
	 * 
	 * 
	 */
	public static class ToInstanceId implements Function<Instance, String> {
		@Override
		public String apply(Instance instance) {
			Preconditions.checkNotNull(instance, "null instance");
			return instance.getInstanceId();
		}
	}

	/**
	 * A {@link Function} that for a given
	 * {@link com.amazonaws.services.autoscaling.model.Instance} input returns
	 * the name of its identifier.
	 * 
	 * 
	 */
	public static class ToAutoScalingInstanceId implements
			Function<com.amazonaws.services.autoscaling.model.Instance, String> {
		@Override
		public String apply(
				com.amazonaws.services.autoscaling.model.Instance instance) {
			Preconditions.checkNotNull(instance, "null instance");
			return instance.getInstanceId();
		}
	}

	/**
	 * A {@link Function} that for a given {@link Instance} calculates the
	 * remaining time (in seconds) of the instance's last started billing hour.
	 * 
	 * 
	 */
	public static class RemainingBillingHourTime implements
			Function<Instance, Long> {

		/**
		 * Calculates the remaining time (in seconds) of the instance's last
		 * started billing hour.
		 * 
		 * @param instance
		 * @return
		 */
		@Override
		public Long apply(Instance instance) {
			checkNotNull(instance, "null instance");
			long executionTimeInSeconds = (UtcTime.now().getMillis() - instance
					.getLaunchTime().getTime()) / 1000;
			long secondsIntoPrepaidHour = executionTimeInSeconds % 3600;
			long secondsLeftOfPrepaidHour = 3600 - secondsIntoPrepaidHour;
			return secondsLeftOfPrepaidHour;
		}
	}
}
