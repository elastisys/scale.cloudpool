package com.elastisys.scale.cloudpool.openstack.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.openstack.compute.domain.NovaServer;

import com.elastisys.scale.cloudpool.openstack.predicates.ServerPredicates;
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
		ServerPredicates.withTag("elastisys:cloudPool", null);
	}

	@Test
	public void testWithTag() {
		Map<String, String> wrongTag = ImmutableMap
				.of("wrong-group", "mygroup");
		Map<String, String> wrongTagValue = ImmutableMap.of(
				"elastisys:cloudPool", "another-group");
		Map<String, String> matchingTag = ImmutableMap.of(
				"elastisys:cloudPool", "mygroup");

		assertFalse(ServerPredicates.withTag("elastisys:cloudPool", "mygroup")
				.apply(server(wrongTag)));
		assertFalse(ServerPredicates.withTag("elastisys:cloudPool", "mygroup")
				.apply(server(wrongTagValue)));
		assertTrue(ServerPredicates.withTag("elastisys:cloudPool", "mygroup")
				.apply(server(matchingTag)));
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
		NovaServer server = new NovaServer();
		server.id = "serverId";
		server.status = status;
		return server;
	}

	private Server server(Map<String, String> metadataTags) {
		NovaServer server = new NovaServer();
		server.id = "serverId";
		server.metadata = metadataTags;
		return server;
	}

}
