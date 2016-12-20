package com.elastisys.scale.cloudpool.gce.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.gce.driver.client.GceClient;
import com.elastisys.scale.cloudpool.gce.driver.client.InstanceGroupClient;
import com.elastisys.scale.cloudpool.gce.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.gce.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.gce.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.gce.util.MetadataUtil;
import com.elastisys.scale.cloudpool.gce.util.UrlUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;

/**
 * Exercises the {@link GcePoolDriver} against a mocked single-zone instance
 * group.
 */
public class TestSingleZoneGcePoolDriverOperation {
    private static final Logger LOG = LoggerFactory.getLogger(TestSingleZoneGcePoolDriverOperation.class);

    /** Sample project name. */
    private static final String PROJECT = "my-project";
    /** Sample zone name. */
    private static final String ZONE = "eu-west1-b";
    /** Sample instance group name. */
    private static final String INSTANCE_GROUP = "webserver-instance-group";

    /** Sample cloud pool name. */
    private static final String POOL_NAME = "webserver-pool";

    private static final DriverConfig POOL_DRIVER_CONFIG = driverConfig(POOL_NAME,
            new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, null, ZONE));

    /** Mocked GCE client used by the GCE pool driver under test. */
    private GceClient gceClientMock = mock(GceClient.class);
    /**
     * Mocked (single-zone) instance group client client used by the GCE pool
     * driver under test.
     */
    private InstanceGroupClient instanceGroupClientMock = mock(InstanceGroupClient.class);
    /** Object under test. */
    private GcePoolDriver driver;

    @Before
    public void beforeTestMethod() {
        this.driver = new GcePoolDriver(this.gceClientMock);
        this.driver.configure(POOL_DRIVER_CONFIG);
        reset(this.gceClientMock);
        reset(this.instanceGroupClientMock);
    }

    /**
     * A call to list machines should invoke the GCE client to retrieve metadata
     * for each instance in the group and convert them to {@link Machine}s.
     */
    @Test
    public void listMachines() {
        int targetSize = 2;
        int runningInstances = 2;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        List<Machine> machines = this.driver.listMachines();

        // verify that instances were correctly translated to machines
        assertThat(machines.size(), is(2));
        assertThat(machines.get(0), is(new InstanceToMachine().apply(simulatedGroup.instances().get(0))));
        assertThat(machines.get(1), is(new InstanceToMachine().apply(simulatedGroup.instances().get(1))));

        //
        // verify expected calls to backend API clients
        //

        // should call to get a single-zone instance group client
        verify(this.gceClientMock).singleZoneInstanceGroup(PROJECT, ZONE, INSTANCE_GROUP);
        // should retrieve instance group metadata (targetSize)
        verify(this.instanceGroupClientMock).getInstanceGroup();
        // should retrieve instance group members
        verify(this.instanceGroupClientMock).listInstances();
        // should retrieve instance metadata about each member
        verify(this.gceClientMock).getInstance(PROJECT, ZONE, FakeSingleZoneInstanceGroup.BASE_INSTANCE_NAME + "-1");
        verify(this.gceClientMock).getInstance(PROJECT, ZONE, FakeSingleZoneInstanceGroup.BASE_INSTANCE_NAME + "-2");
    }

    /**
     * If the actual size of the instance froup is less than the target size
     * (for example, when a target size has been requested but the group has not
     * reached its new desired state yet) the {@link GcePoolDriver} should
     * report the requested-but-not-yet-acquired instances as pseudo instances
     * in state {@link MachineState#REQUESTED}, to not fool the
     * {@link BaseCloudPool} from believing that the pool is too small and order
     * new machines to be started (and excessively increase the target size).
     */
    @Test
    public void listMachinesOnGroupThatHasNotReachedTargetSize() throws CloudPoolDriverException {
        // two outstanding instances: target size: 3, actual size: 1
        int targetSize = 3;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        List<Machine> machines = this.driver.listMachines();

        // verify that instances were correctly translated to machines
        assertThat(machines.size(), is(3));
        assertThat(machines.get(0), is(new InstanceToMachine().apply(simulatedGroup.instances().get(0))));
        assertThat(UrlUtils.basename(machines.get(1).getId()),
                is("requested-" + simulatedGroup.instanceTemplateName() + "-1"));
        assertThat(machines.get(1).getMachineState(), is(MachineState.REQUESTED));
        assertThat(UrlUtils.basename(machines.get(2).getId()),
                is("requested-" + simulatedGroup.instanceTemplateName() + "-2"));
        assertThat(machines.get(2).getMachineState(), is(MachineState.REQUESTED));
    }

    /**
     * A call to {@link GcePoolDriver#startMachines(int)} should call through to
     * resize the instance group.
     */
    @Test
    public void startMachines() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        List<Machine> machines = this.driver.startMachines(2);
        // psuedo-machines should be returned for requested instances
        assertThat(UrlUtils.basename(machines.get(0).getId()),
                is("requested-" + simulatedGroup.instanceTemplateName() + "-1"));
        assertThat(machines.get(0).getMachineState(), is(MachineState.REQUESTED));
        assertThat(UrlUtils.basename(machines.get(1).getId()),
                is("requested-" + simulatedGroup.instanceTemplateName() + "-2"));
        assertThat(machines.get(1).getMachineState(), is(MachineState.REQUESTED));

        //
        // verify calls to mock api clients
        //

        verify(this.instanceGroupClientMock).resize(3);
    }

    /**
     * A call to {@link GcePoolDriver#terminateMachine(String)} should call
     * through to resize the instance group.
     */
    @Test
    public void terminateMachine() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = simulatedGroup.instances().get(0).getSelfLink();
        this.driver.terminateMachine(instanceUrl);

        //
        // verify calls to mock api clients
        //

        verify(this.instanceGroupClientMock).deleteInstances(Arrays.asList(instanceUrl));
    }

    /**
     * It should not be allowed to attempt to delete an instance that is not a
     * member of the instance group.
     */
    @Test
    public void terminateNonMemberInstance() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                PROJECT, ZONE, "mysql-1");
        try {
            this.driver.terminateMachine(instanceUrl);
            fail("should not be possible to delete non-group member");
        } catch (NotFoundException e) {
            // expected
        }

        // should NOT call through to delete
        verify(this.instanceGroupClientMock, times(0)).deleteInstances(Arrays.asList(instanceUrl));
    }

    /**
     * An attempt to terminate a psuedo instance (a stand-in for a
     * requested-but-not-yet-acquired instance) should result in decrementing
     * the target size of the instance group, but no attempt should be made to
     * delete the pseudo instance.
     */
    @Test
    public void terminatePseudoInstance() {
        // two outstanding instances: target size: 2, actual size: 1
        int targetSize = 2;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        List<Machine> machines = this.driver.listMachines();

        // verify that instances were correctly translated to machines
        assertThat(machines.size(), is(2));
        String pseudoInstanceUrl = machines.get(1).getId();

        assertThat(UrlUtils.basename(pseudoInstanceUrl),
                is("requested-" + simulatedGroup.instanceTemplateName() + "-1"));

        this.driver.terminateMachine(pseudoInstanceUrl);

        // should call through to decrement group size
        verify(this.instanceGroupClientMock).resize(1);
        // should NOT call through to delete
        verify(this.instanceGroupClientMock, times(0)).deleteInstances(Arrays.asList(pseudoInstanceUrl));
    }

    /**
     * {@link GcePoolDriver#detachMachine(String)} should call through to
     * abandon instance in instance group API.
     */
    @Test
    public void detachInstance() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = simulatedGroup.instances().get(0).getSelfLink();

        this.driver.detachMachine(instanceUrl);

        // should call through to abandon instance
        verify(this.instanceGroupClientMock).abandonInstances(Arrays.asList(instanceUrl));
    }

    /**
     * It should not be possible to detach non-group members.
     */
    @Test
    public void detachNonMemberInstance() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                PROJECT, ZONE, "mysql-1");
        try {
            this.driver.detachMachine(instanceUrl);
            fail("should not be possible to detach non-group member");
        } catch (NotFoundException e) {
            // expected
        }

        // should NOT call through to abandon instance
        verify(this.instanceGroupClientMock, times(0)).abandonInstances(Arrays.asList(instanceUrl));
    }

    /**
     * setServiceState should call through to modify the instance's metadata.
     */
    @Test
    public void setServiceState() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        Instance instance = simulatedGroup.instances().get(0);
        Map<String, String> instanceMetadata = MetadataUtil.toMap(instance.getMetadata());
        String instanceUrl = instance.getSelfLink();

        this.driver.setServiceState(instanceUrl, ServiceState.IN_SERVICE);

        instanceMetadata.put(MetadataKeys.SERVICE_STATE, ServiceState.IN_SERVICE.name());
        Metadata expectedMetadata = instance.getMetadata().clone().setItems(MetadataUtil.toItems(instanceMetadata));

        // should call through to set metadata
        verify(this.gceClientMock).setMetadata(PROJECT, ZONE, UrlUtils.basename(instanceUrl), expectedMetadata);
    }

    /**
     * It should not be possible to set service state for a non-group member.
     */
    @Test
    public void setServiceStateOnNonMemberInstance() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                PROJECT, ZONE, "mysql-1");

        try {
            this.driver.setServiceState(instanceUrl, ServiceState.IN_SERVICE);
            fail("should not be possible to set metadata for a non-member instance");
        } catch (NotFoundException e) {
            // expected
        }

        // should NOT call through to set metadata
        verify(this.gceClientMock, times(0)).setMetadata(argThat(is(PROJECT)), argThat(is(ZONE)),
                argThat(is(UrlUtils.basename(instanceUrl))), argThat(isA(Metadata.class)));
    }

    /**
     * setMembershipStatus should call through to set the instance's metadata
     */
    @Test
    public void setMembershipStatus() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        Instance instance = simulatedGroup.instances().get(0);
        Map<String, String> instanceMetadata = MetadataUtil.toMap(instance.getMetadata());
        String instanceUrl = instance.getSelfLink();

        this.driver.setMembershipStatus(instanceUrl, MembershipStatus.blessed());

        instanceMetadata.put(MetadataKeys.MEMBERSHIP_STATUS,
                JsonUtils.toString(JsonUtils.toJson(MembershipStatus.blessed())));
        Metadata expectedMetadata = instance.getMetadata().clone().setItems(MetadataUtil.toItems(instanceMetadata));

        // should call through to set metadata
        verify(this.gceClientMock).setMetadata(PROJECT, ZONE, UrlUtils.basename(instanceUrl), expectedMetadata);
    }

    /**
     * It should not be possible to set membership status for a non-group
     * member.
     */
    @Test
    public void setMembershipStatusOnNonMemberInstance() {
        int targetSize = 1;
        int runningInstances = 1;
        FakeSingleZoneInstanceGroup simulatedGroup = new FakeSingleZoneInstanceGroup(POOL_DRIVER_CONFIG, targetSize,
                runningInstances);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                PROJECT, ZONE, "mysql-1");

        try {
            this.driver.setMembershipStatus(instanceUrl, MembershipStatus.blessed());
            fail("should not be possible to set metadata for a non-member instance");
        } catch (NotFoundException e) {
            // expected
        }

        // should NOT call through to set metadata
        verify(this.gceClientMock, times(0)).setMetadata(argThat(is(PROJECT)), argThat(is(ZONE)),
                argThat(is(UrlUtils.basename(instanceUrl))), argThat(isA(Metadata.class)));
    }

    private static DriverConfig driverConfig(String poolName, ProvisioningTemplate instanceTemplate) {
        return new DriverConfig(POOL_NAME,
                JsonUtils.toJson(new CloudApiSettings("src/test/resources/config/valid-service-account-key.json", null))
                        .getAsJsonObject(),
                JsonUtils.toJson(instanceTemplate).getAsJsonObject());
    }

    /**
     * Sets up the mock API clients to front a given fake GCE instance group.
     *
     * @param simulatedGroup
     */
    private void setUpMockedInstanceGroup(FakeSingleZoneInstanceGroup simulatedGroup) {
        LOG.debug("setting up mocked call to get instance group ...");
        when(this.instanceGroupClientMock.getInstanceGroup()).thenReturn(simulatedGroup.instanceGroupManager());
        LOG.debug("setting up mocked call to get instance group members {} ...",
                simulatedGroup.instances().stream().map(Instance::getName).collect(Collectors.toList()));
        when(this.instanceGroupClientMock.listInstances()).thenReturn(simulatedGroup.managedInstances());

        when(this.gceClientMock.singleZoneInstanceGroup(PROJECT, ZONE, INSTANCE_GROUP))
                .thenReturn(this.instanceGroupClientMock);
        when(this.gceClientMock.getInstanceTemplate(PROJECT, simulatedGroup.instanceTemplateName()))
                .thenReturn(simulatedGroup.instanceTemplate());

        for (Instance instance : simulatedGroup.instances()) {
            LOG.debug("setting up mocked call to get instance metadata for {} ...", instance.getName());
            when(this.gceClientMock.getInstance(PROJECT, UrlUtils.basename(instance.getZone()), instance.getName()))
                    .thenReturn(instance);
        }
    }

}
