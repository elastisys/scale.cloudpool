package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.amazonaws.services.ec2.model.SpotInstanceRequest;

public class TestSpotRequestPredicates {

	/**
	 * Test the {@link SpotRequestPredicates#stateIn(String...)} predicate when
	 * there is only a single state to match {@link SpotInstanceRequest}s
	 * against.
	 */
	@Test
	public void testInPredicateWithSingleMatchingState() {
		// make sure all valid states are recognized
		assertTrue(SpotRequestPredicates.stateIn("active").apply(
				spotRequest("sir-1", "active")));
		assertTrue(SpotRequestPredicates.stateIn("closed").apply(
				spotRequest("sir-1", "closed")));
		assertTrue(SpotRequestPredicates.stateIn("cancelled").apply(
				spotRequest("sir-1", "cancelled")));
		assertTrue(SpotRequestPredicates.stateIn("failed").apply(
				spotRequest("sir-1", "failed")));
		assertTrue(SpotRequestPredicates.stateIn("open").apply(
				spotRequest("sir-1", "open")));
	}

	/**
	 * Test the {@link SpotRequestPredicates#stateIn(String...)} predicate when
	 * there are more than one state to match {@link SpotInstanceRequest}s
	 * against.
	 */
	@Test
	public void testInPredicateWithMultipleMatchingStates() {

		assertTrue(SpotRequestPredicates.stateIn("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "closed")));
		assertTrue(SpotRequestPredicates.stateIn("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "cancelled")));
		assertTrue(SpotRequestPredicates.stateIn("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "failed")));
		assertFalse(SpotRequestPredicates.stateIn("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "active")));
		assertFalse(SpotRequestPredicates.stateIn("closed", "cancelled",
				"failed").apply(spotRequest("sir-1", "open")));

		assertTrue(SpotRequestPredicates.stateIn("active", "open").apply(
				spotRequest("sir-1", "active")));
		assertTrue(SpotRequestPredicates.stateIn("active", "open").apply(
				spotRequest("sir-1", "open")));
		assertFalse(SpotRequestPredicates.stateIn("active", "open").apply(
				spotRequest("sir-1", "closed")));
		assertFalse(SpotRequestPredicates.stateIn("active", "open").apply(
				spotRequest("sir-1", "cancelled")));
		assertFalse(SpotRequestPredicates.stateIn("active", "open").apply(
				spotRequest("sir-1", "failed")));
	}

	/**
	 * Should not be possible to create a
	 * {@link SpotRequestPredicates#stateIn(String...)} with an illegal
	 * {@link SpotInstanceRequest} state.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testStateInPredicateWithIllegalState() {
		SpotRequestPredicates.stateIn("badstate");
	}

	private SpotInstanceRequest spotRequest(String id, String state) {
		return new SpotInstanceRequest().withSpotInstanceRequestId(id)
				.withState(state);
	}
}
