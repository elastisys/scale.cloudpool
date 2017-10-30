package com.elastisys.scale.cloudpool.google.compute.driver;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceGroupUrl;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceTemplateUrl;
import com.elastisys.scale.cloudpool.google.compute.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.google.api.services.compute.model.Instance;

/**
 * Exercises the {@link GoogleComputeEnginePoolDriver} against a mocked
 * multi-zone instance group.
 * <p/>
 * These tests only verify that the {@link GoogleComputeEnginePoolDriver} makes
 * use of a *multi-zone* instance group when the {@link DriverConfig}'s
 * {@link ProvisioningTemplate} specifies a region rather than a zone. More
 * extensive testing of the {@link GoogleComputeEnginePoolDriver} logic can be
 * found in {@link TestSingleZoneGcePoolDriverOperation}.
 */
public class TestMultiZoneGcePoolDriverOperation {
    private static final Logger LOG = LoggerFactory.getLogger(TestSingleZoneGcePoolDriverOperation.class);

    /** Sample project name. */
    private static final String PROJECT = "my-project";
    /** Sample region name. */
    private static final String REGION = "eu-west1";
    /** Sample instance group name. */
    private static final String INSTANCE_GROUP = "webserver-instance-group";
    private static final String INSTANCE_GROUP_URL = InstanceGroupUrl.managedRegional(PROJECT, REGION, INSTANCE_GROUP)
            .getUrl();

    /** Sample cloud pool name. */
    private static final String POOL_NAME = "webserver-pool";

    private static final DriverConfig POOL_DRIVER_CONFIG = driverConfig(POOL_NAME,
            new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, REGION, null));

    /** Mocked GCE client used by the GCE pool driver under test. */
    private ComputeClient gceClientMock = mock(ComputeClient.class);
    /**
     * Mocked (multi-zone) instance group client client used by the GCE pool
     * driver under test.
     */
    private InstanceGroupClient instanceGroupClientMock = mock(InstanceGroupClient.class);
    /** Object under test. */
    private GoogleComputeEnginePoolDriver driver;

    @Before
    public void beforeTestMethod() {
        this.driver = new GoogleComputeEnginePoolDriver(this.gceClientMock);
        this.driver.configure(POOL_DRIVER_CONFIG);
        reset(this.gceClientMock);
        reset(this.instanceGroupClientMock);
    }

    /**
     * Verify that when {@link DriverConfig} specifies a region in its
     * {@link ProvisioningTemplate}, the call is made through the multi-zone
     * instance group client.
     */
    @Test
    public void listMachines() {
        FakeMultiZoneInstanceGroup simulatedGroup = new FakeMultiZoneInstanceGroup(POOL_DRIVER_CONFIG, 2, 2);
        setUpMockedInstanceGroup(simulatedGroup);

        this.driver.listMachines();

        //
        // verify expected calls to backend API clients
        //

        // should call to get a multi-zone instance group client
        verify(this.gceClientMock).multiZoneInstanceGroup(INSTANCE_GROUP_URL);
        // should retrieve instance group metadata (targetSize)
        verify(this.instanceGroupClientMock).getInstanceGroup();
        // should retrieve instance group members
        verify(this.instanceGroupClientMock).listInstances();
    }

    /**
     * Verify that when {@link DriverConfig} specifies a region in its
     * {@link ProvisioningTemplate}, the call is made through the multi-zone
     * instance group client.
     */
    @Test
    public void startMachines() {
        FakeMultiZoneInstanceGroup simulatedGroup = new FakeMultiZoneInstanceGroup(POOL_DRIVER_CONFIG, 1, 1);
        setUpMockedInstanceGroup(simulatedGroup);

        this.driver.startMachines(2);

        //
        // verify calls to mock api clients
        //
        // should call through to resize on multi-zone instance group
        verify(this.gceClientMock).multiZoneInstanceGroup(INSTANCE_GROUP_URL);
        verify(this.instanceGroupClientMock).resize(3);
    }

    /**
     * Verify that when {@link DriverConfig} specifies a region in its
     * {@link ProvisioningTemplate}, the call is made through the multi-zone
     * instance group client.
     */
    @Test
    public void terminateMachines() {
        FakeMultiZoneInstanceGroup simulatedGroup = new FakeMultiZoneInstanceGroup(POOL_DRIVER_CONFIG, 1, 1);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = simulatedGroup.instances().get(0).getSelfLink();
        this.driver.terminateMachines(asList(instanceUrl));

        //
        // verify calls to mock api clients
        //
        // should call through to delete instance on multi-zone instance group
        verify(this.gceClientMock, atLeast(1)).multiZoneInstanceGroup(INSTANCE_GROUP_URL);
        verify(this.instanceGroupClientMock).deleteInstances(Arrays.asList(instanceUrl));
    }

    /**
     * Verify that when {@link DriverConfig} specifies a region in its
     * {@link ProvisioningTemplate}, the call is made through the multi-zone
     * instance group client.
     */
    @Test
    public void detachInstance() {
        FakeMultiZoneInstanceGroup simulatedGroup = new FakeMultiZoneInstanceGroup(POOL_DRIVER_CONFIG, 1, 1);
        setUpMockedInstanceGroup(simulatedGroup);

        String instanceUrl = simulatedGroup.instances().get(0).getSelfLink();

        this.driver.detachMachine(instanceUrl);

        // should call through to abandon instance on multi-zone instance group
        verify(this.gceClientMock, atLeast(1)).multiZoneInstanceGroup(INSTANCE_GROUP_URL);
        verify(this.instanceGroupClientMock).abandonInstances(Arrays.asList(instanceUrl));
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
    private void setUpMockedInstanceGroup(FakeMultiZoneInstanceGroup simulatedGroup) {
        LOG.debug("setting up mocked call to get instance group ...");
        when(this.instanceGroupClientMock.getInstanceGroup()).thenReturn(simulatedGroup.instanceGroupManager());
        LOG.debug("setting up mocked call to get instance group members {} ...",
                simulatedGroup.instances().stream().map(Instance::getName).collect(Collectors.toList()));
        when(this.instanceGroupClientMock.listInstances()).thenReturn(simulatedGroup.managedInstances());

        when(this.gceClientMock.multiZoneInstanceGroup(INSTANCE_GROUP_URL)).thenReturn(this.instanceGroupClientMock);
        when(this.gceClientMock
                .getInstanceTemplate(InstanceTemplateUrl.from(PROJECT, simulatedGroup.instanceTemplateName()).getUrl()))
                        .thenReturn(simulatedGroup.instanceTemplate());

        for (Instance instance : simulatedGroup.instances()) {
            String zone = UrlUtils.basename(instance.getZone());
            LOG.debug("setting up mocked call to get instance metadata for {} ...",
                    PROJECT + "/" + zone + "/" + instance.getName());
            when(this.gceClientMock.getInstance(instance.getSelfLink())).thenReturn(instance);
        }
    }

}
