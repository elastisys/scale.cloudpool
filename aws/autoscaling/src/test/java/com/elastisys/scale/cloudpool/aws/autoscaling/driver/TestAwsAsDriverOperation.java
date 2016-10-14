package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.elastisys.scale.cloudpool.api.types.ServiceState.IN_SERVICE;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver.REQUESTED_ID_PREFIX;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.config;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.ec2Instance;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.ec2Instances;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.group;
import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.spotInstance;
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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifiers;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsAutoScalingFunctions;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;

/**
 * Verifies the operational behavior of the {@link AwsAsPoolDriver}.
 */
public class TestAwsAsDriverOperation {
    static Logger LOG = LoggerFactory.getLogger(TestAwsAsDriverOperation.class);

    private static final String GROUP_NAME = "MyScalingGroup";

    /** Launch config that orders spot instances. */
    private static final LaunchConfiguration SPOT_LAUNCH_CONFIG = new LaunchConfiguration()
            .withLaunchConfigurationName("SpotTemplate").withInstanceType(InstanceType.M1Medium.toString())
            .withSpotPrice("0.010");

    /** Launch config that orders on-demand instances. */
    private static final LaunchConfiguration ONDEMAND_LAUNCH_CONFIG = new LaunchConfiguration()
            .withLaunchConfigurationName("OnDemandTemplate").withInstanceType(InstanceType.M1Medium.toString());

    /** Object under test. */
    private AwsAsPoolDriver cloudPool;

    private AutoScalingClient mockAwsClient = mock(AutoScalingClient.class);

