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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

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
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Exercises the {@link SpotPoolDriver}.
 */
public class TestSpotPoolDriverOperation {

    private final static List<Instance> emptyInstances = Collections.emptyList();
    private final static List<SpotInstanceRequest> emptySpotRequests = Collections.emptyList();
    private final static List<Machine> emptyMachines = Collections.emptyList();
    private final static List<String> emptyIds = Collections.emptyList();
    private final static List<Filter> emptyFilters = Collections.emptyList();

    /** The name of the spot request pool used in the tests. */
    private static final String POOL_NAME = "pool1";
    /** Tag that is set on spot request pool members. */
    private static final Tag POOL1_TAG = new Tag().withKey(ScalingTags.CLOUD_POOL_TAG).withValue("pool1");
    /** Tag that is set on spot request that are members of a different pool. */
    private static final Tag POOL2_TAG = new Tag().withKey(ScalingTags.CLOUD_POOL_TAG).withValue("pool2");

    /** Sample AWS access key id. */
    private static final String ACCESS_KEY_ID = "awsAccessKeyId";
    /** Sample AWS secret access key. */
    private static final String SECRET_ACCESS_KEY = "awsSecretAccessKey";
    /** Sample region */
    private static final String REGION = "us-east-1";
    private static final TimeInterval DANGLING_INSTANCE_CLEANUP_PERIOD = new TimeInterval(30L, TimeUnit.SECONDS);
    private static final TimeInterval BID_REPLACEMENT_PERIOD = new TimeInterval(30L, TimeUnit.SECONDS);
    private static final double BID_PRICE = 0.0070;

