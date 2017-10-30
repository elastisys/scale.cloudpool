package com.elastisys.scale.cloudpool.aws.spot.metadata;

import static com.google.common.base.Preconditions.checkArgument;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceToMachine;

/**
 * A meta data type that includes both a {@link SpotInstanceRequest} and its
 * spot {@link Instance}, if the request has been fulfilled.
 */
public class InstancePairedSpotRequest {

    /** The {@link SpotInstanceRequest}. */
    private final SpotInstanceRequest request;

    /**
     * The acquired {@link Instance} if one has been assigned, <code>null</code>
     * if the {@link SpotInstanceRequest} hasn't been fulfilled.
     */
    private final Instance instance;

    public InstancePairedSpotRequest(SpotInstanceRequest request, Instance instance) {
        checkArgument(request != null, "no spot instance request given");
        this.request = request;
        this.instance = instance;
    }

    /**
     * Returns the spot instance request id.
     */
    public String getId() {
        return getRequest().getSpotInstanceRequestId();
    }

    /**
     * @return the {@link #request}
     */
    public SpotInstanceRequest getRequest() {
        return this.request;
    }

    /**
     * @return the {@link #instance}
     */
    public Instance getInstance() {
        return this.instance;
    }

    /**
     * Indicates if this spot request has been fulfilled and has an associtaed
     * spot {@link Instance}.
     *
     * @return
     */
    public boolean hasInstance() {
        return this.instance != null;
    }

    /**
     * Maps the {@link InstancePairedSpotRequest} to a {@link MachineState}. The
     * rules involved in this translation are as follows:
     * <ul>
     * <li>Unfulfilled spot requests (open, not yet assigned an instance) are
     * returned with state `REQUESTED`.</li>
     * <li>Active fulfilled spot requests (that are assigned an instance) are
     * returned with a state that corresponds to the state of their associated
     * instances.</li>
     * <li>Closed and canceled spot requests are returned with a `TERMINATED`
     * state.</li>
     * <li>Failed spot requests are returned with state `REJECTED`.</li>
     * </ul>
     *
     * For a complete coverage of the different states a spot instance request
     * can enter, refer to the <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-spot-bid-specifications.html"
     * >EC2 documentation</a>
     *
     * @return
     */
    public MachineState getMachineState() {
        switch (this.request.getState()) {
        case "active":
            if (hasInstance()) {
                return new InstanceToMachine().apply(getInstance()).getMachineState();
            }
            // no associated instance, assume it has recently been terminated
            return MachineState.TERMINATED;
        case "open":
            return MachineState.REQUESTED;
        case "failed":
            return MachineState.REJECTED;
        case "closed":
            return MachineState.TERMINATED;
        case "cancelled":
            return MachineState.TERMINATED;
        default:
            throw new IllegalArgumentException(
                    String.format("Unrecognized spot instance request state '%s'", this.request.getState()));
        }
    }
}
