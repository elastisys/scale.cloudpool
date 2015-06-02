package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link Machine} class.
 */
public class TestMachine {

	/**
	 * Verify behavior of equals comparisons.
	 *
	 * @throws IOException
	 */
	@Test
	public void testEquality() throws IOException {
		final DateTime now = UtcTime.now();
		// with null launch time
		Machine noLaunchTime = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).build();
		Machine noLaunchTimeClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).build();

		// with set requestTime, null launch time
		Machine unlaunchedRequested = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).requestTime(now).build();
		Machine unlaunchedRequestedClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).requestTime(now).build();

		// with service state
		Machine withServiceState = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now).build();
		Machine withServiceStateClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now).build();

		// with service state
		Machine withIps = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now)
				.launchTime(now).publicIps(ips("1.2.3.4"))
				.privateIps(ips("1.2.3.5")).build();
		Machine withIpsClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now)
				.launchTime(now).publicIps(ips("1.2.3.4"))
				.privateIps(ips("1.2.3.5")).build();

		// with meta data and membership status
		JsonObject metadata = JsonUtils.parseJsonString(
				"{'a': 1, 'b': 2, c: {'d': 4}}").getAsJsonObject();
		Machine withMetadata = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING)
				.membershipStatus(new MembershipStatus(true, false))
				.requestTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.launchTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5"))
				.metadata(metadata).build();
		Machine withMetadataClone = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING)
				.membershipStatus(new MembershipStatus(true, false))
				.requestTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.launchTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5"))
				.metadata(metadata).build();

		assertEquals(noLaunchTime, noLaunchTimeClone);
		assertEquals(unlaunchedRequested, unlaunchedRequestedClone);
		assertEquals(withIps, withIpsClone);
		assertEquals(withServiceState, withServiceStateClone);
		assertEquals(withMetadata, withMetadataClone);

		assertFalse(noLaunchTime.equals(withIps));
		assertFalse(noLaunchTime.equals(withServiceState));
		assertFalse(noLaunchTime.equals(withMetadata));

		assertFalse(withIps.equals(noLaunchTime));
		assertFalse(withIps.equals(withServiceState));
		assertFalse(withIps.equals(withMetadata));

		assertFalse(withServiceState.equals(withIps));
		assertFalse(withServiceState.equals(noLaunchTime));
		assertFalse(withServiceState.equals(withMetadata));

		assertFalse(withMetadata.equals(withIps));
		assertFalse(withMetadata.equals(noLaunchTime));
		assertFalse(withMetadata.equals(withServiceState));
	}

	/**
	 * Verify behavior of hashCode method.
	 *
	 * @throws IOException
	 */
	@Test
	public void testHashCode() throws IOException {
		final DateTime now = UtcTime.now();
		// with null launch time
		Machine noLaunchTime = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).build();
		Machine noLaunchTimeClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).build();

		// with set requestTime, null launch time
		Machine unlaunchedRequested = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).requestTime(now).build();
		Machine unlaunchedRequestedClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).requestTime(now).build();

		// with service state
		Machine withServiceState = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now).build();
		Machine withServiceStateClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now).build();

		// with service state
		Machine withIps = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now)
				.launchTime(now).publicIps(ips("1.2.3.4"))
				.privateIps(ips("1.2.3.5")).build();
		Machine withIpsClone = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED)
				.serviceState(ServiceState.BOOTING).requestTime(now)
				.launchTime(now).publicIps(ips("1.2.3.4"))
				.privateIps(ips("1.2.3.5")).build();

		// with meta data and membership status
		JsonObject metadata = JsonUtils.parseJsonString(
				"{'a': 1, 'b': 2, c: {'d': 4}}").getAsJsonObject();
		Machine withMetadata = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING)
				.membershipStatus(new MembershipStatus(true, false))
				.requestTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.launchTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5"))
				.metadata(metadata).build();
		Machine withMetadataClone = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING)
				.membershipStatus(new MembershipStatus(true, false))
				.requestTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.launchTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5"))
				.metadata(metadata).build();

		assertEquals(noLaunchTime.hashCode(), noLaunchTimeClone.hashCode());
		assertEquals(unlaunchedRequested.hashCode(),
				unlaunchedRequestedClone.hashCode());
		assertEquals(withServiceState.hashCode(),
				withServiceStateClone.hashCode());
		assertEquals(withMetadata.hashCode(), withMetadataClone.hashCode());
		assertEquals(withIps.hashCode(), withIpsClone.hashCode());
		assertEquals(withMetadata.hashCode(), withMetadataClone.hashCode());

		assertFalse(noLaunchTime.hashCode() == withIps.hashCode());
		assertFalse(noLaunchTime.hashCode() == withServiceState.hashCode());
		assertFalse(noLaunchTime.hashCode() == unlaunchedRequested.hashCode());

		assertFalse(withIps.hashCode() == withServiceState.hashCode());
		assertFalse(withIps.hashCode() == unlaunchedRequested.hashCode());
		assertFalse(withIps.hashCode() == withMetadata.hashCode());

		assertFalse(withMetadata.hashCode() == withServiceState.hashCode());
		assertFalse(withMetadata.hashCode() == unlaunchedRequested.hashCode());

		assertFalse(withServiceState.hashCode() == unlaunchedRequested
				.hashCode());
	}

	@Test
	public void createWithoutIpAddresses() {
		Machine noIpAddresses = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).build();
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));

		noIpAddresses = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).publicIps(ips())
				.privateIps(ips()).build();
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));
	}

	/**
	 * Exercise the
	 * {@link Machine#sort(java.util.Collection, java.util.Comparator)} method.
	 */
	@Test
	public void testSort() {
		// with service state

		DateTime am = UtcTime.parse("2015-02-13T08:00:00.000Z");
		DateTime noon = UtcTime.parse("2015-02-13T12:00:00.000Z");
		DateTime pm = UtcTime.parse("2015-02-13T15:00:00.000Z");

		Machine first = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).launchTime(am).build();
		Machine second = Machine.builder().id("i-2")
				.machineState(MachineState.PENDING).launchTime(noon).build();
		Machine third = Machine.builder().id("i-3")
				.machineState(MachineState.REQUESTED).launchTime(pm).build();

		Comparator<Machine> earliestFirst = new Comparator<Machine>() {
			@Override
			public int compare(Machine m1, Machine m2) {
				return m1.getLaunchTime().compareTo(m2.getLaunchTime());
			}
		};

		// sort with empty list
		List<Machine> emptyList = Collections.emptyList();
		assertThat(Machine.sort(emptyList, earliestFirst), is(emptyList));

		// single machine
		assertThat(Machine.sort(asList(first), earliestFirst),
				is(asList(first)));

		// multiple machines
		assertThat(Machine.sort(asList(third, first, second), earliestFirst),
				is(asList(first, second, third)));

	}

	/**
	 * Tests {@link Machine} copying through the
	 * {@link Machine#withMetadata(JsonObject)} method.
	 */
	@Test
	public void testWithMetadata() {
		JsonObject metadata = JsonUtils.parseJsonString(
				"{'a': 1, 'b': 2, c: {'d': 4}}").getAsJsonObject();
		Machine original = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING)
				.membershipStatus(new MembershipStatus(true, false))
				.requestTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.launchTime(UtcTime.parse("2014-01-10T08:00:00Z"))
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5"))
				.metadata(metadata).build();

		// other metadata
		JsonObject otherMetadata = JsonUtils
				.parseJsonString("{'d': 1, 'e': 2}").getAsJsonObject();
		Machine copy = original.withMetadata(otherMetadata);
		// all fields should be equal except metadata
		assertThat(copy.getId(), is(original.getId()));
		assertThat(copy.getMachineState(), is(original.getMachineState()));
		assertThat(copy.getMembershipStatus(),
				is(original.getMembershipStatus()));
		assertThat(copy.getServiceState(), is(original.getServiceState()));
		assertThat(copy.getLaunchTime(), is(original.getLaunchTime()));
		assertThat(copy.getPublicIps(), is(original.getPublicIps()));
		assertThat(copy.getPrivateIps(), is(original.getPrivateIps()));
		assertFalse(copy.getMetadata().equals(original.getMetadata()));
		assertThat(copy.getMetadata(), is(otherMetadata));

		// should be possible to set null metadata
		copy = original.withMetadata(null);
		assertThat(copy.getMetadata(), is(nullValue()));
	}
}
