package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

import static com.elastisys.scale.cloudadapers.api.types.ServiceState.BOOTING;
import static com.elastisys.scale.cloudadapers.api.types.ServiceState.IN_SERVICE;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.Constants.SCALING_GROUP_TAG;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.Constants.SERVICE_STATE_TAG;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.MachinesMatcher.machines;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.TestUtils.config;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.TestUtils.servers;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jersey.repackaged.com.google.common.collect.Maps;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.OpenstackClient;
import com.google.common.collect.ImmutableMap;

/**
 * Verifies the behavior of the {@link OpenStackScalingGroup} as it operates.
 */
public class TestOpenStackScalingGroupOperation {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestOpenStackScalingGroupOperation.class);

	private static final String GROUP_NAME = "MyScalingGroup";

	private OpenstackClient mockClient = mock(OpenstackClient.class);
	/** Object under test. */
	private OpenStackScalingGroup scalingGroup;

	@Before
	public void onSetup() {
		this.scalingGroup = new OpenStackScalingGroup(this.mockClient);
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME, true));
	}

	/**
	 * {@link ScalingGroup#listMachines()} should delegate to
	 * {@link OpenstackClient} and basically return anything it returns.
	 */
	@Test
	public void listMachines() throws ScalingGroupException {
		// empty scaling group
		setUpMockedScalingGroup(GROUP_NAME, servers());
		assertThat(this.scalingGroup.listMachines(), is(machines()));
		verify(this.mockClient).getServers(Constants.SCALING_GROUP_TAG,
				GROUP_NAME);

		// non-empty group
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Server> members = servers(memberServer("i-1", Status.ACTIVE),
				memberServer("i-2", Status.BUILD),
				memberServer("i-3", Status.DELETED));
		setUpMockedScalingGroup(GROUP_NAME, members);
		List<Machine> machines = this.scalingGroup.listMachines();
		assertThat(machines, is(machines("i-1", "i-2", "i-3")));
		// verify that cloud-specific metadata is included for each machine
		assertTrue(machines.get(0).getMetadata().has("id"));
		assertTrue(machines.get(1).getMetadata().has("id"));
		assertTrue(machines.get(2).getMetadata().has("id"));
	}

	/**
	 * A {@link ScalingGroupException} should be thrown if listing scaling group
	 * members fails.
	 */
	@Test(expected = ScalingGroupException.class)
	public void listMachinesOnError() throws ScalingGroupException {
		// set up Amazon API call to fail
		when(this.mockClient.getServers(SCALING_GROUP_TAG, GROUP_NAME))
		.thenThrow(new RuntimeException("API unreachable"));
		this.scalingGroup.listMachines();
	}

	/**
	 * Started machines should the group membership should be marked as a meta
	 * data tag. If requested, floating IP addresses should also be assigned to
	 * new machines.
	 */
	@Test
	public void startMachines() throws Exception {
		boolean assignFloatingIp = true;
		BaseCloudAdapterConfig config = config(GROUP_NAME, assignFloatingIp);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// scale up from 0 -> 1
		List<Server> servers = servers();
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		List<Machine> startedMachines = this.scalingGroup.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-1")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));
		// verify that a floating IP address was assigned to machine
		assertThat(startedMachines.get(0).getPublicIps().size(), is(1));

		// scale up from 1 -> 2
		servers = servers(memberServer("i-1", Status.ACTIVE));
		fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-2")), is(GROUP_NAME));

		// scale up from 2 -> 4
		servers = servers(memberServer("i-1", Status.ACTIVE),
				memberServer("i-2", Status.BUILD));
		fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-3", "i-4")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-3")), is(GROUP_NAME));
		assertThat(membershipTag(fakeClient.getServer("i-4")), is(GROUP_NAME));
	}

	/**
	 * It should be possible to not acquire floating IP addresses to new
	 * instances.
	 */
	@Test
	public void startMachinesWithoutFloatingIp() {
		boolean assignFloatingIp = false;
		BaseCloudAdapterConfig config = config(GROUP_NAME, assignFloatingIp);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// scale up from 0 -> 1
		List<Server> servers = servers();
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		List<Machine> startedMachines = this.scalingGroup.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-1")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));
		// verify that no floating IP address was assigned to machine
		assertThat(startedMachines.get(0).getPublicIps().size(), is(0));
	}

	/**
	 * On cloud API errors, a {@link StartMachinesException} should be thrown.
	 */
	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		ScaleUpConfig scaleUpConfig = config(GROUP_NAME, true)
				.getScaleUpConfig();

		// set up mock to throw an error whenever asked to launch an instance
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		when(
				this.mockClient.launchServer(any(String.class),
						any(ScaleUpConfig.class), any(Map.class))).thenThrow(
								new RuntimeException("API unreachable"));

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
		BaseCloudAdapterConfig config = config(GROUP_NAME, true);
		ScaleUpConfig scaleUpConfig = config.getScaleUpConfig();

		// set up mock to throw an error when asked to launch second instance
		// (first should succeed)
		int numLaunchesBeforeFailure = 1;
		FakeOpenstackClient fakeClient = new FailingFakeOpenstackClient(
				servers(), numLaunchesBeforeFailure);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
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
		BaseCloudAdapterConfig config = config(GROUP_NAME, true);

		this.scalingGroup = new OpenStackScalingGroup(new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE),
						memberServer("i-2", Status.BUILD))));
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
		// set up mock to throw an error whenever terminateServer is called
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
		.terminateServer("i-1");

		this.scalingGroup.terminateMachine("i-1");
	}

	/**
	 * It should not be possible to terminate a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void terminateOnNonGroupMember() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.scalingGroup.terminateMachine("i-2");
	}

	/**
	 * Verify that the group membership tag is removed from the server when
	 * detaching a group member.
	 */
	@Test
	public void detach() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE)));
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME, true));
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));

		this.scalingGroup.detachMachine("i-1");
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(nullValue()));
	}

	/**
	 * It should not be possible to detach a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void detachOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.scalingGroup.detachMachine("i-2");
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to untag an
	 * instance that is to be detached from the group.
	 */
	@Test(expected = ScalingGroupException.class)
	public void detachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
		.untagServer("i-1", Arrays.asList(SCALING_GROUP_TAG));

		this.scalingGroup.detachMachine("i-1");
	}

	/**
	 * Verifies that a group membership tag gets set on instances that are
	 * attached to the group.
	 */
	@Test
	public void attach() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(nonMemberServer("i-1", Status.ACTIVE)));
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME, true));
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(nullValue()));

		this.scalingGroup.attachMachine("i-1");
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));
	}

	/**
	 * An attempt to attach a non-existing machine should result in
	 * {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void attachNonExistingMachine() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(nonMemberServer("i-1", Status.ACTIVE)));
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME, true));

		this.scalingGroup.attachMachine("i-2");
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to tag an
	 * instance that is to be attached to the group.
	 */
	@Test(expected = ScalingGroupException.class)
	public void attachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(nonMemberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
		.tagServer("i-1",
				ImmutableMap.of(SCALING_GROUP_TAG, GROUP_NAME));

		this.scalingGroup.attachMachine("i-1");
	}

	/**
	 * Verifies that a
	 * {@link ScalingGroup#setServiceState(String, ServiceState)} stores the
	 * state by setting a tag on the server.
	 */
	@Test
	public void setServiceState() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE)));
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config(GROUP_NAME, true));
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(nullValue()));

		this.scalingGroup.setServiceState("i-1", BOOTING);
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(BOOTING.name()));

		this.scalingGroup.setServiceState("i-1", IN_SERVICE);
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(IN_SERVICE.name()));
	}

	/**
	 * It should not be possible to set service state on a machine instance that
	 * is not recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void setServiceStateOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.scalingGroup.setServiceState("i-2", ServiceState.IN_SERVICE);
	}

	/**
	 * A {@link ScalingGroupException} should be thrown on failure to tag the
	 * service state of a group instance.
	 */
	@Test(expected = ScalingGroupException.class)
	public void setServiceStateOnError() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
		.tagServer("i-1",
				ImmutableMap.of(SERVICE_STATE_TAG, IN_SERVICE.name()));

		this.scalingGroup.setServiceState("i-1", IN_SERVICE);
	}

	private void setUpMockedScalingGroup(String groupName,
			List<Server> groupMembers) {
		// set up response to queries for group member instances
		when(this.mockClient.getServers(Constants.SCALING_GROUP_TAG, groupName))
		.thenReturn(groupMembers);

		// set up response to queries for group member meta data
		for (Server server : groupMembers) {
			when(this.mockClient.getServer(server.getId())).thenReturn(server);
		}
	}

	/**
	 * Gets the group membership tag ({@link Constants#SCALING_GROUP_TAG}) for a
	 * {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String membershipTag(Server server) {
		return server.getMetadata().get(SCALING_GROUP_TAG);
	}

	/**
	 * Gets the service state tag ({@link Constants#SERVICE_STATE_TAG}) for a
	 * {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String serviceStateTag(Server server) {
		return server.getMetadata().get(SERVICE_STATE_TAG);
	}

	private static Map<String, String> groupMemberTags() {
		return ImmutableMap.of(SCALING_GROUP_TAG, GROUP_NAME);
	}

	/**
	 * Creates a group member {@link Server} with the group membership tag set.
	 *
	 * @param id
	 * @param status
	 * @return
	 */
	private static Server memberServer(String id, Status status) {
		return TestUtils.server(id, status, groupMemberTags());
	}

	/**
	 * Creates a group member {@link Server} without the group membership tag
	 * set.
	 *
	 * @param id
	 * @param status
	 * @return
	 */
	private static Server nonMemberServer(String id, Status status) {
		Map<String, String> tags = Maps.newHashMap();
		return TestUtils.server(id, status, tags);
	}

}