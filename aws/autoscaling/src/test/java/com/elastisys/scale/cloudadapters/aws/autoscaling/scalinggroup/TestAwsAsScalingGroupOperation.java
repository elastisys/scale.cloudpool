package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup;

import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.MachinesMatcher.machines;
import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.TestUtils.config;
import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.TestUtils.ec2Instance;
import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.TestUtils.ec2Instances;
import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.TestUtils.group;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AutoScalingClient;
import com.elastisys.scale.cloudadapters.aws.commons.functions.AwsAutoScalingFunctions;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.google.common.collect.Lists;

/**
 * Verifies the operational behavior of the {@link AwsAsScalingGroup}.
 *
 *
 *
 */
public class TestAwsAsScalingGroupOperation {
	static Logger LOG = LoggerFactory
			.getLogger(TestAwsAsScalingGroupOperation.class);

	private static final String GROUP_NAME = "MyScalingGroup";

	/** Object under test. */
	private AwsAsScalingGroup scalingGroup;

	private AutoScalingClient mockAwsClient = mock(AutoScalingClient.class);

	@Before
	public void onSetup() throws ScalingGroupException {
		this.scalingGroup = new AwsAsScalingGroup(this.mockAwsClient);
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));
	}

	@Test
	public void listMachines() throws ScalingGroupException {
		// empty Auto Scaling Group
		setUpMockedAutoScalingGroup(GROUP_NAME, 0, ec2Instances());
		assertThat(this.scalingGroup.listMachines(), is(machines()));
		verify(this.mockAwsClient).getAutoScalingGroup(GROUP_NAME);
		verify(this.mockAwsClient).getAutoScalingGroupMembers(GROUP_NAME);

		// non-empty group
		setUpMockedAutoScalingGroup(GROUP_NAME, 0,
				ec2Instances(ec2Instance("i-1", "running")));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"),
				ec2Instance("i-2", "pending"), ec2Instance("i-3", "terminated"));
		setUpMockedAutoScalingGroup(GROUP_NAME, 0, members);
		assertThat(this.scalingGroup.listMachines(),
				is(machines("i-1", "i-2", "i-3")));
	}

	/**
	 * If the actual size of the Auto Scaling Group is less than desired
	 * capacity (for example, when there is a shortage in supply of instances)
	 * the {@link AwsAsScalingGroup} should report the missing instances as
	 * being pseudo instances in state {@link MachineState#REQUESTED}, to not
	 * fool the {@link BaseCloudAdapter} from believing that the
	 * {@link ScalingGroup} is too small and order new machines to be started
	 * (and increase the desired capacity even more).
	 */
	@Test
	public void listMachinesOnGroupThatHasNotReachedDesiredCapacity()
			throws ScalingGroupException {
		// two missing instances: desired capacity: 3, actual capacity: 1
		int desiredCapacity = 3;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		List<Machine> groupMembers = this.scalingGroup.listMachines();
		assertThat(groupMembers,
				is(machines("i-1", "i-requested1", "i-requested2")));
		assertThat(groupMembers.get(0).getState(), is(MachineState.RUNNING));
		assertThat(groupMembers.get(1).getState(), is(MachineState.REQUESTED));
		assertThat(groupMembers.get(2).getState(), is(MachineState.REQUESTED));
	}

	/**
	 * A {@link ScalingGroupException} should be thrown if listing Auto Scaling
	 * Group members fails.
	 */
	@Test(expected = ScalingGroupException.class)
	public void listMachinesOnError() throws ScalingGroupException {
		// set up Amazon API call to fail
		when(this.mockAwsClient.getAutoScalingGroup(GROUP_NAME)).thenThrow(
				new AmazonServiceException("API unreachable"));
		this.scalingGroup.listMachines();
	}

	@Test
	public void startMachines() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// scale up from 0 -> 1
		int desiredCapacity = 0;
		this.scalingGroup = new AwsAsScalingGroup(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances()));
		this.scalingGroup.configure(config);
		List<Machine> startedMachines = this.scalingGroup.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-1")));

		// scale up from 1 -> 2
		desiredCapacity = 1;
		this.scalingGroup = new AwsAsScalingGroup(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(ec2Instance("i-1",
						"running"))));
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));

		// scale up from 2 -> 4
		desiredCapacity = 2;
		this.scalingGroup = new AwsAsScalingGroup(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(
						ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-3", "i-4")));
	}

	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		// set up mock to throw an error whenever setDesiredSize is called
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockAwsClient).setDesiredSize(GROUP_NAME, 2);

		ScaleUpConfig scaleUpConfig = config(GROUP_NAME).getScaleUpConfig();
		// should raise an exception
		try {
			this.scalingGroup.startMachines(1, scaleUpConfig);
			fail("startMachines expected to fail");
		} catch (StartMachinesException e) {
			assertThat(e.getRequestedMachines(), is(1));
			assertThat(e.getStartedMachines().size(), is(0));
		}
	}

	@Test
	public void terminate() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);

		int desiredCapacity = 2;
		this.scalingGroup = new AwsAsScalingGroup(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(
						ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		this.scalingGroup.terminateMachine("i-1");
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));

		this.scalingGroup.terminateMachine("i-2");
		assertThat(this.scalingGroup.listMachines(), is(machines()));
	}

	@Test
	public void removeFakeInstances() {
		class UnsatisfyingAutoScalingClient extends FakeAutoScalingClient {
			public UnsatisfyingAutoScalingClient(String autoScalingGroupName,
					int desiredCapacity, List<Instance> instances) {
				super(autoScalingGroupName, desiredCapacity, instances);
			}

			@Override
			public void setDesiredSize(String autoScalingGroupName,
					int desiredSize) {
				if (desiredSize > this.instances.size()) {
					this.desiredCapacity = desiredSize;
					LOG.debug(
							"not scaling up, desired size now {}, actual machine count {}",
							desiredSize, this.instances.size());
				} else {
					LOG.debug(
							"asked to scale down, requested size {}, actual desired size {}, actual machine count {}",
							desiredSize, this.desiredCapacity,
							this.instances.size());
					if (desiredSize < this.instances.size()) {
						super.setDesiredSize(autoScalingGroupName, desiredSize);
					} else {
						this.desiredCapacity = desiredSize;
					}
				}
			}
		}

		int desiredCapacity = 2;
		UnsatisfyingAutoScalingClient unsatisfyingClient = new UnsatisfyingAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(
						ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending")));

		BaseCloudAdapterConfig config = config(GROUP_NAME);

		this.scalingGroup = new AwsAsScalingGroup(unsatisfyingClient);
		this.scalingGroup.configure(config);

		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		assertThat(unsatisfyingClient.getAutoScalingGroup(GROUP_NAME)
				.getDesiredCapacity(), is(2));

		this.scalingGroup.startMachines(1, scaleUpConfig);
		final String fakeMachineId = AwsAsScalingGroup.REQUESTED_ID_PREFIX
				+ "1";
		// we should now have one machine that is faked by the group
		assertThat(this.scalingGroup.listMachines(),
				is(machines("i-1", "i-2", fakeMachineId)));

		assertThat(unsatisfyingClient.getAutoScalingGroup(GROUP_NAME)
				.getDesiredCapacity(), is(3));

		this.scalingGroup.terminateMachine(fakeMachineId);

		assertThat(unsatisfyingClient.getAutoScalingGroup(GROUP_NAME)
				.getDesiredCapacity(), is(2));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1", "i-2")));

		this.scalingGroup.terminateMachine("i-1");
		assertThat(unsatisfyingClient.getAutoScalingGroup(GROUP_NAME)
				.getDesiredCapacity(), is(1));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));
	}

	@Test(expected = ScalingGroupException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateInstance is called
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockAwsClient).terminateInstance(GROUP_NAME, "i-1");

		this.scalingGroup.terminateMachine("i-1");
	}

	private void setUpMockedAutoScalingGroup(String autoScalingGroupName,
			int desiredCapacity, List<Instance> ec2Instances) {
		AutoScalingGroup autoScalingGroup = group(autoScalingGroupName,
				desiredCapacity, ec2Instances);
		LOG.debug("setting up mocked group: {}", Lists.transform(
				autoScalingGroup.getInstances(),
				AwsAutoScalingFunctions.toAutoScalingInstanceId()));

		when(this.mockAwsClient.getAutoScalingGroup(autoScalingGroupName))
				.thenReturn(autoScalingGroup);
		when(
				this.mockAwsClient
						.getAutoScalingGroupMembers(autoScalingGroupName))
				.thenReturn(ec2Instances);
		for (Instance instance : ec2Instances) {
			when(
					this.mockAwsClient.getInstanceMetadata(instance
							.getInstanceId())).thenReturn(instance);
		}

	}
}
