package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
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
		// with null launch time
		Machine noLaunchTime = new Machine("i-1", MachineState.REQUESTED, null,
				null, null);
		Machine noLaunchTimeClone = new Machine("i-1", MachineState.REQUESTED,
				null, null, null);

		// with service state
		Machine withServiceState = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, null, null, null);
		Machine withServiceStateClone = new Machine("i-1",
				MachineState.REQUESTED, ServiceState.BOOTING, null, null, null);

		// with service state
		DateTime now = UtcTime.now();
		Machine withIps = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, now, Arrays.asList("1.2.3.4"),
				Arrays.asList("1.2.3.5"));
		Machine withIpsClone = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, now, Arrays.asList("1.2.3.4"),
				Arrays.asList("1.2.3.5"));

		// with meta data and membership status
		JsonObject metadata = JsonUtils
				.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}");
		Machine withMetadata = new Machine("i-1", MachineState.RUNNING,
				new MembershipStatus(true, false), ServiceState.UNKNOWN,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);
		Machine withMetadataClone = new Machine("i-1", MachineState.RUNNING,
				new MembershipStatus(true, false), ServiceState.UNKNOWN,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);

		assertEquals(noLaunchTime, noLaunchTimeClone);
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
		// with null launch time
		Machine noLaunchTime = new Machine("i-1", MachineState.REQUESTED, null,
				null, null);
		Machine noLaunchTimeClone = new Machine("i-1", MachineState.REQUESTED,
				null, null, null);
		assertEquals(noLaunchTime.hashCode(), noLaunchTimeClone.hashCode());

		// with service state
		Machine withServiceState = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, null, null, null);
		Machine withServiceStateClone = new Machine("i-1",
				MachineState.REQUESTED, ServiceState.BOOTING, null, null, null);
		assertEquals(withServiceState.hashCode(),
				withServiceStateClone.hashCode());

		// with IP addresses
		DateTime now = UtcTime.now();
		Machine withIps = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, now, Arrays.asList("1.2.3.4"),
				Arrays.asList("1.2.3.5"));
		Machine withIpsClone = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.BOOTING, now, Arrays.asList("1.2.3.4"),
				Arrays.asList("1.2.3.5"));

		// with meta data and membership status
		JsonObject metadata = JsonUtils
				.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}");
		Machine withMetadata = new Machine("i-1", MachineState.RUNNING,
				new MembershipStatus(true, false), ServiceState.UNKNOWN,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);
		Machine withMetadataClone = new Machine("i-1", MachineState.RUNNING,
				new MembershipStatus(true, false), ServiceState.UNKNOWN,
				UtcTime.parse("2014-01-10T08:00:00Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), metadata);

		assertEquals(withMetadata.hashCode(), withMetadataClone.hashCode());
		assertEquals(withIps.hashCode(), withIpsClone.hashCode());
		assertEquals(withMetadata.hashCode(), withMetadataClone.hashCode());

		assertFalse(noLaunchTime.hashCode() == withIps.hashCode());
		assertFalse(noLaunchTime.hashCode() == withServiceState.hashCode());
		assertFalse(withIps.hashCode() == withServiceState.hashCode());
		assertFalse(withMetadata.hashCode() == withIps.hashCode());
	}

	@Test
	public void createWithoutIpAddresses() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, null, null);
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));

		noIpAddresses = new Machine("i-1", MachineState.REQUESTED, null, ips(),
				ips());
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));
	}

	@Test
	public void createWithOnlyPublicIpAddress() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, ips("1.2.3.4"), null);
		assertThat(noIpAddresses.getPrivateIps(), is(ips()));
		assertThat(noIpAddresses.getPublicIps(), is(ips("1.2.3.4")));
	}

	@Test
	public void createWithOnlyPrivateIpAddress() {
		Machine noIpAddresses = new Machine("i-1", MachineState.REQUESTED,
				null, null, ips("1.2.3.4"));
		assertThat(noIpAddresses.getPublicIps(), is(ips()));
		assertThat(noIpAddresses.getPrivateIps(), is(ips("1.2.3.4")));
	}

	/**
	 * Make sure the default {@link MembershipStatus} is set when none is
	 * specified on construction.
	 */
	@Test
	public void createWithoutMembershipStatus() {
		Machine defaultMembershipStatus = new Machine("i-1",
				MachineState.REQUESTED, null, null, null);
		assertThat(defaultMembershipStatus.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
	}

	/**
	 * Make sure a default {@link ServiceState} is set when none is specified on
	 * construction.
	 */
	@Test
	public void createWithoutServiceState() {
		Machine defaultServiceState = new Machine("i-1",
				MachineState.REQUESTED, null, null, null);
		assertThat(defaultServiceState.getServiceState(),
				is(ServiceState.UNKNOWN));
	}

	@Test
	public void createWithServiceState() {
		Machine withServiceState = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.IN_SERVICE, null, null, ips("1.2.3.4"));
		assertThat(withServiceState.getServiceState(),
				is(ServiceState.IN_SERVICE));
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

		Machine first = new Machine("i-1", MachineState.RUNNING,
				ServiceState.IN_SERVICE, am, null, null);
		Machine second = new Machine("i-2", MachineState.PENDING,
				ServiceState.BOOTING, noon, null, null);
		Machine third = new Machine("i-3", MachineState.REQUESTED,
				ServiceState.UNKNOWN, pm, null, null);

		Comparator<Machine> earliestFirst = new Comparator<Machine>() {
			@Override
			public int compare(Machine m1, Machine m2) {
				return m1.getLaunchtime().compareTo(m2.getLaunchtime());
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
}
