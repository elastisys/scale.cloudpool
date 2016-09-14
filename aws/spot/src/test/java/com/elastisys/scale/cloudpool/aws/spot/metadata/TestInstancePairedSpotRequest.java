package com.elastisys.scale.cloudpool.aws.spot.metadata;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.aws.spot.util.SpotTestUtil;

/**
 * Exercise the {@link InstancePairedSpotRequest} class.
 */
public class TestInstancePairedSpotRequest {

    /**
     * Test {@link MachineState} conversion of open (unfulfilled) spot request.
     */
    @Test
    public void getMachineStateOnUnfulfilledRequest() {
        InstancePairedSpotRequest openRequest = new InstancePairedSpotRequest(
                SpotTestUtil.spotRequest("sir-1", "open", null), null);

        assertThat(openRequest.getMachineState(), is(MachineState.REQUESTED));
    }

    /**
     * Test {@link MachineState} conversion of closed spot request.
     */
    @Test
    public void getMachineStateOnClosedRequest() {
        InstancePairedSpotRequest closedRequest = new InstancePairedSpotRequest(
                SpotTestUtil.spotRequest("sir-1", "closed", null), null);

        assertThat(closedRequest.getMachineState(), is(MachineState.TERMINATED));
    }

    /**
     * Test {@link MachineState} conversion of cancelled spot request.
     */
    @Test
    public void getMachineStateOnCancelledRequest() {
        InstancePairedSpotRequest cancelledRequest = new InstancePairedSpotRequest(
                SpotTestUtil.spotRequest("sir-1", "cancelled", null), null);

        assertThat(cancelledRequest.getMachineState(), is(MachineState.TERMINATED));
    }

    /**
     * Test {@link MachineState} conversion of failed spot request.
     */
    @Test
    public void getMachineStateOnFailedRequest() {
        InstancePairedSpotRequest failedRequest = new InstancePairedSpotRequest(
                SpotTestUtil.spotRequest("sir-1", "failed", null), null);

        assertThat(failedRequest.getMachineState(), is(MachineState.REJECTED));
    }

    /**
     * Test {@link MachineState} conversion of an active (fulfilled) spot
     * request. {@link MachineState} should be set to taht of the assigned
     * instance.
     */
    @Test
    public void getMachineStateOnFulfilledRequest() {
        // active request with pending instance
        InstancePairedSpotRequest activeRequest = new InstancePairedSpotRequest(
                SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.Pending, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.PENDING));

        // active request with running instance
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.Running, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.RUNNING));

        // active request with shutting-down instance
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.ShuttingDown, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.TERMINATING));

        // active request with terminated instance
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.Terminated, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.TERMINATED));

        // active request with stopping instance
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.Stopping, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.TERMINATING));

        // active request with stopped instance
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"),
                SpotTestUtil.instance("i-1", InstanceStateName.Stopped, "sir-1"));
        assertThat(activeRequest.getMachineState(), is(MachineState.TERMINATED));

        // fulfilled without instance, which e.g. may have just been terminated
        activeRequest = new InstancePairedSpotRequest(SpotTestUtil.spotRequest("sir-1", "active", "i-1"), null);
        assertThat(activeRequest.getMachineState(), is(MachineState.TERMINATED));
    }

    /**
     * Test {@link MachineState} conversion when {@link SpotInstanceRequest}
     * state is not recognized as a valid state.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getMachineStateOnUnrecognizedSpotRequestState() {
        InstancePairedSpotRequest badStateRequest = new InstancePairedSpotRequest(
                new SpotInstanceRequest().withSpotInstanceRequestId("sir-1").withState("badstate"), null);
        badStateRequest.getMachineState();
    }
}
