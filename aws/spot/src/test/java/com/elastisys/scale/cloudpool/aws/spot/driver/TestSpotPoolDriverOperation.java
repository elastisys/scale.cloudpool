package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.amazonaws.services.ec2.model.InstanceStateName.Pending;
import static com.amazonaws.services.ec2.model.InstanceStateName.Running;
import static com.amazonaws.services.ec2.model.InstanceStateName.Terminated;
import static com.elastisys.scale.cloudpool.api.types.MachineState.PENDING;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REQUESTED;
import static com.elastisys.scale.cloudpool.api.types.MachineState.RUNNING;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingTags.CLOUD_POOL_TAG;
import static com.elastisys.scale.cloudpool.aws.spot.util.SpotTestUtil.instance;
import static com.elastisys.scale.cloudpool.aws.spot.util.SpotTestUtil.spotRequest;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * Exercises the {@link SpotPoolDriver}.
 */
public class TestSpotPoolDriverOperation {

	private final static List<Instance> emptyInstances = Collections
			.emptyList();
	private final static List<SpotInstanceRequest> emptySpotRequests = Collections
			.emptyList();
	private final static List<Machine> emptyMachines = Collections.emptyList();
	private final static List<String> emptyIds = Collections.emptyList();
	private final static List<Filter> emptyFilters = Collections.emptyList();

	/** The name of the spot request pool used in the tests. */
	private static final String POOL_NAME = "pool1";
	/** Tag that is set on spot request pool members. */
	private static final Tag POOL1_TAG = new Tag().withKey(
			ScalingTags.CLOUD_POOL_TAG).withValue("pool1");
	/** Tag that is set on spot request that are members of a different pool. */
	private static final Tag POOL2_TAG = new Tag().withKey(
			ScalingTags.CLOUD_POOL_TAG).withValue("pool2");

	/** Fake stubbed {@link SpotClient}. */
	private FakeSpotClient fakeClient;
	/** Mock {@link SpotClient}. */
	private SpotClient mockClient = mock(SpotClient.class);
	/** Mocked event bus. */
	private EventBus mockEventBus = mock(EventBus.class);
	/** Object under test. */
	private SpotPoolDriver driver;

	@Before
	public void beforeTestMethod() {
		this.fakeClient = new FakeSpotClient();
	}

	/**
	 * @return config to use for the {@link SpotPoolDriver} under test.
	 */
	private BaseCloudPoolConfig config() {
		ScaleInConfig scaleInConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
		ScaleOutConfig scaleOutConfig = new ScaleOutConfig("m1.small",
				"ami-123", "instancekey", asList("webserver"), asList(
						"apt-get update -qy", "apt-get install apache2 -qy"));
		int poolUpdatePeriod = 30;
		SpotPoolDriverConfig driverConfig = new SpotPoolDriverConfig("ABC",
				"XYZ", "us-east-1", 0.0070, 30L, 30L);
		return new BaseCloudPoolConfig(new CloudPoolConfig(POOL_NAME, JsonUtils
				.toJson(driverConfig).getAsJsonObject()), scaleOutConfig,
				scaleInConfig, null, poolUpdatePeriod);
	}

	/**
	 * Verifies proper operation of {@link CloudPoolDriver#listMachines()}.
	 */
	@Test
	public void testListMachines() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// test against empty account with no spot requests
		this.fakeClient.setupFakeAccount(emptySpotRequests, emptyInstances);
		assertThat(this.driver.listMachines(), is(emptyMachines));

