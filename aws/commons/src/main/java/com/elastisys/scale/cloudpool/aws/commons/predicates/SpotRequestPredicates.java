package com.elastisys.scale.cloudpool.aws.commons.predicates;

import java.util.Arrays;

import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceState;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * {@link Predicate}s relating to AWS EC2 spot instance requests.
 */
public class SpotRequestPredicates {

	/** The range of permissible states a {@link SpotInstanceRequest} can be in. */
	private static final ImmutableList<String> VALID_STATES = ImmutableList.of(
			SpotInstanceState.Active.toString(),
			SpotInstanceState.Cancelled.toString(),
			SpotInstanceState.Closed.toString(),
			SpotInstanceState.Failed.toString(),
			SpotInstanceState.Open.toString());

	/**
	 * Returns a {@link Predicate} that indicates if a
	 * {@link SpotInstanceRequest} is in any of a set of
	 * {@link SpotInstanceState}s.
	 *
	 * @param states
	 * @return
	 */
	public static Predicate<SpotInstanceRequest> stateIn(final String... states) {
		// validate states
		for (String state : states) {
			Preconditions.checkArgument(VALID_STATES.contains(state),
					"unrecognized spot instance request state '%s'", state);
		}
		return new Predicate<SpotInstanceRequest>() {
			@Override
			public boolean apply(SpotInstanceRequest spotRequest) {
				return Arrays.asList(states).contains(spotRequest.getState());
			}
		};
	}
}
