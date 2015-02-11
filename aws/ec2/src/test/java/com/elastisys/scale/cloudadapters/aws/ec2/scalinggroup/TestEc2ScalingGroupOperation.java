package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import static com.elastisys.scale.cloudadapers.api.types.ServiceState.BOOTING;
import static com.elastisys.scale.cloudadapers.api.types.ServiceState.IN_SERVICE;
import static com.elastisys.scale.cloudadapters.aws.commons.ScalingTags.SCALING_GROUP_TAG;
import static com.elastisys.scale.cloudadapters.aws.commons.ScalingTags.SERVICE_STATE_TAG;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.MachinesMatcher.machines;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.TestUtils.config;
import static com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.TestUtils.ec2Instances;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.aws.commons.ScalingTags;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.Ec2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;

/**
 * Verifies the operational behavior of the {@link Ec2ScalingGroup}.
 *
 *
 *
 */
public class TestEc2ScalingGroupOperation {

	private static Logger LOG = LoggerFactory
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

	/**
	 * {@link ScalingGroup#listMachines()} should delegate to {@link Ec2Client}
	 * and basically return anything it returns.
	 */
	@Test
	public void listMachines() throws ScalingGroupException {
		// empty scaling group
		setUpMockedScalingGroup(GROUP_NAME, ec2Instances());
		assertThat(this.scalingGroup.listMachines(), is(machines()));
		verify(this.mockClient).getInstances(
				asList(GROUP_INSTANCE_QUERY_FILTER));

		// non-empty group
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Instance> members = ec2Instances(memberInstance("i-1", "running"),
				memberInstance("i-2", "pending"),
				memberInstance("i-3", "terminated"));
		setUpMockedScalingGroup(GROUP_NAME, members);
		List<Machine> machines = this.scalingGroup.listMachines();
		assertThat(machines, is(machines("i-1", "i-2", "i-3")));
		// verify that cloud-specific metadata is included for each machine
		assertTrue(machines.get(0).getMetadata().has("instanceId"));
		assertTrue(machines.get(1).getMetadata().has("instanceId"));
		assertTrue(machines.get(2).getMetadata().has("instanceId"));
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

	/**
	 * Started machines should the group membership should be marked with an
	 * instance tag.
	 */
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
		assertThat(membershipTag(fakeEc2Client.getInstanceMetadata("i-1")),
				is(GROUP_NAME));

		// scale up from 1 -> 2
		instances = ec2Instances(memberInstance("i-1", "running"));
		fakeEc2Client = new FakeEc2Client(instances);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeEc2Client.getInstanceMetadata("i-2")),
				is(GROUP_NAME));

		// scale up from 2 -> 4
		instances = ec2Instances(memberInstance("i-1", "running"),
				memberInstance("i-2", "pending"));
		fakeEc2Client = new FakeEc2Client(instances);
		this.scalingGroup = new Ec2ScalingGroup(fakeEc2Client);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-3", "i-4")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeEc2Client.getInstanceMetadata("i-3")),
				is(GROUP_NAME));
		assertThat(membershipTag(fakeEc2Client.getInstanceMetadata("i-4")),
				is(GROUP_NAME));
	}

	/**
	 * On cloud API errors, a {@link StartMachinesException} should be thrown.
	 */
	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		ScaleUpConfig scaleUpConfig = config(GROUP_NAME).getScaleUpConfig();

		// set up mock to throw an error whenever asked to launch an instance
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));
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

	/**
	 * Any machines that were started prior to encountering an error should be
	 * listed in {@link StartMachinesException}s.
	 */
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

	/**
	 * Verifies behavior when terminating a group member.
	 */
	@Test
	public void terminate() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);

		this.scalingGroup = new Ec2ScalingGroup(new FakeEc2Client(ec2Instances(
				memberInstance("i-1", "running"),
				memberInstance("i-2", "pending"))));
		this.scalingGroup.configure(config);
		this.scalingGroup.terminateMachine("i-1");
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));

		this.scalingGroup.terminateMachine("i-2");
		assertThat(this.scalingGroup.listMachines(), is(machines()));
	}

	/**
	 * On client error, a {@link ScalingGroupException} should be raised.
	 */
	@Test(expected = ScalingGroupException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateInstance is called
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));
		doThrow(new AmazonClientException("API unreachable")).when(
				this.mockClient).terminateInstance("i-1");

		this.scalingGroup.terminateMachine("i-1");
	}

	/**
	 * It should not be possible to terminate a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void terminateOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));

		this.scalingGroup.terminateMachine("i-2");
	}

	/**
	 * Verify that the group membership tag is removed from the server when
	 * detaching a group member.
	 */
	@Test
	public void detach() {
		FakeEc2Client fakeClient = new FakeEc2Client(
				ec2Instances(memberInstance("i-1", "running")));
		this.scalingGroup = new Ec2ScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME));
		assertThat(membershipTag(fakeClient.getInstanceMetadata("i-1")),
				is(GROUP_NAME));

		this.scalingGroup.detachMachine("i-1");
		assertThat(membershipTag(fakeClient.getInstanceMetadata("i-1")),
				is(nullValue()));
	}

	/**
	 * It should not be possible to detach a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void detachOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));

		this.scalingGroup.detachMachine("i-2");
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to untag an
	 * instance that is to be detached from the group.
	 */
	@Test(expected = ScalingGroupException.class)
	public void detachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));

		List<Tag> groupTag = Arrays.asList(new Tag().withKey(SCALING_GROUP_TAG)
				.withValue(GROUP_NAME));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.untagInstance("i-1", groupTag);

		this.scalingGroup.detachMachine("i-1");
	}

	/**
	 * Verifies that a group membership tag gets set on instances that are
	 * attached to the group.
	 */
	@Test
	public void attach() {
		FakeEc2Client fakeClient = new FakeEc2Client(
				ec2Instances(nonMemberInstance("i-1", "running")));
		this.scalingGroup = new Ec2ScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME));
		assertThat(membershipTag(fakeClient.getInstanceMetadata("i-1")),
				is(nullValue()));

		this.scalingGroup.attachMachine("i-1");
		assertThat(membershipTag(fakeClient.getInstanceMetadata("i-1")),
				is(GROUP_NAME));
	}

	/**
	 * An attempt to attach a non-existing machine should result in
	 * {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void attachNonExistingMachine() {
		FakeEc2Client fakeClient = new FakeEc2Client(
				ec2Instances(nonMemberInstance("i-1", "running")));

		this.scalingGroup = new Ec2ScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME));

		this.scalingGroup.attachMachine("i-2");
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to tag an
	 * instance that is to be attached to the group.
	 */
	@Test(expected = ScalingGroupException.class)
	public void attachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(nonMemberInstance("i-1", "running")));
		List<Tag> groupTag = Arrays.asList(new Tag().withKey(SCALING_GROUP_TAG)
				.withValue(GROUP_NAME));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.tagInstance("i-1", groupTag);

		this.scalingGroup.attachMachine("i-1");
	}

	/**
	 * Verifies that a
	 * {@link ScalingGroup#setServiceState(String, ServiceState)} stores the
	 * state by setting a tag on the server.
	 */
	@Test
	public void setServiceState() {
		FakeEc2Client fakeClient = new FakeEc2Client(
				ec2Instances(memberInstance("i-1", "running")));
		this.scalingGroup = new Ec2ScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME));
		assertThat(serviceStateTag(fakeClient.getInstanceMetadata("i-1")),
				is(nullValue()));

		this.scalingGroup.setServiceState("i-1", BOOTING);
		assertThat(serviceStateTag(fakeClient.getInstanceMetadata("i-1")),
				is(BOOTING.name()));

		this.scalingGroup.setServiceState("i-1", IN_SERVICE);
		assertThat(serviceStateTag(fakeClient.getInstanceMetadata("i-1")),
				is(IN_SERVICE.name()));
	}

	/**
	 * It should not be possible to set service state on a machine instance that
	 * is not recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void setServiceStateOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));

		this.scalingGroup.setServiceState("i-2", ServiceState.IN_SERVICE);
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to tag the
	 * service state of a group instance.
	 */
	@Test(expected = ScalingGroupException.class)
	public void setServiceStateOnError() {
		setUpMockedScalingGroup(GROUP_NAME,
				ec2Instances(memberInstance("i-1", "running")));

		List<Tag> serviceStateTag = asList(new Tag().withKey(SERVICE_STATE_TAG)
				.withValue(IN_SERVICE.name()));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.tagInstance("i-1", serviceStateTag);

		this.scalingGroup.setServiceState("i-1", IN_SERVICE);
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
	 * Gets the group membership tag ({@link Constants#SCALING_GROUP_TAG}) for a
	 * {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String membershipTag(Instance instance) {
		return getTag(instance, ScalingTags.SCALING_GROUP_TAG);
	}

	/**
	 * Gets the service state tag ({@link Constants#SERVICE_STATE_TAG}) for a
	 * {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String serviceStateTag(Instance instance) {
		return getTag(instance, SERVICE_STATE_TAG);
	}

	/**
	 * Retrieves a particular meta data tag value from an {@link Instance} or
	 * return <code>null</code> if no such tag key is set on the instance.
	 *
	 * @param instance
	 * @param tagKey
	 * @return The value set for the key or <code>null</code> if not found.
	 */
	private static String getTag(Instance instance, String tagKey) {
		List<Tag> tags = instance.getTags();
		for (Tag tag : tags) {
			if (tag.getKey().equals(tagKey)) {
				return tag.getValue();
			}
		}
		return null;
	}

	private static Instance memberInstance(String id, String state) {
		List<Tag> tags = Arrays.asList(new Tag().withKey(
				ScalingTags.SCALING_GROUP_TAG).withValue(GROUP_NAME));
		return TestUtils.ec2Instance(id, state, tags);
	}

	private static Instance nonMemberInstance(String id, String state) {
		List<Tag> tags = Arrays.asList();
		return TestUtils.ec2Instance(id, state, tags);
	}

}