    /** Sample instance type. */
    private static final String INSTANCE_TYPE = "m1.small";
    /** Sample image. */
    private static final String AMI = "ami-12345678";
    private static final List<String> SUBNET_IDS = asList("subnet-44b5786b", "subnet-dcd15f97");
    private static final boolean ASSIGN_PUBLIC_IP = true;
    /** Sample keypair. */
    private static final String KEYPAIR = "ssh-loginkey";
    private static final String IAM_INSTANCE_PROFILE = "arn:aws:iam::123456789012:instance-profile/my-iam-profile";
    /** Sample security groups. */
    private static final List<String> SECURITY_GROUP_IDS = Arrays.asList("sg-12345678");
    /** Sample base64-encoded user data. */
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));
    private static final boolean EBS_OPTIMIZED = true;
    private static final Map<String, String> TAGS = Maps.of("Cluster", "mycluster");

    /** Fake stubbed {@link SpotClient}. */
    private FakeSpotClient fakeClient;
    /** Mock {@link SpotClient}. */
    private SpotClient mockClient = mock(SpotClient.class);
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    /** Mocked event bus. */
    private EventBus mockEventBus = mock(EventBus.class);
    /** Object under test. */
    private SpotPoolDriver driver;

    @Before
    public void beforeTestMethod() {
        this.fakeClient = new FakeSpotClient();
    }

    private DriverConfig config() {
        CloudApiSettings cloudApiSettings = new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, BID_PRICE,
                BID_REPLACEMENT_PERIOD, DANGLING_INSTANCE_CLEANUP_PERIOD);
        Ec2ProvisioningTemplate provisioningTemplate = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, SUBNET_IDS,
                ASSIGN_PUBLIC_IP, KEYPAIR, IAM_INSTANCE_PROFILE, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS);
        return new DriverConfig(POOL_NAME, JsonUtils.toJson(cloudApiSettings).getAsJsonObject(),
                JsonUtils.toJson(provisioningTemplate).getAsJsonObject());
    }

    /**
     * Verifies proper operation of {@link CloudPoolDriver#listMachines()}.
     */
    @Test
    public void testListMachines() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // test against empty account with no spot requests
        this.fakeClient.setupFakeAccount(emptySpotRequests, emptyInstances);
        assertThat(this.driver.listMachines(), is(emptyMachines));

        // test against a mix of fulfilled and unfulfilled spot requests
        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Pending, "sir-3")));
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
        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL1_TAG),
                spotRequest("sir-2", "cancelled", "i-2", POOL1_TAG), spotRequest("sir-3", "closed", null, POOL1_TAG),
                spotRequest("sir-4", "failed", null, POOL1_TAG)), asList(instance("i-2", Running, "sir-2")));
        machines = this.driver.listMachines();
        assertRequestIds(machines, asList("sir-1"));
    }

    /**
     * Spot requests with wrong group membership tag are not to be considered
     * pool members.
     */
    @Test
    public void testGroupMembershipFilteringInListMachines() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // sir-3 should be ignored, as it is a member of a different pool
        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "active", "i-3", POOL2_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Pending, "sir-3")));
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
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient)
                .getSpotInstanceRequests(Matchers.anyCollection());

        this.driver.listMachines();
    }

    /**
     * Verify that spot requests are placed and tagged with group tag on
     * {@link CloudPoolDriver#startMachines}.
     */
    @Test
    public void testStartMachines() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        assertThat(this.driver.listMachines().size(), is(0));
        List<Machine> started = this.driver.startMachines(1);
        assertThat(started.size(), is(1));
        SpotInstanceRequest placedSpotRequest = this.fakeClient.getSpotInstanceRequest(started.get(0).getId());
        assertTrue(placedSpotRequest.getTags().contains(new Tag(CLOUD_POOL_TAG, POOL_NAME)));
    }

    /**
     * An error to complete the operation should result in a
     * {@link StartMachinesException}.
     */
    @Test(expected = StartMachinesException.class)
    public void testStartMachinesOnError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient).placeSpotRequests(
                Matchers.anyDouble(), Matchers.any(Ec2ProvisioningTemplate.class), Matchers.anyInt());

        this.driver.startMachines(1);
    }

    /**
     * Terminating a single unfulfilled spot requests should only cancel the
     * spot request (there is no associated instance to terminate).
     */
    @Test
    public void terminateSingleUnfulfilledRequest() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Terminated, "sir-3")));
        assertRequestIds(this.driver.listMachines(), asList("sir-1", "sir-2", "sir-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2", "i-3"));

        this.driver.terminateMachines(asList("sir-1"));
        assertRequestIds(this.driver.listMachines(), asList("sir-2", "sir-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2", "i-3"));
    }

    /**
     * Terminating a single fulfilled spot requests should both cancel the spot
     * request and also terminate its associated instance.
     */
    @Test
    public void terminateSingleFulfilledRequest() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Terminated, "sir-3")));
        assertRequestIds(this.driver.listMachines(), asList("sir-1", "sir-2", "sir-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2", "i-3"));

        // terminating a fulfilled spot request should both cancel the request
        // and terminate the instance.
        this.driver.terminateMachines(asList("sir-2"));
        assertRequestIds(this.driver.listMachines(), asList("sir-1", "sir-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-3"));

        // terminating a fulfilled spot request should work even if the
        // associated instance has already been terminated (e.g., user-initiated
        // shutdown).
        this.driver.terminateMachines(asList("sir-1", "sir-3"));
        assertRequestIds(this.driver.listMachines(), emptyIds);
    }

    /**
     * It should be possible to terminate several spot requests/instances in a
     * single terminate call.
     */
    @Test
    public void terminateMultipleSpotRequests() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "active", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Terminated, "sir-3")));
        assertRequestIds(this.driver.listMachines(), asList("sir-1", "sir-2", "sir-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2", "i-3"));

        // terminating a fulfilled spot request should both cancel the request
        // and terminate the instance.
        this.driver.terminateMachines(asList("sir-1", "sir-2", "sir-3"));
        assertRequestIds(this.driver.listMachines(), emptyIds);
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), emptyIds);

    }

    /**
     * On client error, a {@link CloudPoolDriverException} should be raised.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void terminateOnClientError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        doThrow(new AmazonServiceException("api error")).when(this.mockClient).cancelSpotRequests(asList("sir-1"));

        this.driver.terminateMachines(asList("sir-1"));
    }

    /**
     * Trying to terminate a spot request that is not recognized as a pool
     * member should result in a {@link TerminateMachinesException}.
     */
    @Test
    public void terminateOnNonGroupMember() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // terminating an unfulfilled spot request
        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL1_TAG)), emptyInstances);

        try {
            this.driver.terminateMachines(asList("sir-2"));
            fail("should fail to terminate spot request that is not a pool member");
        } catch (TerminateMachinesException e) {
            // expected
            assertTrue(e.getTerminationErrors().keySet().contains("sir-2"));
            assertThat(e.getTerminationErrors().get("sir-2"), instanceOf(NotFoundException.class));
            assertThat(e.getTerminatedMachines(), is(Collections.emptyList()));
        }
    }

    /**
     * When some terminations were successful and some failed, a
     * {@link TerminateMachinesException} should be thrown which indicates which
     * instances were terminated and which instance terminations failed.
     */
    @Test
    public void terminateOnPartialFailure() {

        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2")));
        this.driver.configure(config());

        // sir-3 is not a pool member and should fail
        try {
            this.driver.terminateMachines(asList("sir-3", "sir-2"));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            // terminating sir-2 should succeed
            assertThat(e.getTerminatedMachines(), is(asList("sir-2")));
            // terminating sir-3 should fail
            assertTrue(e.getTerminationErrors().keySet().contains("sir-3"));
            assertThat(e.getTerminationErrors().get("sir-3"), instanceOf(NotFoundException.class));
        }
    }

    /**
     * Test that whenever the bid price changes, unfulfilled requests with an
     * incorrect bid price are cancelled (to be replaced by requests with the
     * new bid price on the next pool size update).
     */
    @Test
    public void testCancelWrongPricedRequests() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());
        double currentBidPrice = this.driver.cloudApiSettings().getBidPrice();

        // unfulfilled, wrong bid price => should be replaced
        SpotInstanceRequest spot1 = spotRequest("sir-1", "open", null, POOL1_TAG);
        spot1.setSpotPrice(String.valueOf(currentBidPrice + 0.01));
        // unfulfilled, right bid price => should not be replaced
        SpotInstanceRequest spot2 = spotRequest("sir-2", "open", null, POOL1_TAG);
        spot2.setSpotPrice(String.valueOf(currentBidPrice));
        // fulfilled, right bid price => should not be replaced
        SpotInstanceRequest spot3 = spotRequest("sir-3", "active", "i-3", POOL1_TAG);
        spot3.setSpotPrice(String.valueOf(currentBidPrice));
        // fulfilled, wrong bid price => should not be replaced
        SpotInstanceRequest spot4 = spotRequest("sir-4", "active", "i-4", POOL1_TAG);
        spot4.setSpotPrice(String.valueOf(currentBidPrice + 0.01));

        this.fakeClient.setupFakeAccount(asList(spot1, spot2, spot3, spot4),
                asList(instance("i-3", Running, "sir-3"), instance("i-4", Running, "sir-4")));

        List<String> cancelledRequests = this.driver.cancelWrongPricedRequests();
        assertThat(cancelledRequests.size(), is(1));
        assertThat(cancelledRequests.get(0), is("sir-1"));
        assertRequestIds(this.driver.listMachines(), asList("sir-2", "sir-3", "sir-4"));

        // verify event posted on event bus
        verify(this.mockEventBus).post(argThat(IsCancelAlert.isCancelAlert("sir-1")));
    }

    /**
     * Make sure that in the case when all bid prices are correct no
     * cancellation {@link Alert}s are sent out.
     */
    @Test
    public void cancellationOfWrongPricedRequestsWhenAllRequestsHaveCorrectPrice() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());
        double currentBidPrice = this.driver.cloudApiSettings().getBidPrice();

        // unfulfilled, right bid price => should not be replaced
        SpotInstanceRequest spot1 = spotRequest("sir-1", "open", null, POOL1_TAG);
        spot1.setSpotPrice(String.valueOf(currentBidPrice));

        List<Instance> instances = asList();
        this.fakeClient.setupFakeAccount(asList(spot1), instances);

        List<String> cancelledRequests = this.driver.cancelWrongPricedRequests();
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
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // i-3 is a dangling instance, since its spot request (sir-3) is
        // cancelled
        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "cancelled", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Running, "sir-3")));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2", "i-3"));

        List<Instance> danglingInstances = this.driver.cleanupDanglingInstances();
        assertThat(danglingInstances.size(), is(1));
        assertThat(danglingInstances.get(0).getInstanceId(), is("i-3"));
        assertInstanceIds(this.fakeClient.getInstances(emptyFilters), asList("i-2"));

    }

    /**
     * Verify that the group membership tag is removed from the server when
     * detaching a group member.
     */
    @Test
    public void testDetach() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2", POOL1_TAG),
                        spotRequest("sir-3", "cancelled", "i-3", POOL1_TAG)),
                asList(instance("i-2", Running, "sir-2"), instance("i-3", Running, "sir-3")));
        // pool membership tag should exist
        Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
        assertTrue(this.fakeClient.getSpotInstanceRequest("sir-1").getTags().contains(poolTag));

        this.driver.detachMachine("sir-1");
        // pool membership tag should be gone
        assertFalse(this.fakeClient.getSpotInstanceRequest("sir-1").getTags().contains(poolTag));
    }

    /**
     * It should not be possible to detach a non-member of the group.
     */
    @Test(expected = NotFoundException.class)
    public void testDetachOnNonGroupMember() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL1_TAG)), emptyInstances);

        this.driver.detachMachine("sir-2");
    }

    /**
     * An error to complete the operation should result in a
     * {@link CloudPoolDriverException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CloudPoolDriverException.class)
    public void testDetachOnError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1", "open", null, POOL1_TAG));
        when(this.mockClient.getSpotInstanceRequests(Matchers.anyCollection())).thenReturn(poolMembers);
        Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient).untagResource("sir-1",
                asList(poolTag));

        this.driver.detachMachine("sir-1");
    }

    /**
     * Verifies that a group membership tag gets set on spot requests that are
     * attached to the group.
     */
    @Test
    public void testAttach() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // sir-2 doesn't belong to the pool (no POOL1_TAG)
        this.fakeClient.setupFakeAccount(
                asList(spotRequest("sir-1", "open", null, POOL1_TAG), spotRequest("sir-2", "active", "i-2")),
                asList(instance("i-2", Running, "sir-2")));
        // pool membership tag should not exist
        Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
        assertFalse(this.fakeClient.getSpotInstanceRequest("sir-2").getTags().contains(poolTag));

        this.driver.attachMachine("sir-1");
        // pool membership tag should now be set
        assertTrue(this.fakeClient.getSpotInstanceRequest("sir-1").getTags().contains(poolTag));

    }

    /**
     * An attempt to attach a spot request that doesn't exist should yield a
     * {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void testAttachNonExistingSpotRequest() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL2_TAG)), emptyInstances);

        this.driver.attachMachine("sir-2");
    }

    /**
     * An error to complete the operation should result in a
     * {@link CloudPoolDriverException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CloudPoolDriverException.class)
    public void testAttachOnError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1", "open", null, POOL1_TAG));
        when(this.mockClient.getSpotInstanceRequests(Matchers.anyCollection())).thenReturn(poolMembers);
        Tag poolTag = new Tag(CLOUD_POOL_TAG, POOL_NAME);
        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient).tagResource("sir-1",
                asList(poolTag));

        this.driver.attachMachine("sir-1");
    }

    /**
     * Verifies that setting the {@link ServiceState} of a pool member sets a
     * tag on the spot request.
     */
    @Test
    public void testSetServiceState() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "active", "i-1", POOL1_TAG)),
                asList(instance("i-1", Running, "sir-1")));

        assertThat(this.driver.listMachines().get(0).getId(), is("sir-1"));
        assertThat(this.driver.listMachines().get(0).getServiceState(), is(ServiceState.UNKNOWN));

        this.driver.setServiceState("sir-1", ServiceState.IN_SERVICE);
        assertThat(this.driver.listMachines().get(0).getServiceState(), is(ServiceState.IN_SERVICE));
    }

    /**
     * An attempt to set service state for a spot request that is not a part of
     * the pool should yield a {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void testSetServiceStateOnNonPoolMember() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // sir-1 doesn't belong to the managed pool (POOL1)
        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL2_TAG)), emptyInstances);

        this.driver.setServiceState("sir-1", ServiceState.IN_SERVICE);
    }

    /**
     * An error to complete the operation should result in a
     * {@link CloudPoolDriverException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CloudPoolDriverException.class)
    public void testSetServiceStateOnError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1", "open", null, POOL1_TAG));
        when(this.mockClient.getSpotInstanceRequests(Matchers.anyCollection())).thenReturn(poolMembers);
        Tag stateTag = new Tag(ScalingTags.SERVICE_STATE_TAG, "BOOTING");
        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient).tagResource("sir-1",
                asList(stateTag));

        this.driver.setServiceState("sir-1", ServiceState.BOOTING);
    }

    /**
     * Verifies that setting the {@link MembershipStatus} of a pool member sets
     * a tag on the spot request.
     */
    @Test
    public void testSetMembershipStatus() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "active", "i-1", POOL1_TAG)),
                asList(instance("i-1", Running, "sir-1")));

        assertThat(this.driver.listMachines().get(0).getId(), is("sir-1"));
        assertThat(this.driver.listMachines().get(0).getMembershipStatus(), is(MembershipStatus.defaultStatus()));

        this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
        assertThat(this.driver.listMachines().get(0).getMembershipStatus(), is(MembershipStatus.blessed()));
    }

    /**
     * An attempt to set membership status for a spot request that is not a part
     * of the pool should yield a {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void testSetMembershipStatusOnNonPoolMember() {
        this.driver = new SpotPoolDriver(this.fakeClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        // sir-1 doesn't belong to the managed pool (POOL1)
        this.fakeClient.setupFakeAccount(asList(spotRequest("sir-1", "open", null, POOL2_TAG)), emptyInstances);

        this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
    }

    /**
     * An error to complete the operation should result in a
     * {@link CloudPoolDriverException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CloudPoolDriverException.class)
    public void testSetMembershipStatusOnError() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
        this.driver.configure(config());

        List<SpotInstanceRequest> poolMembers = asList(spotRequest("sir-1", "open", null, POOL1_TAG));
        when(this.mockClient.getSpotInstanceRequests(Matchers.anyCollection())).thenReturn(poolMembers);
        Tag statusTag = new Tag(ScalingTags.MEMBERSHIP_STATUS_TAG, MembershipStatus.blessed().toString());
        doThrow(new AmazonServiceException("something went wrong")).when(this.mockClient).tagResource("sir-1",
                asList(statusTag));

        this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
    }

    private void assertRequestIds(List<Machine> spotRequests, List<String> expectedIds) {
        assertThat(spotRequests.size(), is(expectedIds.size()));
        List<String> machineIds = spotRequests.stream().map(Machine::getId).collect(Collectors.toList());
        assertTrue("not all expected machine ids were present", machineIds.containsAll(expectedIds));
    }

    private void assertInstanceIds(List<Instance> instances, List<String> expectedInstanceIds) {
        assertThat(instances.size(), is(expectedInstanceIds.size()));
        List<String> machineIds = instances.stream().map(Instance::getInstanceId).collect(Collectors.toList());
        assertTrue("not all expected instance ids were present", machineIds.containsAll(expectedInstanceIds));
    }
}