    @Before
    public void onSetup() throws CloudPoolDriverException {
        this.cloudPool = new AwsAsPoolDriver(this.mockAwsClient);
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));
    }

    /**
     * {@link CloudPoolDriver#listMachines()} should delegate to
     * {@link AutoScalingClient} and basically return anything it returns.
     */
    @Test
    public void listMachines() throws CloudPoolDriverException {
        // empty Auto Scaling Group
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, 0, ec2Instances());
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines()));
        verify(this.mockAwsClient).getAutoScalingGroup(GROUP_NAME);
        verify(this.mockAwsClient).getAutoScalingGroupMembers(GROUP_NAME);

        // non-empty group
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, 0, ec2Instances(ec2Instance("i-1", "running")));
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines("i-1")));

        // group with machines in different states
        List<Instance> members = ec2Instances(ec2Instance("i-1", "running"), ec2Instance("i-2", "pending"),
                ec2Instance("i-3", "terminated"));
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, 0, members);
        List<Machine> machines = this.cloudPool.listMachines();
        assertThat(machines, is(MachinesMatcher.machines("i-1", "i-2", "i-3")));
        // verify that listMachines returns cloud-specific metadata about each
        // machine
        assertTrue(machines.get(0).getMetadata().getAsJsonObject().has("instanceId"));
        assertTrue(machines.get(1).getMetadata().getAsJsonObject().has("instanceId"));
        assertTrue(machines.get(2).getMetadata().getAsJsonObject().has("instanceId"));
    }

    /**
     * If the actual size of the Auto Scaling Group is less than desired
     * capacity (for example, when there is a shortage in supply of instances)
     * the {@link AwsAsPoolDriver} should report the missing instances as being
     * pseudo instances in state {@link MachineState#REQUESTED}, to not fool the
     * {@link BaseCloudPool} from believing that the {@link CloudPoolDriver} is
     * too small and order new machines to be started (and increase the desired
     * capacity even more).
     */
    @Test
    public void listMachinesOnGroupThatHasNotReachedDesiredCapacity() throws CloudPoolDriverException {
        // two missing instances: desired capacity: 3, actual capacity: 1
        int desiredCapacity = 3;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));
        List<Machine> groupMembers = this.cloudPool.listMachines();
        assertThat(groupMembers, is(MachinesMatcher.machines("i-1", "i-requested1", "i-requested2")));
        assertThat(groupMembers.get(0).getMachineState(), is(MachineState.RUNNING));
        assertThat(groupMembers.get(0).getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
        assertThat(groupMembers.get(0).getMachineSize(), is(InstanceType.M1Medium.toString()));

        assertThat(groupMembers.get(1).getMachineState(), is(MachineState.REQUESTED));
        assertThat(groupMembers.get(1).getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
        assertThat(groupMembers.get(1).getMachineSize(), is(InstanceType.M1Medium.toString()));

        assertThat(groupMembers.get(2).getMachineState(), is(MachineState.REQUESTED));
        assertThat(groupMembers.get(2).getCloudProvider(), is(PoolIdentifiers.AWS_EC2));
        assertThat(groupMembers.get(2).getMachineSize(), is(InstanceType.M1Medium.toString()));
    }

    /**
     * Make sure that requested but not yet acquired pseudo instances get
     * correctly reported for an Auto Scaling Group of spot instances as well.
     */
    @Test
    public void listPsuedoMachinesOnGroupWithSpotInstances() {
        // two missing instances: desired capacity: 3, actual capacity: 1
        int desiredCapacity = 3;
        setUpMockedAutoScalingGroup(GROUP_NAME, SPOT_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(spotInstance("sir-1", "i-1", "running")));
        List<Machine> groupMembers = this.cloudPool.listMachines();
        assertThat(groupMembers, is(MachinesMatcher.machines("i-1", "i-requested1", "i-requested2")));
        assertThat(groupMembers.get(0).getMachineState(), is(MachineState.RUNNING));
        assertThat(groupMembers.get(0).getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
        assertThat(groupMembers.get(0).getMachineSize(), is(InstanceType.M1Medium.toString()));

        assertThat(groupMembers.get(1).getMachineState(), is(MachineState.REQUESTED));
        assertThat(groupMembers.get(1).getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
        assertThat(groupMembers.get(1).getMachineSize(), is(InstanceType.M1Medium.toString()));

        assertThat(groupMembers.get(2).getMachineState(), is(MachineState.REQUESTED));
        assertThat(groupMembers.get(2).getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
        assertThat(groupMembers.get(2).getMachineSize(), is(InstanceType.M1Medium.toString()));
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown if listing Auto
     * Scaling Group members fails.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void listMachinesOnError() throws CloudPoolDriverException {
        // set up Amazon API call to fail
        when(this.mockAwsClient.getAutoScalingGroup(GROUP_NAME))
                .thenThrow(new AmazonServiceException("API unreachable"));
        this.cloudPool.listMachines();
    }

    /**
     * Exercises the scenario when a call to describe the autoscaling group
     * yields a different outcome from a call to list the group members, which
     * may happen since there is always a time-window between those calls where
     * things may change. We need to verify that this doesn't confuse the
     * AwsAsDriver when it creates pseudo-instances in REQUESTED state.
     * <p/>
     * In this particular case, the scenario is that a call is made to describe
     * the autoscaling group (desiredCapacity: 1, instances: 0), then the
     * requested instance comes online and the call to get the group members
     * returns 1 RUNNING instance. In this case, the pool driver should act as
     * if the first call to describe the group returned (desiredCapacity: 1,
     * instances: 1) and not return any pseudo-machines in state REQUESTED.
     */
    @Test
    public void listMachinesOnInconsistentApiInformation() {
        int desiredCapacity = 1;
        List<Instance> emptyGroup = ec2Instances();

        // empty group returned on call to describe autoscaling group
        AutoScalingGroup groupAtT1 = group(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, emptyGroup);
        when(this.mockAwsClient.getLaunchConfiguration(ONDEMAND_LAUNCH_CONFIG.getLaunchConfigurationName()))
                .thenReturn(ONDEMAND_LAUNCH_CONFIG);
        when(this.mockAwsClient.getAutoScalingGroup(GROUP_NAME)).thenReturn(groupAtT1);

        // one running instance returned on later call to get group members
        List<Instance> groupMembersAtT2 = ec2Instances(ec2Instance("i-1", "running"));
        when(this.mockAwsClient.getAutoScalingGroupMembers(GROUP_NAME)).thenReturn(groupMembersAtT2);

        List<Machine> machines = this.cloudPool.listMachines();
        assertThat(machines.size(), is(1));
        assertThat(machines.get(0).getId(), is("i-1"));
        assertThat(machines.get(0).getMachineState(), is(MachineState.RUNNING));

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
        this.cloudPool = new AwsAsPoolDriver(
                new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, ec2Instances()));
        this.cloudPool.configure(config);
        List<Machine> startedMachines = this.cloudPool.startMachines(1, scaleUpConfig);
        assertThat(startedMachines, is(MachinesMatcher.machines("i-requested1")));

        // scale up from 1 -> 2
        desiredCapacity = 1;
        this.cloudPool = new AwsAsPoolDriver(new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG,
                desiredCapacity, ec2Instances(ec2Instance("i-1", "running"))));
        this.cloudPool.configure(config);
        startedMachines = this.cloudPool.startMachines(1, scaleUpConfig);
        assertThat(startedMachines, is(MachinesMatcher.machines("i-requested1")));

        // scale up from 2 -> 4
        desiredCapacity = 2;
        this.cloudPool = new AwsAsPoolDriver(new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG,
                desiredCapacity, ec2Instances(ec2Instance("i-1", "running"), ec2Instance("i-2", "pending"))));
        this.cloudPool.configure(config);
        startedMachines = this.cloudPool.startMachines(2, scaleUpConfig);
        assertThat(startedMachines, is(MachinesMatcher.machines("i-requested1", "i-requested2")));
    }

    /**
     * A {@link CloudPoolDriverException} should be raised on failure to start
     * more instances.
     */
    @Test
    public void startMachinesOnFailure() throws StartMachinesException {
        // set up mock to throw an error whenever setDesiredSize is called
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));
        doThrow(new AmazonClientException("API unreachable")).when(this.mockAwsClient).setDesiredSize(GROUP_NAME, 2);

        ScaleOutConfig scaleUpConfig = config(GROUP_NAME).getScaleOutConfig();
        // should raise an exception
        try {
            this.cloudPool.startMachines(1, scaleUpConfig);
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
        this.cloudPool = new AwsAsPoolDriver(new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG,
                desiredCapacity, ec2Instances(ec2Instance("i-1", "running"), ec2Instance("i-2", "pending"))));
        this.cloudPool.configure(config);
        this.cloudPool.terminateMachine("i-1");
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines("i-2")));

        this.cloudPool.terminateMachine("i-2");
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines()));
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
        FakeAutoScalingClient client = new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running"), ec2Instance("i-2", "pending")));
        this.cloudPool = new AwsAsPoolDriver(client);
        this.cloudPool.configure(config);
        assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(), is(2));

        // request scaling group to grow by one
        this.cloudPool.startMachines(1, config.getScaleOutConfig());
        final String requestedMachineId = REQUESTED_ID_PREFIX + "1";
        // we should now have incremented the desired capacity of the Auto
        // Scaling Group and have one requested machine that is faked by the
        // group
        assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(), is(3));
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines("i-1", "i-2", requestedMachineId)));

        // now ask scaling group to terminate the requested instance (this
        // should only result in the desired capacity being decremented, not
        // attempting to actually terminate the fake instance)
        this.cloudPool.terminateMachine(requestedMachineId);
        assertThat(client.getAutoScalingGroup(GROUP_NAME).getDesiredCapacity(), is(2));
        assertThat(this.cloudPool.listMachines(), is(MachinesMatcher.machines("i-1", "i-2")));
    }

    /**
     * On a client error, a {@link CloudPoolDriverException} should be raised.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void terminateOnError() throws Exception {
        // set up mock to throw an error whenever terminateInstance is called
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));
        doThrow(new AmazonClientException("API unreachable")).when(this.mockAwsClient).terminateInstance(GROUP_NAME,
                "i-1");

        this.cloudPool.terminateMachine("i-1");
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
        this.cloudPool = new AwsAsPoolDriver(
                new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, members, nonMembers));
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));

        this.cloudPool.terminateMachine("i-2");
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
        this.cloudPool = new AwsAsPoolDriver(
                new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, members, nonMembers));
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));

        this.cloudPool.detachMachine("i-1");
        List<String> emptyList = asList();
        assertThat(groupIds(this.cloudPool), is(emptyList));
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
        this.cloudPool = new AwsAsPoolDriver(
                new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, members, nonMembers));
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));

        this.cloudPool.detachMachine("i-2");
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to detach
     * an instance from the group.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void detachOnError() throws Exception {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        doThrow(new RuntimeException("API unreachable")).when(this.mockAwsClient).detachInstance(GROUP_NAME, "i-1");

        this.cloudPool.detachMachine("i-1");
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
        this.cloudPool = new AwsAsPoolDriver(
                new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, members, nonMembers));
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));

        assertThat(groupIds(this.cloudPool), is(Arrays.asList("i-1")));
        this.cloudPool.attachMachine("i-2");
        assertThat(groupIds(this.cloudPool), is(Arrays.asList("i-1", "i-2")));
    }

    /**
     * An attempt to attach a non-existing machine should result in
     * {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void attachNonExistingMachine() {
        int desiredCapacity = 0;
        this.cloudPool = new AwsAsPoolDriver(new FakeAutoScalingClient(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG,
                desiredCapacity, ec2Instances(), ec2Instances()));
        this.cloudPool.configure(TestUtils.config(GROUP_NAME));

        this.cloudPool.attachMachine("i-3");
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
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity, members, nonMembers);

        doThrow(new RuntimeException("API unreachable")).when(this.mockAwsClient).attachInstance(GROUP_NAME, "i-2");

        this.cloudPool.attachMachine("i-2");
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setServiceState(String, ServiceState)} stores the
     * state by setting a tag on the instance.
     */
    @Test
    public void setServiceState() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        List<Tag> stateTag = asList(
                new Tag().withKey(ScalingTags.SERVICE_STATE_TAG).withValue(ServiceState.IN_SERVICE.name()));
        this.cloudPool.setServiceState("i-1", ServiceState.IN_SERVICE);

        verify(this.mockAwsClient).tagInstance("i-1", stateTag);
    }

    /**
     * It should not be possible to set service state on a machine instance that
     * is not recognized as a group member.
     */
    @Test(expected = NotFoundException.class)
    public void setServiceStateOnNonGroupMember() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        this.cloudPool.setServiceState("i-2", ServiceState.IN_SERVICE);
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * service state of a group instance.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setServiceStateOnError() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        List<Tag> serviceStateTag = asList(new Tag().withKey(SERVICE_STATE_TAG).withValue(IN_SERVICE.name()));
        doThrow(new RuntimeException("API unreachable")).when(this.mockAwsClient).tagInstance("i-1", serviceStateTag);

        this.cloudPool.setServiceState("i-1", IN_SERVICE);
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setMembershipStatus(String, MembershipStatus)}
     * stores the status by setting a tag on the instance.
     */
    @Test
    public void setMembershipStatus() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        MembershipStatus status = MembershipStatus.awaitingService();
        String tagValue = JsonUtils.toString(JsonUtils.toJson(status));

        List<Tag> statusTag = asList(new Tag().withKey(ScalingTags.MEMBERSHIP_STATUS_TAG).withValue(tagValue));
        this.cloudPool.setMembershipStatus("i-1", status);

        verify(this.mockAwsClient).tagInstance("i-1", statusTag);
    }

    /**
     * It should not be possible to set membership status on a machine instance
     * that is not recognized as a group member.
     */
    @Test(expected = NotFoundException.class)
    public void setMembershipStatusOnNonGroupMember() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        this.cloudPool.setMembershipStatus("i-2", MembershipStatus.awaitingService());
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * membership status of a pool member.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setMembershipStatusOnError() {
        int desiredCapacity = 1;
        setUpMockedAutoScalingGroup(GROUP_NAME, ONDEMAND_LAUNCH_CONFIG, desiredCapacity,
                ec2Instances(ec2Instance("i-1", "running")));

        MembershipStatus status = MembershipStatus.awaitingService();
        String tagValue = JsonUtils.toString(JsonUtils.toJson(status));
        List<Tag> statusTag = asList(new Tag().withKey(ScalingTags.MEMBERSHIP_STATUS_TAG).withValue(tagValue));
        doThrow(new RuntimeException("API unreachable")).when(this.mockAwsClient).tagInstance("i-1", statusTag);

        this.cloudPool.setMembershipStatus("i-1", status);

        this.cloudPool.setServiceState("i-1", IN_SERVICE);
    }

    /**
     * Sets up a fake {@link AutoScalingClient} with a fake pool containing only
     * instances that are actual members of the Auto Scaling Group.
     *
     * @param autoScalingGroupName
     * @param launchConfig
     * @param desiredCapacity
     * @param memberInstances
     */
    private void setUpMockedAutoScalingGroup(String autoScalingGroupName, LaunchConfiguration launchConfig,
            int desiredCapacity, List<Instance> memberInstances) {
        setUpMockedAutoScalingGroup(autoScalingGroupName, launchConfig, desiredCapacity, memberInstances,
                new ArrayList<Instance>());
    }

    /**
     * Sets up a fake {@link AutoScalingClient} with a fake pool containing both
     * instances that are members of the Auto Scaling Group and instances that
     * are not members.
     *
     * @param autoScalingGroupName
     * @param launchConfig
     * @param desiredCapacity
     * @param memberInstances
     *            Auto Scaling Group members.
     * @param nonMemberInstances
     *            EC2 instances that exist in the (fake) cloud but aren't
     *            members of the Auto Scaling Group.
     */
    private void setUpMockedAutoScalingGroup(String autoScalingGroupName, LaunchConfiguration launchConfig,
            int desiredCapacity, List<Instance> memberInstances, List<Instance> nonMemberInstances) {
        AutoScalingGroup autoScalingGroup = group(autoScalingGroupName, launchConfig, desiredCapacity, memberInstances);
        LOG.debug("setting up mocked group: {}",
                Lists.transform(autoScalingGroup.getInstances(), AwsAutoScalingFunctions.toAutoScalingInstanceId()));

        when(this.mockAwsClient.getAutoScalingGroup(autoScalingGroupName)).thenReturn(autoScalingGroup);
        when(this.mockAwsClient.getLaunchConfiguration(launchConfig.getLaunchConfigurationName()))
                .thenReturn(launchConfig);
        when(this.mockAwsClient.getAutoScalingGroupMembers(autoScalingGroupName)).thenReturn(memberInstances);
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
