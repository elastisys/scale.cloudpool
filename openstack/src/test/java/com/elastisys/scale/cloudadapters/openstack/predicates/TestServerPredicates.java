package com.elastisys.scale.cloudadapters.openstack.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.junit.Test;

import com.elastisys.scale.cloudadapters.openstack.predicates.ServerPredicates;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises the {@link ServerPredicates} class.
 * 
 * 
 */
public class TestServerPredicates {

	@Test(expected = NullPointerException.class)
	public void testWithTagOnNullTag() {
		ServerPredicates.withTag(null, "mygroup");
	}

	@Test(expected = NullPointerException.class)
	public void testWithTagOnNullTagValue() {
		ServerPredicates.withTag("scaling-group", null);
	}

	@Test
	public void testWithTag() {
		Map<String, String> wrongTag = ImmutableMap
				.of("wrong-group", "mygroup");
		Map<String, String> wrongTagValue = ImmutableMap.of("scaling-group",
				"another-group");
		Map<String, String> matchingTag = ImmutableMap.of("scaling-group",
				"mygroup");

		assertFalse(ServerPredicates.withTag("scaling-group", "mygroup").apply(
				server(wrongTag)));
		assertFalse(ServerPredicates.withTag("scaling-group", "mygroup").apply(
				server(wrongTagValue)));
		assertTrue(ServerPredicates.withTag("scaling-group", "mygroup").apply(
				server(matchingTag)));
	}

	@Test(expected = NullPointerException.class)
	public void testWithStateInOnNullArgument() {
		ServerPredicates.withStateIn(null);
	}

	@Test
	public void testWithStateIn() {
		Server server = server(Status.STOPPED);

		// empty set
		assertFalse(ServerPredicates.withStateIn().apply(server));
		// non-empty set, missing a matching state
		assertFalse(ServerPredicates.withStateIn(Status.ACTIVE).apply(server));
		assertFalse(ServerPredicates.withStateIn(Status.ACTIVE, Status.BUILD)
				.apply(server));
		// set contains matching state only
		assertTrue(ServerPredicates.withStateIn(Status.STOPPED).apply(server));
		// set contains matching state and other states
		assertTrue(ServerPredicates.withStateIn(Status.ACTIVE, Status.STOPPED)
				.apply(server));

		// test with another server state
		server = server(Status.ACTIVE);
		assertFalse(ServerPredicates.withStateIn(Status.STOPPED, Status.BUILD)
				.apply(server));
		assertTrue(ServerPredicates.withStateIn(Status.ACTIVE, Status.STOPPED)
				.apply(server));
	}

	private Server server(Status status) {
		return Server.builder().tenantId("tenantId").id("serverId")
				.userId("userId").created(new Date())
				.image(Resource.builder().id("imageId").build())
				.flavor(Resource.builder().id("flavorId").build())
				.status(status).build();
	}

	private Server server(Map<String, String> metadataTags) {
		return Server.builder().tenantId("tenantId").id("serverId")
				.userId("userId").created(new Date())
				.image(Resource.builder().id("imageId").build())
				.flavor(Resource.builder().id("flavorId").build())
				.status(Status.ACTIVE).metadata(metadataTags).build();
	}

}
