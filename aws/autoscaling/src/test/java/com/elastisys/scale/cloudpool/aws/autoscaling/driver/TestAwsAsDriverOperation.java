package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.elastisys.scale.cloudpool.api.types.ServiceState.IN_SERVICE;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver.REQUESTED_ID_PREFIX;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.MachinesMatcher.machines;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.config;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.ec2Instance;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.ec2Instances;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.group;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingTags.SERVICE_STATE_TAG;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsAutoScalingFunctions;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.google.common.collect.Lists;

/**
 * Verifies the operational behavior of the {@link AwsAsPoolDriver}.
 *
 *
 *
 */
public class TestAwsAsDriverOperation {
	static Logger LOG = LoggerFactory
			.getLogger(TestAwsAsDriverOperation.class);

	private static final String GROUP_NAME = "MyScalingGroup";

	/** Object under test. */
	private AwsAsPoolDriver scalingGroup;

	private AutoScalingClient mockAwsClient = mock(AutoScalingClient.class);

	@Before
	public void onSetup() throws CloudPoolDriverException {
		this.scalingGroup = new AwsAsPoolDriver(this.mockAwsClient);
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));
	}

	/**
	 * {@link CloudPoolDriver#listMachines()} should delegate to
	 * {@link AutoScalingClient} and basically return anything it returns.
	 */
	@Test
	public void listMachines() throws CloudPoolDriverException {
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
		List<Machine> machines = this.scalingGroup.listMachines();
		assertThat(machines, is(machines("i-1", "i-2", "i-3")));
		// verify that listMachines returns cloud-specific metadata about each
		// machine
		assertTrue(machines.get(0).getMetadata().has("instanceId"));
		assertTrue(machines.get(1).getMetadata().has("instanceId"));
		assertTrue(machines.get(2).getMetadata().has("instanceId"));
	}

	/**
	 * If the actual size of the Auto Scaling Group is less than desired
	 * capacity (for example, when there is a shortage in supply of instances)
	 * the {@link AwsAsPoolDriver} should report the missing instances as
	 * being pseudo instances in state {@link MachineState#REQUESTED}, to not
	 * fool the {@link BaseCloudPool} from believing that the
	 * {@link CloudPoolDriver} is too small and order new machines to be started
	 * (and increase the desired capacity even more).
	 */
	@Test
	public void listMachinesOnGroupThatHasNotReachedDesiredCapacity()
			throws CloudPoolDriverException {
		// two missing instances: desired capacity: 3, actual capacity: 1
		int desiredCapacity = 3;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		List<Machine> groupMembers = this.scalingGroup.listMachines();
		assertThat(groupMembers,
				is(machines("i-1", "i-requested1", "i-requested2")));
		assertThat(groupMembers.get(0).getMachineState(),
				is(MachineState.RUNNING));
		assertThat(groupMembers.get(1).getMachineState(),
				is(MachineState.REQUESTED));
		assertThat(groupMembers.get(2).getMachineState(),
				is(MachineState.REQUESTED));
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown if listing Auto
	 * Scaling Group members fails.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void listMachinesOnError() throws CloudPoolDriverException {
		// set up Amazon API call to fail
		when(this.mockAwsClient.getAutoScalingGroup(GROUP_NAME)).thenThrow(
				new AmazonServiceException("API unreachable"));
		this.scalingGroup.listMachines();
	}

	/**
	 * Starting machines should result in incrementing the desiredCapacity of
	 * the group and "placeholder instances" (with pseudo identifiers) should be
	 * returned.
	 */
	@Test
	public void startMachines() throws Exception {
		BaseCloudPoolConfig config = config(GROUP_NAME);
		ScaleOutConfig scaleUpConfig = config.getScaleOutConfig();

		// scale up from 0 -> 1
		int desiredCapacity = 0;
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances()));
		this.scalingGroup.configure(config);
		List<Machine> startedMachines = this.scalingGroup.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-requested1")));

		// scale up from 1 -> 2
		desiredCapacity = 1;
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(ec2Instance("i-1",
						"running"))));
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-requested1")));

		// scale up from 2 -> 4
		desiredCapacity = 2;
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(
						ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines,
				is(machines("i-requested1", "i-requested2")));
	}

	/**
	 * A {@link CloudPoolDriverException} should be raised on failure to start
	 * more instances.
	 */
	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		// set up mock to throw an error whenever setDesiredSize is called
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockAwsClient).setDesiredSize(GROUP_NAME, 2);

		ScaleOutConfig scaleUpConfig = config(GROUP_NAME).getScaleOutConfig();
		// should raise an exception
		try {
			this.scalingGroup.startMachines(1, scaleUpConfig);
			fail("startMachines expected to fail");
		} catch (StartMachinesException e) {
			assertThat(e.getRequestedMachines(), is(1));
			assertThat(e.getStartedMachines().size(), is(0));
		}
	}

	/**
	 * Verifies behavior when terminating an actual group member.
	 */
	@Test
	public void terminate() throws Exception {
		BaseCloudPoolConfig config = config(GROUP_NAME);

		int desiredCapacity = 2;
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(
						ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		this.scalingGroup.terminateMachine("i-1");
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));

		this.scalingGroup.terminateMachine("i-2");
		assertThat(this.scalingGroup.listMachines(), is(machines()));
	}

	/**
	 * Verifies behavior when terminating an group member that has only been
	 * requested but not yet been acquired by the Auto Scaling Group (and has a
	 * placeholder id such as 'i-requested1').
	 */
	@Test
	public void terminateRequestedInstance() {
		BaseCloudPoolConfig config = config(GROUP_NAME);
		int desiredCapacity = 2;
		FakeAutoScalingClient client = new FakeAutoScalingClient(GROUP_NAME,
				desiredCapacity, ec2Instances(ec2Instance("i-1", "running"),
						ec2Instance("i-2", "pending")));
		this.scalingGroup = new AwsAsPoolDriver(client);
		this.scalingGroup.configure(config);
		assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(),
				is(2));

		// request scaling group to grow by one
		this.scalingGroup.startMachines(1, config.getScaleOutConfig());
		final String requestedMachineId = REQUESTED_ID_PREFIX + "1";
		// we should now have incremented the desired capacity of the Auto
		// Scaling Group and have one requested machine that is faked by the
		// group
		assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(),
				is(3));
		assertThat(this.scalingGroup.listMachines(),
				is(machines("i-1", "i-2", requestedMachineId)));

		// now ask scaling group to terminate the requested instance (this
		// should only result in the desired capacity being decremented, not
		// attempting to actually terminate the fake instance)
		this.scalingGroup.terminateMachine(requestedMachineId);
		assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(),
				is(2));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1", "i-2")));
	}

	/**
	 * On a client error, a {@link CloudPoolDriverException} should be raised.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateInstance is called
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockAwsClient).terminateInstance(GROUP_NAME, "i-1");

		this.scalingGroup.terminateMachine("i-1");
	}

	/**
	 * It should not be possible to terminate a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void terminateOnNonGroupMember() throws Exception {
		int desiredCapacity = 1;
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"));
		List<Instance> nonMembers = ec2Instances(ec2Instance("i-2", "running"));
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, members, nonMembers));
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));

		this.scalingGroup.terminateMachine("i-2");
	}

	/**
	 * Verify that the detach is called on the Auto Scaling Group API when
	 * detaching a group member.
	 */
	@Test
	public void detach() {
		int desiredCapacity = 1;
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"));
		List<Instance> nonMembers = ec2Instances();
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, members, nonMembers));
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));

		this.scalingGroup.detachMachine("i-1");
		List<String> emptyList = asList();
		assertThat(groupIds(this.scalingGroup), is(emptyList));
	}

	/**
	 * It should not be possible to detach a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void detachOnNonGroupMember() {
		int desiredCapacity = 1;
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"));
		List<Instance> nonMembers = ec2Instances(ec2Instance("i-2", "running"));
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, members, nonMembers));
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));

		this.scalingGroup.detachMachine("i-2");
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to detach
	 * an instance from the group.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void detachOnError() throws Exception {
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));

		doThrow(new RuntimeException("API unreachable")).when(
				this.mockAwsClient).detachInstance(GROUP_NAME, "i-1");

		this.scalingGroup.detachMachine("i-1");
	}

	/**
	 * Verifies that attach is called on the Auto Scaling Group API when
	 * instances are attached to the group.
	 */
	@Test
	public void attach() {
		int desiredCapacity = 1;
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"));
		List<Instance> nonMembers = ec2Instances(ec2Instance("i-2", "running"));
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, members, nonMembers));
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));

		assertThat(groupIds(this.scalingGroup), is(Arrays.asList("i-1")));
		this.scalingGroup.attachMachine("i-2");
		assertThat(groupIds(this.scalingGroup), is(Arrays.asList("i-1", "i-2")));
	}

	/**
	 * An attempt to attach a non-existing machine should result in
	 * {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void attachNonExistingMachine() {
		int desiredCapacity = 0;
		this.scalingGroup = new AwsAsPoolDriver(new FakeAutoScalingClient(
				GROUP_NAME, desiredCapacity, ec2Instances(), ec2Instances()));
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));

		this.scalingGroup.attachMachine("i-3");
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to call
	 * attach on the Auto Scaling Group API when instances are attached to the
	 * group.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void attachOnError() {
		int desiredCapacity = 1;
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"));
		List<Instance> nonMembers = ec2Instances(ec2Instance("i-2", "running"));
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity, members,
				nonMembers);

		doThrow(new RuntimeException("API unreachable")).when(
				this.mockAwsClient).attachInstance(GROUP_NAME, "i-2");

		this.scalingGroup.attachMachine("i-2");
	}

	/**
	 * Verifies that a
	 * {@link CloudPoolDriver#setServiceState(String, ServiceState)} stores the
	 * state by setting a tag on the instance.
	 */
	@Test
	public void setServiceState() {
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));

		List<Tag> stateTag = asList(new Tag().withKey(
				ScalingTags.SERVICE_STATE_TAG).withValue(
				ServiceState.IN_SERVICE.name()));
		this.scalingGroup.setServiceState("i-1", ServiceState.IN_SERVICE);

		verify(this.mockAwsClient).tagInstance("i-1", stateTag);
	}

	/**
	 * It should not be possible to set service state on a machine instance that
	 * is not recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void setServiceStateOnNonGroupMember() {
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));

		this.scalingGroup.setServiceState("i-2", ServiceState.IN_SERVICE);
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to tag the
	 * service state of a group instance.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void setServiceStateOnError() {
		int desiredCapacity = 1;
		setUpMockedAutoScalingGroup(GROUP_NAME, desiredCapacity,
				ec2Instances(ec2Instance("i-1", "running")));

		List<Tag> serviceStateTag = asList(new Tag().withKey(SERVICE_STATE_TAG)
				.withValue(IN_SERVICE.name()));
		doThrow(new RuntimeException("API unreachable")).when(
				this.mockAwsClient).tagInstance("i-1", serviceStateTag);

		this.scalingGroup.setServiceState("i-1", IN_SERVICE);
	}

	/**
	 * Sets up a fake {@link AutoScalingClient} with a fake pool containing only
	 * instances that are actual members of the Auto Scaling Group.
	 *
	 * @param autoScalingGroupName
	 * @param desiredCapacity
	 * @param memberInstances
	 */
	private void setUpMockedAutoScalingGroup(String autoScalingGroupName,
			int desiredCapacity, List<Instance> memberInstances) {
		setUpMockedAutoScalingGroup(autoScalingGroupName, desiredCapacity,
				memberInstances, new ArrayList<Instance>());
	}

	/**
	 * Sets up a fake {@link AutoScalingClient} with a fake pool containing both
	 * instances that are members of the Auto Scaling Group and instances that
	 * are not members.
	 *
	 * @param autoScalingGroupName
	 * @param desiredCapacity
	 * @param memberInstances
	 *            Auto Scaling Group members.
	 * @param nonMemberInstances
	 *            EC2 instances that exist in the (fake) cloud but aren't
	 *            members of the Auto Scaling Group.
	 */
	private void setUpMockedAutoScalingGroup(String autoScalingGroupName,
			int desiredCapacity, List<Instance> memberInstances,
			List<Instance> nonMemberInstances) {
		AutoScalingGroup autoScalingGroup = group(autoScalingGroupName,
				desiredCapacity, memberInstances);
		LOG.debug("setting up mocked group: {}", Lists.transform(
				autoScalingGroup.getInstances(),
				AwsAutoScalingFunctions.toAutoScalingInstanceId()));

		when(this.mockAwsClient.getAutoScalingGroup(autoScalingGroupName))
				.thenReturn(autoScalingGroup);
		when(
				this.mockAwsClient
						.getAutoScalingGroupMembers(autoScalingGroupName))
				.thenReturn(memberInstances);
	}

	/**
	 * Return the group member ids.
	 *
	 * @param scalingGroup
	 * @return
	 */
	private static List<String> groupIds(CloudPoolDriver scalingGroup) {
		return Lists.transform(scalingGroup.listMachines(), Machine.toId());
	}

}
