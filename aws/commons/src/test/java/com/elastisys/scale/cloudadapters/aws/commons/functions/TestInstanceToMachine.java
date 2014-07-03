package com.elastisys.scale.cloudadapters.aws.commons.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.ec2.model.HypervisorType;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Monitoring;
import com.amazonaws.services.ec2.model.MonitoringState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;

/**
 * Exercises the {@link InstanceToMachine} class.
 * 
 * 
 * 
 */
public class TestInstanceToMachine {

	@Before
	public void onSetup() {
		FrozenTime.setFixed(UtcTime.parse("2014-04-01T12:00:00Z"));
	}

	@Test
	public void convertCompleteInstance() {
		DateTime launchTime = UtcTime.now();
		Instance instance = new Instance()
				.withInstanceId("i-1")
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withPublicIpAddress("1.2.3.4")
				.withPrivateIpAddress("1.2.3.5")
				.withInstanceType(InstanceType.M1Small)
				.withLaunchTime(launchTime.toDate())
				.withMonitoring(
						new Monitoring().withState(MonitoringState.Disabled))
				.withHypervisor(HypervisorType.Xen);

		Machine machine = convert(instance);
		assertThat(machine.getId(), is(instance.getInstanceId()));
		assertThat(machine.getLaunchtime(), is(launchTime));
		assertThat(machine.getState(), is(MachineState.RUNNING));
		assertThat(machine.getPublicIps().size(), is(1));
		assertThat(machine.getPublicIps().get(0),
				is(instance.getPublicIpAddress()));
		assertThat(machine.getPrivateIps().size(), is(1));
		assertThat(machine.getPrivateIps().get(0),
				is(instance.getPrivateIpAddress()));
		assertThat(machine.getMetadata(), is(JsonElement.class));
	}

	@Test
	public void convertInstanceMissingPublicIp() {
		DateTime launchTime = UtcTime.now();

		Instance instance = new Instance()
				.withInstanceId("i-1")
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withPrivateIpAddress("1.2.3.5")
				.withInstanceType(InstanceType.M1Small)
				.withLaunchTime(launchTime.toDate())
				.withMonitoring(
						new Monitoring().withState(MonitoringState.Disabled))
				.withHypervisor(HypervisorType.Xen);

		Machine machine = convert(instance);
		assertThat(machine.getId(), is(instance.getInstanceId()));
		assertThat(machine.getLaunchtime(), is(launchTime));
		assertThat(machine.getState(), is(MachineState.RUNNING));
		assertThat(machine.getPublicIps().size(), is(0));
		assertThat(machine.getPrivateIps().size(), is(1));
		assertThat(machine.getPrivateIps().get(0),
				is(instance.getPrivateIpAddress()));
		assertThat(machine.getMetadata(), is(JsonElement.class));
	}

	@Test
	public void convertInstanceMissingPrivateIp() {
		DateTime launchTime = UtcTime.now();
		Instance instance = new Instance()
				.withInstanceId("i-1")
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withPublicIpAddress("1.2.3.4")
				.withInstanceType(InstanceType.M1Small)
				.withLaunchTime(launchTime.toDate())
				.withMonitoring(
						new Monitoring().withState(MonitoringState.Disabled))
				.withHypervisor(HypervisorType.Xen);

		Machine machine = convert(instance);
		assertThat(machine.getId(), is(instance.getInstanceId()));
		assertThat(machine.getLaunchtime(), is(launchTime));
		assertThat(machine.getState(), is(MachineState.RUNNING));
		assertThat(machine.getPublicIps().size(), is(1));
		assertThat(machine.getPublicIps().get(0),
				is(instance.getPublicIpAddress()));
		assertThat(machine.getPrivateIps().size(), is(0));
		assertThat(machine.getMetadata(), is(JsonElement.class));
	}

	public Machine convert(Instance instance) {
		return new InstanceToMachine().apply(instance);
	}
}
