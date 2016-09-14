package com.elastisys.scale.cloudpool.openstack.functions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.openstack.driver.Constants;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Translates a {@link Server} from the OpenStack API to its {@link Machine}
 * counterpart.
 */
public class ServerToMachine implements Function<Server, Machine> {

    /** {@link Address} type that denotes a floating (public) IP address. */
    private static final String FLOATING = "floating";

    /**
     * The name of the cloud provider (for example, "OpenStack", "RackSpace",
     * "CityCloud").
     */
    private final String cloudProvider;

    /**
     * The region being connected to. For example, {@code Kna1} or {@code LON}.
     */
    private final String region;

    /**
     * Constructs a {@link ServerToMachine} conversion {@link Function}.
     *
     * @param cloudProvider
     *            The name of the cloud provider (for example, "OpenStack",
     *            "RackSpace", "CityCloud").
     * @param The
     *            region being connected to. For example, {@code Kna1} or
     *            {@code LON}.
     */
    public ServerToMachine(String cloudProvider, String region) {
        this.cloudProvider = cloudProvider;
        this.region = region;
    }

    /**
     * Converts a {@link Server} from the OpenStack API to its {@link Machine}
     * representation.
     *
     * @param server
     *            The {@link Server} representation.
     * @return The corresponding {@link Machine} representation.
     */
    @Override
    public Machine apply(Server server) {
        return asMachine(server);
    }

    /**
     * Converts a {@link Server} from the OpenStack API to its {@link Machine}
     * representation.
     */
    private Machine asMachine(Server server) {
        MachineState machineState = new StatusToMachineState().apply(server.getStatus());

        final DateTime creationTime = new DateTime(server.getCreated(), DateTimeZone.UTC);
        final DateTime launchedAtTime = new DateTime(server.getLaunchedAt(), DateTimeZone.UTC);

        // collect public (floating) and private (fixed) IP addresses assigned
        // to server
        List<String> publicIps = Lists.newArrayList();
        List<String> privateIps = Lists.newArrayList();
        Map<String, List<? extends Address>> serverAddresses = server.getAddresses().getAddresses();
        for (Entry<String, List<? extends Address>> networkAddresses : serverAddresses.entrySet()) {
            List<? extends Address> addresses = networkAddresses.getValue();
            for (Address address : addresses) {
                if (address.getType().equals(FLOATING)) {
                    publicIps.add(address.getAddr());
                } else {
                    privateIps.add(address.getAddr());
                }
            }
        }

        // extract membership status if tag has been set on server
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        if (server.getMetadata().containsKey(Constants.MEMBERSHIP_STATUS_TAG)) {
            membershipStatus = JsonUtils.toObject(
                    JsonUtils.parseJsonString(server.getMetadata().get(Constants.MEMBERSHIP_STATUS_TAG)),
                    MembershipStatus.class);
        }

        // extract service state if tag has been set on server
        ServiceState serviceState = ServiceState.UNKNOWN;
        if (server.getMetadata().containsKey(Constants.SERVICE_STATE_TAG)) {
            serviceState = ServiceState.valueOf(server.getMetadata().get(Constants.SERVICE_STATE_TAG));
        }
        JsonObject metadata = JsonUtils.toJson(server).getAsJsonObject();
        return Machine.builder().id(server.getId()).machineState(machineState).cloudProvider(this.cloudProvider)
                .region(this.region).machineSize(server.getFlavor().getName()).membershipStatus(membershipStatus)
                .serviceState(serviceState).requestTime(creationTime).launchTime(launchedAtTime).publicIps(publicIps)
                .privateIps(privateIps).metadata(metadata).build();
    }
}
