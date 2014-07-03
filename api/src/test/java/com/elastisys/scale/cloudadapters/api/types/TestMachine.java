package com.elastisys.scale.cloudadapters.api.types;

import static com.elastisys.scale.cloudadapters.api.types.TestUtils.ips;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link Machine} class.
 * 
 * 
 * 
 */
public class TestMachine {

	/**
	 * Verify behavior of equals comparisons.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEquality() throws IOException {
		// with null launch time
		Machine noLaunchTime = new Machine("i-1", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine noLaunchTimeClone = new Machine("i-1", MachineState.REQUESTED,
				null, null, null, new JsonObject());

		// with liveness
		Machine withLiveness = new Machine("i-1", MachineState.REQUESTED,
				LivenessState.BOOTING, null, null, null, new JsonObject());
		Machine withLivenessClone = new Machine("i-1", MachineState.REQUESTED,
				LivenessState.BOOTING, null, null, null, new JsonObject());

		// with meta data
		JsonObject metadata = JsonUtils
				.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}");
		Machine withMetadata = new Machine("i-1", MachineState.RUNNING,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);
		Machine withMetadataClone = new Machine("i-1", MachineState.RUNNING,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);

		assertEquals(noLaunchTime, noLaunchTimeClone);
		assertEquals(withMetadata, withMetadataClone);
		assertEquals(withLiveness, withLivenessClone);

		assertFalse(noLaunchTime.equals(withMetadata));
		assertFalse(noLaunchTime.equals(withLiveness));

		assertFalse(withMetadata.equals(noLaunchTime));
		assertFalse(withMetadata.equals(withLiveness));

		assertFalse(withLiveness.equals(withMetadata));
		assertFalse(withLiveness.equals(noLaunchTime));
	}

	/**
	 * Verify behavior of hashCode method.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testHashCode() throws IOException {
		// with null launch time
		Machine noLaunchTime = new Machine("i-1", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine noLaunchTimeClone = new Machine("i-1", MachineState.REQUESTED,
				null, null, null, new JsonObject());
		assertEquals(noLaunchTime.hashCode(), noLaunchTimeClone.hashCode());

		// with liveness
		Machine withLiveness = new Machine("i-1", MachineState.REQUESTED,
				LivenessState.BOOTING, null, null, null, new JsonObject());
		Machine withLivenessClone = new Machine("i-1", MachineState.REQUESTED,
				LivenessState.BOOTING, null, null, null, new JsonObject());
		assertEquals(withLiveness.hashCode(), withLivenessClone.hashCode());

		// with meta data
		JsonObject metadata = JsonUtils
				.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}");
		Machine withMetadata = new Machine("i-1", MachineState.RUNNING,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);
		Machine withMetadataClone = new Machine("i-1", MachineState.RUNNING,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);
		assertEquals(withMetadata.hashCode(), withMetadataClone.hashCode());

		assertFalse(noLaunchTime.hashCode() == withMetadata.hashCode());
		assertFalse(noLaunchTime.hashCode() == withLiveness.hashCode());
		assertFalse(withMetadata.hashCode() == withLiveness.hashCode());
	}

	@Test
	public void createWithoutIpAddresses() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, null, null, new JsonObject());
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));

		noIpAddresses = new Machine("i-1", MachineState.REQUESTED, null, ips(),
				ips(), new JsonObject());
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));
	}

	@Test
	public void createWithOnlyPublicIpAddress() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, ips("1.2.3.4"), null, new JsonObject());
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips("1.2.3.4")));
	}

	@Test
	public void createWithOnlyPrivateIpAddress() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, null, ips("1.2.3.4"), new JsonObject());
		assertThat(noIpAddresses.getPublicIps(), is(ips()));
		assertThat(noIpAddresses.getPrivateIps(), is(ips("1.2.3.4")));
	}

	/**
	 * Make sure a default {@link LivenessState} is set when none is specified
	 * on construction.
	 */
	@Test
	public void createWithoutLiveness() {
		Machine defaultLiveness = new Machine("i-1", MachineState.REQUESTED,
				null, null, null, new JsonObject());
		assertThat(defaultLiveness.getLiveness(), is(LivenessState.UNKNOWN));
	}

	@Test
	public void createWithLiveness() {
		Machine withLiveness = new Machine("i-1", MachineState.REQUESTED,
				LivenessState.LIVE, null, null, ips("1.2.3.4"),
				new JsonObject());
		assertThat(withLiveness.getLiveness(), is(LivenessState.LIVE));
	}

}
