package com.elastisys.scale.cloudpool.aws.commons.predicates;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceState;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * {@link Predicate}s relating to AWS EC2 spot instance requests.
 */
public class SpotRequestPredicates {

    /**
     * The range of permissible states a {@link SpotInstanceRequest} can be in.
     */
    private static final List<String> VALID_STATES = Arrays.asList(SpotInstanceState.Active.toString(),
            SpotInstanceState.Cancelled.toString(), SpotInstanceState.Closed.toString(),
            SpotInstanceState.Failed.toString(), SpotInstanceState.Open.toString());

    /**
     * Returns a {@link Predicate} that indicates if a
     * {@link SpotInstanceRequest} is in any of a set of
     * {@link SpotInstanceState}s.
     *
     * @param states
     * @return
     */
    public static Predicate<SpotInstanceRequest> inAnyOfStates(final String... states) {
        // validate states
        for (String state : states) {
            Preconditions.checkArgument(VALID_STATES.contains(state), "unrecognized spot instance request state '%s'",
                    state);
        }
        return spotRequest -> Arrays.asList(states).contains(spotRequest.getState());
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> if every spot
     * instance request in a collection of spot instance requests is in a given
     * set of {@link SpotInstanceState}s.
     *
     * @param states
     *            The acceptable states.
     * @return
     */
    public static Predicate<List<SpotInstanceRequest>> allInAnyOfStates(final String... states) {
        // validate states
        for (String state : states) {
            Preconditions.checkArgument(VALID_STATES.contains(state), "unrecognized spot instance request state '%s'",
                    state);
        }
        List<String> expectedStates = Arrays.asList(states);
        return spotRequests -> {
            for (SpotInstanceRequest request : spotRequests) {
                if (!expectedStates.contains(request.getState())) {
                    return false;
                }
            }
            return true;
        };
    }
}