		// test against a mix of fulfilled and unfulfilled spot requests
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2", POOL1_TAG),
						spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
				asList(instance("i-2", Running, "sir-2"),
						instance("i-3", Pending, "sir-3")));
		List<Machine> machines = this.driver.listMachines();
		assertRequestIds(machines, asList("sir-1", "sir-2", "sir-3"));
		assertThat(machines.get(0).getId(), is("sir-1"));
		assertThat(machines.get(0).getMachineState(), is(REQUESTED));
		assertThat(machines.get(1).getId(), is("sir-2"));
		assertThat(machines.get(1).getMachineState(), is(RUNNING));
		assertThat(machines.get(2).getId(), is("sir-3"));
		assertThat(machines.get(2).getMachineState(), is(PENDING));

		// only open/active requests are to be returned. cancelled/closed/failed
		// ones are ignored.
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "cancelled", "i-2", POOL1_TAG),
						spotRequest("sir-3", "closed", null, POOL1_TAG),
						spotRequest("sir-4", "failed", null, POOL1_TAG)),
				asList(instance("i-2", Running, "sir-2")));
		machines = this.driver.listMachines();
		assertRequestIds(machines, asList("sir-1"));
	}

	/**
	 * Spot requests with wrong group membership tag are not to be considered
	 * pool members.
	 */
	@Test
	public void testGroupMembershipFilteringInListMachines() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// sir-3 should be ignored, as it is a member of a different pool
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2", POOL1_TAG),
						spotRequest("sir-3", "active", "i-3", POOL2_TAG)),
				asList(instance("i-2", Running, "sir-2"),
						instance("i-3", Pending, "sir-3")));
		List<Machine> machines = this.driver.listMachines();
		assertRequestIds(machines, asList("sir-1", "sir-2"));
	}

	/**
	 * Verify that a {@link CloudPoolDriverException} is thrown on unexpected
	 * errors.
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = CloudPoolDriverException.class)
	public void testListMachinesOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).getSpotInstanceRequests(
				Mockito.anyCollection());

		this.driver.listMachines();
	}

	/**
	 * Verify that spot requests are placed and tagged with group tag on
	 * {@link CloudPoolDriver#startMachines}.
	 */
	@Test
	public void testStartMachines() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		assertThat(this.driver.listMachines().size(), is(0));
		List<Machine> started = this.driver.startMachines(1, config()
				.getScaleOutConfig());
		assertThat(started.size(), is(1));
		SpotInstanceRequest placedSpotRequest = this.fakeClient
				.getSpotInstanceRequest(started.get(0).getId());
		assertTrue(placedSpotRequest.getTags().contains(
				new Tag(CLOUD_POOL_TAG, POOL_NAME)));
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link StartMachinesException}.
	 */
	@Test(expected = StartMachinesException.class)
	public void testStartMachinesOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).placeSpotRequests(Mockito.anyDouble(),
				Mockito.any(ScaleOutConfig.class), Mockito.anyInt(),
				Mockito.anyListOf(Tag.class));

		this.driver.startMachines(1, config().getScaleOutConfig());
	}

	/**
	 * Test termination.
	 */
	@Test
	public void testTerminateUnfulfilledRequest() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// terminating an unfulfilled spot request
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2", POOL1_TAG),
						spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
				asList(instance("i-2", Running, "sir-2"),
						instance("i-3", Terminated, "sir-3")));
		assertRequestIds(this.driver.listMachines(),
				asList("sir-1", "sir-2", "sir-3"));
		assertInstanceIds(this.fakeClient.getInstances(emptyFilters),
				asList("i-2", "i-3"));

		this.driver.terminateMachine("sir-1");
		assertRequestIds(this.driver.listMachines(), asList("sir-2", "sir-3"));
		assertInstanceIds(this.fakeClient.getInstances(emptyFilters),
				asList("i-2", "i-3"));

		// terminating a fulfilled spot request should both cancel the request
		// and terminate the instance.
		this.driver.terminateMachine("sir-2");
		assertRequestIds(this.driver.listMachines(), asList("sir-3"));
		assertInstanceIds(this.fakeClient.getInstances(emptyFilters),
				asList("i-3"));

		// Terminating a fulfilled spot request should work even if the
		// associated instance has already been terminated (e.g., user-initiated
		// shutdown).
		this.driver.terminateMachine("sir-3");
		assertRequestIds(this.driver.listMachines(), emptyIds);
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link CloudPoolDriverException}.
	 */
	@Test(expected = NotFoundException.class)
	public void testTerminateOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).cancelSpotRequests(asList("sir-1"));

		this.driver.terminateMachine("sir-1");
	}

	/**
	 * It should not be possible to terminate an spot request that isn't a group
	 * member.
	 */
	@Test(expected = NotFoundException.class)
	public void testTerminateRequestOnNonGroupMember() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// terminating an unfulfilled spot request
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG)),
				emptyInstances);

		this.driver.terminateMachine("sir-2");
	}

	/**
	 * Test that whenever the bid price changes, unfulfilled requests with an
	 * incorrect bid price are cancelled (to be replaced by requests with the
	 * new bid price on the next pool size update).
	 */
	@Test
	public void testCancelWrongPricedRequests() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());
		double currentBidPrice = this.driver.driverConfig().getBidPrice();

		// unfulfilled, wrong bid price => should be replaced
		SpotInstanceRequest spot1 = spotRequest("sir-1", "open", null,
				POOL1_TAG);
		spot1.setSpotPrice(String.valueOf(currentBidPrice + 0.01));
		// unfulfilled, right bid price => should not be replaced
		SpotInstanceRequest spot2 = spotRequest("sir-2", "open", null,
				POOL1_TAG);
		spot2.setSpotPrice(String.valueOf(currentBidPrice));
		// fulfilled, right bid price => should not be replaced
		SpotInstanceRequest spot3 = spotRequest("sir-3", "active", "i-3",
				POOL1_TAG);
		spot3.setSpotPrice(String.valueOf(currentBidPrice));
		// fulfilled, wrong bid price => should not be replaced
		SpotInstanceRequest spot4 = spotRequest("sir-4", "active", "i-4",
				POOL1_TAG);
		spot4.setSpotPrice(String.valueOf(currentBidPrice + 0.01));

		this.fakeClient.setupFakeAccount(
				asList(spot1, spot2, spot3, spot4),
				asList(instance("i-3", Running, "sir-3"),
						instance("i-4", Running, "sir-4")));

		List<String> cancelledRequests = this.driver
				.cancelWrongPricedRequests();
		assertThat(cancelledRequests.size(), is(1));
		assertThat(cancelledRequests.get(0), is("sir-1"));
		assertRequestIds(this.driver.listMachines(),
				asList("sir-2", "sir-3", "sir-4"));

		// verify event posted on event bus
		verify(this.mockEventBus).post(
				argThat(IsCancelAlert.isCancelAlert("sir-1")));
	}

	/**
	 * Make sure that in the case when all bid prices are correct no
	 * cancellation {@link Alert}s are sent out.
	 */
	@Test
	public void cancellationOfWrongPricedRequestsWhenAllRequestsHaveCorrectPrice() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());
		double currentBidPrice = this.driver.driverConfig().getBidPrice();

		// unfulfilled, right bid price => should not be replaced
		SpotInstanceRequest spot1 = spotRequest("sir-1", "open", null,
				POOL1_TAG);
		spot1.setSpotPrice(String.valueOf(currentBidPrice));

		List<Instance> instances = asList();
		this.fakeClient.setupFakeAccount(asList(spot1), instances);

		List<String> cancelledRequests = this.driver
				.cancelWrongPricedRequests();
		assertThat(cancelledRequests.isEmpty(), is(true));

		// verify event posted on event bus
		verifyZeroInteractions(this.mockEventBus);
	}

	/**
	 * Verify that instances belonging to inactive (cancelled) spot requests are
	 * eventually terminated.
	 */
	@Test
	public void testCleanupOfDanglingInstances() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// i-3 is a dangling instance, since its spot request (sir-3) is
		// cancelled
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2", POOL1_TAG),
						spotRequest("sir-3", "cancelled", "i-3", POOL1_TAG)),
				asList(instance("i-2", Running, "sir-2"),
						instance("i-3", Running, "sir-3")));
		assertInstanceIds(this.fakeClient.getInstances(emptyFilters),
				asList("i-2", "i-3"));

		List<Instance> danglingInstances = this.driver
				.cleanupDanglingInstances();
		assertThat(danglingInstances.size(), is(1));
		assertThat(danglingInstances.get(0).getInstanceId(), is("i-3"));
		assertInstanceIds(this.fakeClient.getInstances(emptyFilters),
				asList("i-2"));

	}

	/**
	 * Verify that the group membership tag is removed from the server when
	 * detaching a group member.
	 */
	@Test
	public void testDetach() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2", POOL1_TAG),
						spotRequest("sir-3", "cancelled", "i-3", POOL1_TAG)),
				asList(instance("i-2", Running, "sir-2"),
						instance("i-3", Running, "sir-3")));
		// pool membership tag should exist
		Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
		assertTrue(this.fakeClient.getSpotInstanceRequest("sir-1").getTags()
				.contains(poolTag));

		this.driver.detachMachine("sir-1");
		// pool membership tag should be gone
		assertFalse(this.fakeClient.getSpotInstanceRequest("sir-1").getTags()
				.contains(poolTag));
	}

	/**
	 * It should not be possible to detach a non-member of the group.
	 */
	@Test(expected = NotFoundException.class)
	public void testDetachOnNonGroupMember() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG)),
				emptyInstances);

		this.driver.detachMachine("sir-2");
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link CloudPoolDriverException}.
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = CloudPoolDriverException.class)
	public void testDetachOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1",
				"open", null, POOL1_TAG));
		when(this.mockClient.getSpotInstanceRequests(Mockito.anyCollection()))
				.thenReturn(poolMembers);
		Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).untagResource("sir-1", asList(poolTag));

		this.driver.detachMachine("sir-1");
	}

	/**
	 * Verifies that a group membership tag gets set on spot requests that are
	 * attached to the group.
	 */
	@Test
	public void testAttach() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// sir-2 doesn't belong to the pool (no POOL1_TAG)
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL1_TAG),
						spotRequest("sir-2", "active", "i-2")),
				asList(instance("i-2", Running, "sir-2")));
		// pool membership tag should not exist
		Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
		assertFalse(this.fakeClient.getSpotInstanceRequest("sir-2").getTags()
				.contains(poolTag));

		this.driver.attachMachine("sir-1");
		// pool membership tag should now be set
		assertTrue(this.fakeClient.getSpotInstanceRequest("sir-1").getTags()
				.contains(poolTag));

	}

	/**
	 * An attempt to attach a spot request that doesn't exist should yield a
	 * {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void testAttachNonExistingSpotRequest() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL2_TAG)),
				emptyInstances);

		this.driver.attachMachine("sir-2");
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link CloudPoolDriverException}.
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = CloudPoolDriverException.class)
	public void testAttachOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1",
				"open", null, POOL1_TAG));
		when(this.mockClient.getSpotInstanceRequests(Mockito.anyCollection()))
				.thenReturn(poolMembers);
		Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).tagResource("sir-1", asList(poolTag));

		this.driver.attachMachine("sir-1");
	}

	/**
	 * Verifies that setting the {@link ServiceState} of a pool member sets a
	 * tag on the spot request.
	 */
	@Test
	public void testSetServiceState() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "active", "i-1", POOL1_TAG)),
				asList(instance("i-1", Running, "sir-1")));

		assertThat(this.driver.listMachines().get(0).getId(), is("sir-1"));
		assertThat(this.driver.listMachines().get(0).getServiceState(),
				is(ServiceState.UNKNOWN));

		this.driver.setServiceState("sir-1", ServiceState.IN_SERVICE);
		assertThat(this.driver.listMachines().get(0).getServiceState(),
				is(ServiceState.IN_SERVICE));
	}

	/**
	 * An attempt to set service state for a spot request that is not a part of
	 * the pool should yield a {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void testSetServiceStateOnNonPoolMember() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// sir-1 doesn't belong to the managed pool (POOL1)
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL2_TAG)),
				emptyInstances);

		this.driver.setServiceState("sir-1", ServiceState.IN_SERVICE);
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link CloudPoolDriverException}.
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = CloudPoolDriverException.class)
	public void testSetServiceStateOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1",
				"open", null, POOL1_TAG));
		when(this.mockClient.getSpotInstanceRequests(Mockito.anyCollection()))
				.thenReturn(poolMembers);
		Tag stateTag = new Tag(ScalingTags.SERVICE_STATE_TAG, "BOOTING");
		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).tagResource("sir-1", asList(stateTag));

		this.driver.setServiceState("sir-1", ServiceState.BOOTING);
	}

	/**
	 * Verifies that setting the {@link MembershipStatus} of a pool member sets
	 * a tag on the spot request.
	 */
	@Test
	public void testSetMembershipStatus() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "active", "i-1", POOL1_TAG)),
				asList(instance("i-1", Running, "sir-1")));

		assertThat(this.driver.listMachines().get(0).getId(), is("sir-1"));
		assertThat(this.driver.listMachines().get(0).getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));

		this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
		assertThat(this.driver.listMachines().get(0).getMembershipStatus(),
				is(MembershipStatus.blessed()));
	}

	/**
	 * An attempt to set membership status for a spot request that is not a part
	 * of the pool should yield a {@link NotFoundException}.
	 */
	@Test(expected = NotFoundException.class)
	public void testSetMembershipStatusOnNonPoolMember() {
		this.driver = new SpotPoolDriver(this.fakeClient, this.mockEventBus);
		this.driver.configure(config());

		// sir-1 doesn't belong to the managed pool (POOL1)
		this.fakeClient.setupFakeAccount(
				asList(spotRequest("sir-1", "open", null, POOL2_TAG)),
				emptyInstances);

		this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
	}

	/**
	 * An error to complete the operation should result in a
	 * {@link CloudPoolDriverException}.
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = CloudPoolDriverException.class)
	public void testSetMembershipStatusOnError() {
		this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
		this.driver.configure(config());

		List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1",
				"open", null, POOL1_TAG));
		when(this.mockClient.getSpotInstanceRequests(Mockito.anyCollection()))
				.thenReturn(poolMembers);
		Tag statusTag = new Tag(ScalingTags.MEMBERSHIP_STATUS_TAG,
				MembershipStatus.blessed().toString());
		doThrow(new AmazonServiceException("something went wrong")).when(
				this.mockClient).tagResource("sir-1", asList(statusTag));

		this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
	}

	private void assertRequestIds(List<Machine> spotRequests,
			List<String> expectedIds) {
		assertThat(spotRequests.size(), is(expectedIds.size()));
		List<String> machineIds = Lists.transform(spotRequests, Machine.toId());
		assertTrue("not all expected machine ids were present",
				machineIds.containsAll(expectedIds));
	}

	private void assertInstanceIds(List<Instance> instances,
			List<String> expectedInstanceIds) {
		assertThat(instances.size(), is(expectedInstanceIds.size()));
		List<String> machineIds = Lists.transform(instances,
				AwsEc2Functions.toInstanceId());
		assertTrue("not all expected instance ids were present",
				machineIds.containsAll(expectedInstanceIds));

	}

}
