package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercise the {@link Machine.Builder} class.
 */
public class TestMachineBuilder {

	/**
	 * A {@link Machine} must at least have an id and a {@link MachineState}.
	 * All other attributes should be given default values.
	 */
	@Test
	public void createMinimal() {
		Machine m1 = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small").build();
		assertThat(m1.getId(), is("i-1"));
		assertThat(m1.getMachineState(), is(MachineState.RUNNING));
		assertThat(m1.getCloudProvider(), is("AWS-EC2"));
		assertThat(m1.getMachineSize(), is("m1.small"));

		// check defaults
		assertThat(m1.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(m1.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(m1.getRequestTime(), is(nullValue()));
		assertThat(m1.getLaunchTime(), is(nullValue()));
		List<String> emptyList = Collections.emptyList();
		assertThat(m1.getPublicIps(), is(emptyList));
		assertThat(m1.getPrivateIps(), is(emptyList));
		assertThat(m1.getMetadata(), is(nullValue()));
	}

	/**
	 * A {@link Machine} must at least have an id and a {@link MachineState}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void createWithoutId() {
		Machine.builder().machineState(MachineState.RUNNING).build();
	}

	/**
	 * A {@link Machine} must at least have an id and a {@link MachineState}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void createWithoutMachineState() {
		Machine.builder().id("i-1").build();
	}

	/**
	 * A {@link Machine} must have a cloud provider.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void createWithoutCloudProvider() {
		Machine.builder().id("i-1").machineState(MachineState.RUNNING)
				.machineSize("m1.small").build();
	}

	/**
	 * A {@link Machine} must have a size.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void createWithoutMachineSize() {
		Machine.builder().id("i-1").machineState(MachineState.RUNNING)
				.cloudProvider("AWS-EC2").build();
	}

	@Test
	public void createWithServiceState() {
		Machine withServiceState = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
				.machineSize("m1.small").serviceState(ServiceState.IN_SERVICE)
				.build();
		assertThat(withServiceState.getServiceState(),
				is(ServiceState.IN_SERVICE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullServiceState() {
		Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
				.serviceState(null).build();
	}

	@Test
	public void createWithMembershipStatus() {
		Machine machine = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small")
				.membershipStatus(MembershipStatus.blessed()).build();
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.blessed()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullMembershipStatus() {
		Machine.builder().id("i-1").machineState(MachineState.RUNNING)
				.membershipStatus(null).build();
	}

	@Test
	public void createWithRequestTime() {
		DateTime now = UtcTime.now();
		Machine machine = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small").requestTime(now).build();
		assertThat(machine.getRequestTime(), is(now));
		assertThat(machine.getLaunchTime(), is(nullValue()));
	}

	@Test
	public void createWithNullRequestTime() {
		Machine.builder().id("i-1").machineState(MachineState.RUNNING)
				.cloudProvider("AWS-EC2").machineSize("m1.small")
				.requestTime(null).build();
	}

	@Test
	public void createWithLaunchTime() {
		DateTime now = UtcTime.now();
		Machine machine = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small").launchTime(now).build();
		assertThat(machine.getLaunchTime(), is(now));
		assertThat(machine.getRequestTime(), is(nullValue()));
	}

	@Test
	public void createWithNullLaunchTime() {
		Machine.builder().id("i-1").machineState(MachineState.RUNNING)
				.cloudProvider("AWS-EC2").machineSize("m1.small")
				.launchTime(null).build();
	}

	@Test
	public void createWithPrivateIpAddress() {
		Machine m = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
				.machineSize("m1.small").privateIp("1.2.3.4")
				.privateIp("1.2.3.5").build();
		assertThat(m.getPublicIps(), is(ips()));
		assertThat(m.getPrivateIps(), is(ips("1.2.3.4", "1.2.3.5")));

		m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
				.cloudProvider("AWS-EC2").machineSize("m1.small")
				.privateIps(ips("1.2.3.4", "1.2.3.5")).build();
		assertThat(m.getPublicIps(), is(ips()));
		assertThat(m.getPrivateIps(), is(ips("1.2.3.4", "1.2.3.5")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullPrivateIpAddress() {
		Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
				.privateIp(null).build();
	}

	@Test
	public void createWithPublicIpAddress() {
		Machine m = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
				.machineSize("m1.small").publicIp("1.2.3.4").publicIp("1.2.3.5")
				.build();
		assertThat(m.getPrivateIps(), is(ips()));
		assertThat(m.getPublicIps(), is(ips("1.2.3.4", "1.2.3.5")));

		m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
				.cloudProvider("AWS-EC2").machineSize("m1.small")
				.publicIps(ips("1.2.3.4", "1.2.3.5")).build();
		assertThat(m.getPrivateIps(), is(ips()));
		assertThat(m.getPublicIps(), is(ips("1.2.3.4", "1.2.3.5")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullPublicIpAddress() {
		Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
				.publicIp(null).build();
	}

	@Test
	public void createWithMetadata() {
		JsonObject metadata = JsonUtils
				.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}")
				.getAsJsonObject();
		Machine machine = Machine.builder().id("i-1")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small").metadata(metadata).build();
		assertThat(machine.getMetadata(), is(metadata));
	}

	@Test
	public void createWithOnlyPublicIpAddress() {
		Machine onlyPublicIps = Machine.builder().id("i-1")
				.machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
				.machineSize("m1.small").publicIps(ips("1.2.3.4")).build();
		assertThat(onlyPublicIps.getPrivateIps(), is(ips()));
		assertThat(onlyPublicIps.getPublicIps(), is(ips("1.2.3.4")));
	}

}
