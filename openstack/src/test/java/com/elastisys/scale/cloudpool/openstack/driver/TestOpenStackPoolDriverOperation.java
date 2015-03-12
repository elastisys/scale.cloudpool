package com.elastisys.scale.cloudpool.openstack.driver;

import static com.elastisys.scale.cloudpool.openstack.driver.Constants.CLOUD_POOL_TAG;
import static com.elastisys.scale.cloudpool.openstack.driver.Constants.MEMBERSHIP_STATUS_TAG;
import static com.elastisys.scale.cloudpool.openstack.driver.Constants.SERVICE_STATE_TAG;
import static com.elastisys.scale.cloudpool.openstack.driver.MachinesMatcher.machines;
import static com.elastisys.scale.cloudpool.openstack.driver.TestUtils.config;
import static com.elastisys.scale.cloudpool.openstack.driver.TestUtils.servers;
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

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.ImmutableMap;

/**
 * Verifies the behavior of the {@link OpenStackPoolDriver} as it operates.
 */
public class TestOpenStackPoolDriverOperation {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestOpenStackPoolDriverOperation.class);

	private static final String GROUP_NAME = "MyScalingGroup";

	private OpenstackClient mockClient = mock(OpenstackClient.class);
	/** Object under test. */
	private OpenStackPoolDriver poolDriver;

	@Before
	public void onSetup() {
		this.poolDriver = new OpenStackPoolDriver(this.mockClient);
		this.poolDriver.configure(TestUtils.config(GROUP_NAME, true));
	}

	/**
	 * {@link CloudPoolDriver#listMachines()} should delegate to
	 * {@link OpenstackClient} and basically return anything it returns.
	 */
	@Test
	public void listMachines() throws CloudPoolDriverException {
		// empty scaling group
		setUpMockedScalingGroup(GROUP_NAME, servers());
		assertThat(this.poolDriver.listMachines(), is(machines()));
		verify(this.mockClient)
				.getServers(Constants.CLOUD_POOL_TAG, GROUP_NAME);

		// non-empty group
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		assertThat(this.poolDriver.listMachines(), is(machines("i-1")));

		// group with machines in different states
		List<Server> members = servers(memberServer("i-1", Status.ACTIVE),
				memberServer("i-2", Status.BUILD),
				memberServer("i-3", Status.DELETED));
		setUpMockedScalingGroup(GROUP_NAME, members);
		List<Machine> machines = this.poolDriver.listMachines();
		assertThat(machines, is(machines("i-1", "i-2", "i-3")));
		// verify that cloud-specific metadata is included for each machine
		assertTrue(machines.get(0).getMetadata().has("id"));
		assertTrue(machines.get(1).getMetadata().has("id"));
		assertTrue(machines.get(2).getMetadata().has("id"));
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown if listing scaling
	 * group members fails.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void listMachinesOnError() throws CloudPoolDriverException {
		// set up Amazon API call to fail
		when(this.mockClient.getServers(CLOUD_POOL_TAG, GROUP_NAME)).thenThrow(
				new RuntimeException("API unreachable"));
		this.poolDriver.listMachines();
	}

	/**
	 * Started machines should the group membership should be marked as a meta
	 * data tag. If requested, floating IP addresses should also be assigned to
	 * new machines.
	 */
	@Test
	public void startMachines() throws Exception {
		boolean assignFloatingIp = true;
		BaseCloudPoolConfig config = config(GROUP_NAME, assignFloatingIp);
		ScaleOutConfig scaleUpConfig = config.getScaleOutConfig();

		// scale up from 0 -> 1
		List<Server> servers = servers();
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(servers);
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config);
		List<Machine> startedMachines = this.poolDriver.startMachines(1,
				scaleUpConfig);
		assertThat(startedMachines, is(machines("i-1")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));
		// verify that a floating IP address was assigned to machine
		assertThat(startedMachines.get(0).getPublicIps().size(), is(1));

		// scale up from 1 -> 2
		servers = servers(memberServer("i-1", Status.ACTIVE));
		fakeClient = new FakeOpenstackClient(servers);
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config);
		startedMachines = this.poolDriver.startMachines(1, scaleUpConfig);
		assertThat(startedMachines, is(machines("i-2")));
		// verify that group/name tag was set on instance
		assertThat(membershipTag(fakeClient.getServer("i-2")), is(GROUP_NAME));

		// scale up from 2 -> 4
		servers = servers(memberServer("i-1", Status.ACTIVE),
				memberServer("i-2", Status.BUILD));
		fakeClient = new FakeOpenstackClient(servers);
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config);
		startedMachines = this.poolDriver.startMachines(2, scaleUpConfig);
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
		BaseCloudPoolConfig config = config(GROUP_NAME, assignFloatingIp);
		ScaleOutConfig scaleUpConfig = config.getScaleOutConfig();

		// scale up from 0 -> 1
		List<Server> servers = servers();
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(servers);
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config);
		List<Machine> startedMachines = this.poolDriver.startMachines(1,
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
		ScaleOutConfig scaleUpConfig = config(GROUP_NAME, true)
				.getScaleOutConfig();

		// set up mock to throw an error whenever asked to launch an instance
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		when(
				this.mockClient.launchServer(any(String.class),
						any(ScaleOutConfig.class), any(Map.class))).thenThrow(
				new RuntimeException("API unreachable"));

		// should raise an exception
		try {
			this.poolDriver.startMachines(1, scaleUpConfig);
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
		BaseCloudPoolConfig config = config(GROUP_NAME, true);
		ScaleOutConfig scaleUpConfig = config.getScaleOutConfig();

		// set up mock to throw an error when asked to launch second instance
		// (first should succeed)
		int numLaunchesBeforeFailure = 1;
		FakeOpenstackClient fakeClient = new FailingFakeOpenstackClient(
				servers(), numLaunchesBeforeFailure);
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config);

		// should raise an exception on second attempt to launch
		try {
			this.poolDriver.startMachines(2, scaleUpConfig);
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
		BaseCloudPoolConfig config = config(GROUP_NAME, true);

		this.poolDriver = new OpenStackPoolDriver(new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE),
						memberServer("i-2", Status.BUILD))));
		this.poolDriver.configure(config);
		this.poolDriver.terminateMachine("i-1");
		assertThat(this.poolDriver.listMachines(), is(machines("i-2")));

		this.poolDriver.terminateMachine("i-2");
		assertThat(this.poolDriver.listMachines(), is(machines()));
	}

	/**
	 * On client error, a {@link CloudPoolDriverException} should be raised.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void terminateOnError() throws Exception {
		// set up mock to throw an error whenever terminateServer is called
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.terminateServer("i-1");

		this.poolDriver.terminateMachine("i-1");
	}

	/**
	 * It should not be possible to terminate a machine instance that is not
	 * recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void terminateOnNonGroupMember() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.poolDriver.terminateMachine("i-2");
	}

	/**
	 * Verify that the group membership tag is removed from the server when
	 * detaching a group member.
	 */
	@Test
	public void detach() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE)));
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config(GROUP_NAME, true));
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(GROUP_NAME));

		this.poolDriver.detachMachine("i-1");
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

		this.poolDriver.detachMachine("i-2");
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to untag
	 * an instance that is to be detached from the group.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void detachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.untagServer("i-1", Arrays.asList(CLOUD_POOL_TAG));

		this.poolDriver.detachMachine("i-1");
	}

	/**
	 * Verifies that a group membership tag gets set on instances that are
	 * attached to the group.
	 */
	@Test
	public void attach() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(nonMemberServer("i-1", Status.ACTIVE)));
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config(GROUP_NAME, true));
		assertThat(membershipTag(fakeClient.getServer("i-1")), is(nullValue()));

		this.poolDriver.attachMachine("i-1");
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
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config(GROUP_NAME, true));

		this.poolDriver.attachMachine("i-2");
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to tag an
	 * instance that is to be attached to the group.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void attachOnError() throws Exception {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(nonMemberServer("i-1", Status.ACTIVE)));
		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.tagServer("i-1", ImmutableMap.of(CLOUD_POOL_TAG, GROUP_NAME));

		this.poolDriver.attachMachine("i-1");
	}

	/**
	 * Verifies that a
	 * {@link CloudPoolDriver#setServiceState(String, ServiceState)} stores the
	 * state by setting a tag on the server.
	 */
	@Test
	public void setServiceState() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE)));
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config(GROUP_NAME, true));
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(nullValue()));

		this.poolDriver.setServiceState("i-1", ServiceState.BOOTING);
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(ServiceState.BOOTING.name()));

		this.poolDriver.setServiceState("i-1", ServiceState.IN_SERVICE);
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(ServiceState.IN_SERVICE.name()));
	}

	/**
	 * It should not be possible to set service state on a machine instance that
	 * is not recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void setServiceStateOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.poolDriver.setServiceState("i-2", ServiceState.IN_SERVICE);
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to tag the
	 * service state of a group instance.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void setServiceStateOnError() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.tagServer(
						"i-1",
						ImmutableMap.of(SERVICE_STATE_TAG,
								ServiceState.IN_SERVICE.name()));

		this.poolDriver.setServiceState("i-1", ServiceState.IN_SERVICE);
	}

	/**
	 * Verifies that a
	 * {@link CloudPoolDriver#setMembershipStatus(String, MembershipStatus)}
	 * stores the status by setting a tag on the server.
	 */
	@Test
	public void setMembershipStatus() {
		FakeOpenstackClient fakeClient = new FakeOpenstackClient(
				servers(memberServer("i-1", Status.ACTIVE)));
		this.poolDriver = new OpenStackPoolDriver(fakeClient);
		this.poolDriver.configure(config(GROUP_NAME, true));
		assertThat(serviceStateTag(fakeClient.getServer("i-1")),
				is(nullValue()));

		MembershipStatus status = MembershipStatus.awaitingService();
		String statusAsJson = JsonUtils.toString(JsonUtils.toJson(status));
		this.poolDriver.setMembershipStatus("i-1", status);
		assertThat(membershipStatusTag(fakeClient.getServer("i-1")),
				is(statusAsJson));

		MembershipStatus otherStatus = MembershipStatus.blessed();
		String otherStatusAsJson = JsonUtils.toString(JsonUtils
				.toJson(otherStatus));
		this.poolDriver.setMembershipStatus("i-1", otherStatus);
		assertThat(membershipStatusTag(fakeClient.getServer("i-1")),
				is(otherStatusAsJson));
	}

	/**
	 * It should not be possible to set membership status on a server that is
	 * not recognized as a group member.
	 */
	@Test(expected = NotFoundException.class)
	public void setMembershipStatusOnNonGroupMember() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		this.poolDriver.setMembershipStatus("i-2", MembershipStatus.blessed());
	}

	/**
	 * A {@link CloudPoolDriverException} should be thrown on failure to tag the
	 * membership status of a group instance.
	 */
	@Test(expected = CloudPoolDriverException.class)
	public void setMembershipStatusOnError() {
		setUpMockedScalingGroup(GROUP_NAME,
				servers(memberServer("i-1", Status.ACTIVE)));

		MembershipStatus status = MembershipStatus.awaitingService();
		String statusAsJson = JsonUtils.toString(JsonUtils.toJson(status));

		doThrow(new RuntimeException("API unreachable")).when(this.mockClient)
				.tagServer("i-1",
						ImmutableMap.of(MEMBERSHIP_STATUS_TAG, statusAsJson));

		this.poolDriver.setMembershipStatus("i-1", status);
	}

	private void setUpMockedScalingGroup(String groupName,
			List<Server> groupMembers) {
		// set up response to queries for group member instances
		when(this.mockClient.getServers(Constants.CLOUD_POOL_TAG, groupName))
				.thenReturn(groupMembers);

		// set up response to queries for group member meta data
		for (Server server : groupMembers) {
			when(this.mockClient.getServer(server.getId())).thenReturn(server);
		}
	}

	/**
	 * Gets the group membership tag ({@link Constants#CLOUD_POOL_TAG}) for a
	 * {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String membershipTag(Server server) {
		return server.getMetadata().get(CLOUD_POOL_TAG);
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

	/**
	 * Gets the service state tag ({@link Constants#MEMBERSHIP_STATUS_TAG}) for
	 * a {@link Server} or <code>null</code> if none is set.
	 *
	 * @param server
	 */
	private static String membershipStatusTag(Server server) {
		return server.getMetadata().get(Constants.MEMBERSHIP_STATUS_TAG);
	}

	private static Map<String, String> groupMemberTags() {
		return ImmutableMap.of(CLOUD_POOL_TAG, GROUP_NAME);
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