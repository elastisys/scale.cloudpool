package com.elastisys.scale.cloudpool.aws.commons.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.HypervisorType;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Monitoring;
import com.amazonaws.services.ec2.model.MonitoringState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifiers;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link InstanceToMachine} class.
 */
public class TestInstanceToMachine {
	private final static Logger LOG = LoggerFactory
			.getLogger(TestInstanceToMachine.class);

	@Before
	public void onSetup() {
		FrozenTime.setFixed(UtcTime.parse("2014-04-01T12:00:00Z"));
	}

	@Test
	public void convertCompleteInstance() {
		DateTime launchTime = UtcTime.now();
		Instance instance = new Instance().withInstanceId("i-1")
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withPublicIpAddress("1.2.3.4").withPrivateIpAddress("1.2.3.5")
				.withInstanceType(InstanceType.M1Small)
				.withLaunchTime(launchTime.toDate())
				.withMonitoring(
						new Monitoring().withState(MonitoringState.Disabled))
				.withHypervisor(HypervisorType.Xen);

		Machine machine = convert(instance);
		assertThat(machine.getId(), is(instance.getInstanceId()));
		assertThat(machine.getLaunchTime(), is(launchTime));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
		assertThat(machine.getMachineSize(), is("m1.small"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps().size(), is(1));
		assertThat(machine.getPublicIps().get(0),
				is(instance.getPublicIpAddress()));
		assertThat(machine.getPrivateIps().size(), is(1));
		assertThat(machine.getPrivateIps().get(0),
				is(instance.getPrivateIpAddress()));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(instance)));
	}

	@Test
	public void convertInstanceMissingPublicIp() {
		DateTime launchTime = UtcTime.now();

		Instance instance = new Instance().withInstanceId("i-1")
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
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
		assertThat(machine.getMachineSize(), is("m1.small"));
		assertThat(machine.getLaunchTime(), is(launchTime));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps().size(), is(0));
		assertThat(machine.getPrivateIps().size(), is(1));
		assertThat(machine.getPrivateIps().get(0),
				is(instance.getPrivateIpAddress()));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(instance)));
	}

	@Test
	public void convertInstanceMissingPrivateIp() {
		DateTime launchTime = UtcTime.now();
		Instance instance = new Instance().withInstanceId("i-1")
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withPublicIpAddress("1.2.3.4")
				.withInstanceType(InstanceType.M1Medium)
				.withLaunchTime(launchTime.toDate())
				.withMonitoring(
						new Monitoring().withState(MonitoringState.Disabled))
				.withHypervisor(HypervisorType.Xen);

		Machine machine = convert(instance);
		assertThat(machine.getId(), is(instance.getInstanceId()));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getLaunchTime(), is(launchTime));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps().size(), is(1));
		assertThat(machine.getPublicIps().get(0),
				is(instance.getPublicIpAddress()));
		assertThat(machine.getPrivateIps().size(), is(0));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(instance)));
	}

	@Test
	public void convertWithServiceStateTag() {
		Tag serviceStateTag = new Tag().withKey(ScalingTags.SERVICE_STATE_TAG)
				.withValue(ServiceState.OUT_OF_SERVICE.name());
		Instance instance = new Instance().withInstanceId("i-1")
				.withInstanceType(InstanceType.M1Medium)
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withTags(serviceStateTag);

		Machine machine = convert(instance);
		assertThat(machine.getServiceState(), is(ServiceState.OUT_OF_SERVICE));
	}

	@Test
	public void convertWithMembershipStatusTag() {
		MembershipStatus status = MembershipStatus.awaitingService();
		String statusJsonString = JsonUtils.toString(JsonUtils.toJson(status));
		Tag membershipStatusTag = new Tag()
				.withKey(ScalingTags.MEMBERSHIP_STATUS_TAG)
				.withValue(statusJsonString);
		Instance instance = new Instance().withInstanceId("i-1")
				.withInstanceType(InstanceType.M1Medium)
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withTags(membershipStatusTag);

		Machine machine = convert(instance);
		assertThat(machine.getMembershipStatus(), is(status));
	}

	/**
	 * A converted spot instance {@link Machine} should have a cloud provider
	 * value of {@link PoolIdentifiers#AWS_SPOT} to distinguish it from a
	 * regular EC2 on-demand instance.
	 */
	@Test
	public void convertSpotInstance() {
		// convert on-demand instance: cloud provider should be AWS_EC2
		Instance onDemandInstance = new Instance().withInstanceId("i-1")
				.withInstanceType(InstanceType.M1Medium)
				.withState(new InstanceState()
						.withName(InstanceStateName.Running));
		Machine onDemandMachine = convert(onDemandInstance);
		assertThat(onDemandMachine.getCloudProvider(),
				is(PoolIdentifiers.AWS_EC2));

		// convert spot instance: cloud provider should be AWS_EC2
		Instance spotInstance = new Instance().withInstanceId("i-1")
				.withInstanceType(InstanceType.M1Medium)
				.withState(
						new InstanceState().withName(InstanceStateName.Running))
				.withSpotInstanceRequestId("sir-123");
		Machine spotMachine = convert(spotInstance);
		assertThat(spotMachine.getCloudProvider(),
				is(PoolIdentifiers.AWS_SPOT));
	}

	public Machine convert(Instance instance) {
		return InstanceToMachine.convert(instance);
	}
}
