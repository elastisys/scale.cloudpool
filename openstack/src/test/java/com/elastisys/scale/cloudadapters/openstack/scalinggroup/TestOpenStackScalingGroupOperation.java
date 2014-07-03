package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.Constants.SCALING_GROUP_TAG;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.MachinesMatcher.machines;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.TestUtils.config;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.TestUtils.server;
import static com.elastisys.scale.cloudadapters.openstack.scalinggroup.TestUtils.servers;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.OpenstackClient;

/**
 * Verifies the behavior of the {@link OpenStackScalingGroup} as it operates.
 *
 * 
 *
 */
public class TestOpenStackScalingGroupOperation {

	private static final String GROUP_NAME = "MyScalingGroup";

	private OpenstackClient mockClient = mock(OpenstackClient.class);
	/** Object under test. */
	private OpenStackScalingGroup scalingGroup;

	@Before
	public void onSetup() {
		this.scalingGroup = new OpenStackScalingGroup(this.mockClient);
		this.scalingGroup.configure(TestUtils.config(GROUP_NAME));
	}

	@Test
	public void listMachines() throws ScalingGroupException {
		// empty scaling group
		setUpMockedScalingGroup(GROUP_NAME, servers());
		assertThat(this.scalingGroup.listMachines(), is(machines()));
		verify(this.mockClient).getServers(Constants.SCALING_GROUP_TAG,
				GROUP_NAME);

		// non-empty group
		setUpMockedScalingGroup(GROUP_NAME,
				servers(server("i-1", Status.ACTIVE)));
		assertThat(this.scalingGroup.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Server> members = servers(server("i-1", Status.ACTIVE),
				server("i-2", Status.BUILD), server("i-3", Status.DELETED));
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
		when(this.mockClient.getServers(SCALING_GROUP_TAG, GROUP_NAME))
				.thenThrow(new RuntimeException("API unreachable"));
		this.scalingGroup.listMachines();
	}

	@Test
	public void startMachines() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);
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
		assertGroupTag(fakeClient.getServer("i-1"));

		// scale up from 1 -> 2
		servers = servers(server("i-1", Status.ACTIVE));
		fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));
		// verify that group/name tag was set on instance
		assertGroupTag(fakeClient.getServer("i-2"));

		// scale up from 2 -> 4
		servers = servers(server("i-1", Status.ACTIVE),
				server("i-2", Status.BUILD));
		fakeClient = new FakeOpenstackClient(servers);
		this.scalingGroup = new OpenStackScalingGroup(fakeClient);
		this.scalingGroup.configure(config);
		startedMachines = this.scalingGroup.startMachines(2, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-3", "i-4")));
		// verify that group/name tag was set on instance
		assertGroupTag(fakeClient.getServer("i-3"));
		assertGroupTag(fakeClient.getServer("i-4"));

	}

	@Test
	public void startMachinesOnFailure() throws StartMachinesException {
		ScaleUpConfig scaleUpConfig = config(GROUP_NAME).getScaleUpConfig();

		// set up mock to throw an error whenever asked to launch an instance
		setUpMockedScalingGroup(GROUP_NAME,
				servers(server("i-1", Status.ACTIVE)));

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

	@Test
	public void startMachinesOnPartialFailure() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);
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

	@Test
	public void terminate() throws Exception {
		BaseCloudAdapterConfig config = config(GROUP_NAME);

		this.scalingGroup = new OpenStackScalingGroup(new FakeOpenstackClient(
				servers(server("i-1", Status.ACTIVE),
						server("i-2", Status.BUILD))));
		this.scalingGroup.configure(config);
		this.scalingGroup.terminateMachine("i-1");
		assertThat(this.scalingGroup.listMachines(), is(machines("i-2")));

		this.scalingGroup.terminateMachine("i-2");
		assertThat(this.scalingGroup.listMachines(), is(machines()));
	}

	@Test(expected = ScalingGroupException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateServer is called
		setUpMockedScalingGroup(GROUP_NAME,
				servers(server("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.terminateServer("i-1");

		this.scalingGroup.terminateMachine("i-1");
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
	 * Asserts that a group member server has a
	 * {@link Constants#SCALING_GROUP_TAG} with the group name as value.
	 *
	 * @param allegedGroupMember
	 */
	private void assertGroupTag(Server allegedGroupMember) {
		assertThat(allegedGroupMember.getMetadata().get(SCALING_GROUP_TAG),
				is(GROUP_NAME));
	}

}