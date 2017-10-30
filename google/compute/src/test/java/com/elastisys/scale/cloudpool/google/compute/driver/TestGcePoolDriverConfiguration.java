package com.elastisys.scale.cloudpool.google.compute.driver;

import static com.elastisys.scale.cloudpool.google.compute.driver.ConfigTestUtils.validDriverConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;

/**
 * Exercises the {@link GoogleComputeEnginePoolDriver} with a mocked
 * {@link ComputeClient}.
 */
public class TestGcePoolDriverConfiguration {

    /** Sample cloud pool name. */
    private static final String POOL_NAME = "webserver-pool";

    /** Mocked GCE client used by the GCE pool driver under test. */
    private ComputeClient gceClientMock = mock(ComputeClient.class);
    /** Object under test. */
    private GoogleComputeEnginePoolDriver driver;

    @Before
    public void beforeTestMethod() {
        this.driver = new GoogleComputeEnginePoolDriver(this.gceClientMock);
    }

    /**
     * A valid {@link DriverConfig} should be accepted.
     */
    @Test
    public void configure() {
        assertThat(this.driver.config(), is(nullValue()));
        this.driver.configure(validDriverConfig(POOL_NAME));
        assertThat(this.driver.config(), is(validDriverConfig(POOL_NAME)));
    }

    /**
     * An invalid {@link DriverConfig} should be rejected and the old config
     * kept.
     */
    @Test
    public void configureWithInvalidDriverConfig() {
        this.driver.configure(validDriverConfig(POOL_NAME));
        assertThat(this.driver.config(), is(validDriverConfig(POOL_NAME)));

        try {
            this.driver.configure(ConfigTestUtils.invalidDriverConfig(POOL_NAME));
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // verify that old config is kept
        assertThat(this.driver.config(), is(validDriverConfig(POOL_NAME)));
    }

    /**
     * It should be possible to set a new {@link DriverConfig}.
     */
    @Test
    public void reconfigure() {
        this.driver.configure(validDriverConfig(POOL_NAME));
        assertThat(this.driver.config(), is(validDriverConfig(POOL_NAME)));

        // reconfigure
        this.driver.configure(ConfigTestUtils.validDriverConfig("new-pool"));
        assertThat(this.driver.config(), is(validDriverConfig("new-pool")));
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void listMachinesBeforeConfigured() {
        this.driver.listMachines();
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void startMachinesBeforeConfigured() {
        this.driver.startMachines(2);
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void terminateMachineBeforeConfigured() {
        this.driver.terminateMachines(asList(
                "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-instance-group-s4s0"));
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void detachMachineBeforeConfigured() {
        this.driver.detachMachine(
                "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-instance-group-s4s0");
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void setServiceStateBeforeConfigured() {
        this.driver.setServiceState(
                "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-instance-group-s4s0",
                ServiceState.IN_SERVICE);
    }

    /**
     * No operations are allowed until a config has been set.
     */
    @Test(expected = IllegalStateException.class)
    public void setMembershipStatusBeforeConfigured() {
        this.driver.setMembershipStatus(
                "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-instance-group-s4s0",
                MembershipStatus.blessed());
    }

    /**
     * Attaching a machine to managed instance group is not supported by the GCE
     * API.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void attachMachineNotImplemented() {
        this.driver.attachMachine(
                "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-instance-group-s4s0");
    }

}
