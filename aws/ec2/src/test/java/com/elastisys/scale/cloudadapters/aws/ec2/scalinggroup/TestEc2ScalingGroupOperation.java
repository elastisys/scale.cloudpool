package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.MachinesMatcher.machines;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.TestUtils.config;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.TestUtils.ec2Instance;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.TestUtils.ec2Instances;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.Ec2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;

/**
 * Verifies the operational behavior of the {@link Ec2ScalingGroup}.
 *
 * 
 *
 */
public class TestEc2ScalingGroupOperation {

	static Logger LOG = LoggerFactory
			.getLogger(TestEc2ScalingGroupOperation.class);

	private static final String GROUP_NAME = "MyScalingGroup";

	private static final Filter GROUP_INSTANCE_QUERY_FILTER = new Filter()
			.withName(Constants.SCALING_GROUP_TAG_FILTER_KEY).withValues(
					GROUP_NAME);

	private Ec2Client mockClient = mock(Ec2Client.class);
	/** Object under test. */
	private Ec2ScalingGroup scalingGroup;

	@Before
	public void onSetup() throws ScalingGroupException {
		this.scalingGroup = new Ec2ScalingGroup(this.mockClient);
		this.scalingGroup.configure(config(GROUP_NAME));
	}

	@Test
	public void listMachines() throws ScalingGroupException {
		// empty scaling group
		setUpMockedScalingGroup(GROUP_NAME, ec2Instances());
		assertThat(this.scalingGroup.listMachines(), is(machines()));
		verify(this.mockClient).getInstances(
				asList(GROUP_INSTANCE_QUERY_FILTER));

		// non-empty group
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(ec2Instance("i-1", "running")));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Instance> members = ec2Instances(ec2Instance("i-1", "running"),
				ec2Instance("i-2", "pending"), ec2Instance("i-3", "terminated"));
		setUpMockedScalingGroup(GROUP_NAME, members);
		assertThat(this.scalingGroup.listMachines(),
				is(machines("i-1", "i-2", "i-3")));
	}

	/**
	 * A {@link ScalingGroupException} should be thrown if listing scaling group
	 * members fails.
	 */
	@Test(expected = ScalingGroupException.class)
	public void listMachinesOnError() throws ScalingGroupException {
		// set up Amazon API call to fail
		List<Filter> queryFilters = Arrays.asList(GROUP_INSTANCE_QUERY_FILTER);
		when(this.mockClient.getInstances(queryFilters)).thenThrow(
				new AmazonServiceException("API unreachable"));
		this.scalingGroup.listMachines();
	}

	@Test
	public void startMachines() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// scale up from 0 -> 1
		List<Instance> instances = ec2Instances();
		FakeEc2Client fakeEc2Client = new FakeEc2Client(instances);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);
		List<Machine> startedMachines = this.scalingGroup.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-1")));
		// verify that group/name tag was set on instance
		assertThat(fakeEc2Client.getInstanceMetadata("i-1").getTags(),
				is(expectedInstanceTags("i-1")));

		// scale up from 1 -> 2
		instances = ec2Instances(ec2Instance("i-1", "running"));
		fakeEc2Client = new FakeEc2Client(instances);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));
		// verify that group/name tag was set on instance
		assertThat(fakeEc2Client.getInstanceMetadata("i-2").getTags(),
				is(expectedInstanceTags("i-2")));

		// scale up from 2 -> 4
		instances = ec2Instances(ec2Instance("i-1", "running"),
				ec2Instance("i-2", "pending"));
		fakeEc2Client = new FakeEc2Client(instances);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-3", "i-4")));
		// verify that group/name tag was set on instance
		assertThat(fakeEc2Client.getInstanceMetadata("i-3").getTags(),
				is(expectedInstanceTags("i-3")));
		assertThat(fakeEc2Client.getInstanceMetadata("i-4").getTags(),
				is(expectedInstanceTags("i-4")));
	}

	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		ScaleUpConfig scaleUpConfig = config(GROUP_NAME).getScaleUpConfig();

		// set up mock to throw an error whenever asked to launch an instance
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockClient).launchInstance(scaleUpConfig);

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
	public void startMachinesOnPartialFailure() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// set up mock to throw an error when asked to launch second instance
		// (first should succeed)
		int numLaunchesBeforeFailure = 1;
		FakeEc2Client fakeEc2Client = new FailingFakeEc2Client(ec2Instances(),
				numLaunchesBeforeFailure);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);

		// should raise an exception on second attempt to launch
		try {
			this.scalingGroup.startMachines(2, scaleUpConfig);
			fail("startMachines expected to fail");
		} catch (StartMachinesException e) {
			assertThat(e.getRequestedMachines(), is(2));
			assertThat(e.getStartedMachines().size(),
					is(numLaunchesBeforeFailure));
		}
	}

	@Test
	public void terminate() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);

		this.scalingGroup = new Ec2ScalingGroup(new FakeEc2Client(ec2Instances(
				ec2Instance("i-1", "running"), ec2Instance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		this.scalingGroup.terminateMachine("i-1");
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));

		this.scalingGroup.terminateMachine("i-2");
		assertThat(this.scalingGroup.listMachines(), is(machines()));
	}

	@Test(expected = ScalingGroupException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateInstance is called
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(ec2Instance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockClient).terminateInstance("i-1");

		this.scalingGroup.terminateMachine("i-1");
	}

	private void setUpMockedScalingGroup(String groupName,
			List<Instance> groupMembers) {
		// set up response to queries for group member instances
		List<Filter> groupQueryFilters = asList(GROUP_INSTANCE_QUERY_FILTER);
		when(this.mockClient.getInstances(groupQueryFilters)).thenReturn(
				groupMembers);

		// set up response to queries for group member meta data
		for (Instance instance : groupMembers) {
			when(this.mockClient.getInstanceMetadata(instance.getInstanceId()))
					.thenReturn(instance);
		}
	}

	/**
	 * Return the list of tags expected to be set on an instance in the scaling
	 * group.
	 *
	 * @param instanceId
	 *            the instance id.
	 * @return
	 */
	private List<Tag> expectedInstanceTags(String instanceId) {
		Tag nameTag = new Tag().withKey(Constants.NAME_TAG).withValue(
				GROUP_NAME + "-" + instanceId);
		Tag groupTag = new Tag().withKey(Constants.SCALING_GROUP_TAG)
				.withValue(GROUP_NAME);
		return Arrays.asList(nameTag, groupTag);
	}
}
