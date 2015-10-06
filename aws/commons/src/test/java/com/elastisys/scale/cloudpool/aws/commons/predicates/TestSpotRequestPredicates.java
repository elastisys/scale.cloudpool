package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.amazonaws.services.ec2.model.SpotInstanceRequest;

public class TestSpotRequestPredicates {

	/**
	 * Test the {@link SpotRequestPredicates#inAnyOfStates(String...)} predicate
	 * when there is only a single state to match {@link SpotInstanceRequest}s
	 * against.
	 */
	@Test
	public void testInAnyOfStatesPredicateWithSingleMatchingState() {
		// make sure all valid states are recognized
		assertTrue(SpotRequestPredicates.inAnyOfStates("active").apply(
				spotRequest("sir-1", "active")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("closed").apply(
				spotRequest("sir-1", "closed")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("cancelled").apply(
				spotRequest("sir-1", "cancelled")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("failed").apply(
				spotRequest("sir-1", "failed")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("open").apply(
				spotRequest("sir-1", "open")));
	}

	/**
	 * Test the {@link SpotRequestPredicates#inAnyOfStates(String...)} predicate
	 * when there are more than one state to match {@link SpotInstanceRequest}s
	 * against.
	 */
	@Test
	public void testInAnyOfStatesPredicateWithMultipleMatchingStates() {

		assertTrue(SpotRequestPredicates.inAnyOfStates("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "closed")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "cancelled")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "failed")));
		assertFalse(SpotRequestPredicates.inAnyOfStates("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "active")));
		assertFalse(SpotRequestPredicates.inAnyOfStates("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "open")));

		assertTrue(SpotRequestPredicates.inAnyOfStates("active", "open").apply(
				spotRequest("sir-1", "active")));
		assertTrue(SpotRequestPredicates.inAnyOfStates("active", "open").apply(
				spotRequest("sir-1", "open")));
		assertFalse(SpotRequestPredicates.inAnyOfStates("active", "open")
				.apply(spotRequest("sir-1", "closed")));
		assertFalse(SpotRequestPredicates.inAnyOfStates("active", "open")
				.apply(spotRequest("sir-1", "cancelled")));
		assertFalse(SpotRequestPredicates.inAnyOfStates("active", "open")
				.apply(spotRequest("sir-1", "failed")));
	}

	/**
	 * Should not be possible to create a
	 * {@link SpotRequestPredicates#inAnyOfStates(String...)} with an illegal
	 * {@link SpotInstanceRequest} state.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInAnyOfStatesPredicateWithIllegalState() {
		SpotRequestPredicates.inAnyOfStates("badstate");
	}

	/**
	 * Test the {@link SpotRequestPredicates#allInAnyOfStates(String...)}
	 * predicate when there is only a single state to match
	 * {@link SpotInstanceRequest}s against.
	 */
	@Test
	public void testAllInAnyOfStatesPredicateWithSingleMatchingState() {
		// make sure all valid states are recognized
		assertTrue(SpotRequestPredicates.allInAnyOfStates("active").apply(
				spotRequests()));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("active").apply(
				spotRequests("active")));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("active").apply(
				spotRequests("active", "active")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("active").apply(
				spotRequests("active", "open")));

		assertTrue(SpotRequestPredicates.allInAnyOfStates("closed").apply(
				spotRequests()));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("closed").apply(
				spotRequests("closed")));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("closed").apply(
				spotRequests("closed", "closed")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("closed").apply(
				spotRequests("closed", "open")));

		assertTrue(SpotRequestPredicates.allInAnyOfStates("cancelled").apply(
				spotRequests()));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("cancelled").apply(
				spotRequests("cancelled")));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("cancelled").apply(
				spotRequests("cancelled", "cancelled")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("cancelled").apply(
				spotRequests("cancelled", "open")));

		assertTrue(SpotRequestPredicates.allInAnyOfStates("failed").apply(
				spotRequests()));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("failed").apply(
				spotRequests("failed")));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("failed").apply(
				spotRequests("failed", "failed")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("failed").apply(
				spotRequests("failed", "open")));

		assertTrue(SpotRequestPredicates.allInAnyOfStates("open").apply(
				spotRequests()));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("open").apply(
				spotRequests("open")));
		assertTrue(SpotRequestPredicates.allInAnyOfStates("open").apply(
				spotRequests("open", "open")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("open").apply(
				spotRequests("open", "cancelled")));
	}

	/**
	 * Test the {@link SpotRequestPredicates#allInAnyOfStates(String...)}
	 * predicate when there are more than one state to match
	 * {@link SpotInstanceRequest}s against.
	 */
	@Test
	public void testAllInAnyOfStatesPredicateWithMultipleMatchingStates() {
		assertTrue(SpotRequestPredicates
				.allInAnyOfStates("cancelled", "failed").apply(spotRequests()));
		assertTrue(SpotRequestPredicates
				.allInAnyOfStates("cancelled", "failed").apply(
						spotRequests("cancelled", "failed")));
		assertTrue(SpotRequestPredicates
				.allInAnyOfStates("cancelled", "failed").apply(
						spotRequests("cancelled", "failed", "failed",
								"cancelled")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("cancelled",
				"failed").apply(
				spotRequests("cancelled", "active", "failed", "failed",
						"cancelled")));

		assertTrue(SpotRequestPredicates.allInAnyOfStates("open", "active")
				.apply(spotRequests("open", "open", "active", "open")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("open", "active")
				.apply(spotRequests("open", "open", "active", "cancelled")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("open", "active")
				.apply(spotRequests("open", "open", "active", "closed")));
		assertFalse(SpotRequestPredicates.allInAnyOfStates("open", "active")
				.apply(spotRequests("open", "open", "active", "failed")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAllInAnyOfStatesPredicateWithIllegalState() {
		SpotRequestPredicates.allInAnyOfStates("badstate");
	}

	private SpotInstanceRequest spotRequest(String id, String state) {
		return new SpotInstanceRequest().withSpotInstanceRequestId(id)
				.withState(state);
	}

	private List<SpotInstanceRequest> spotRequests(String... states) {
		List<SpotInstanceRequest> requests = new ArrayList<>();
		int index = 1;
		for (String state : states) {
			requests.add(new SpotInstanceRequest().withSpotInstanceRequestId(
					"sir-" + index++).withState(state));
		}
		return requests;
	}
}
