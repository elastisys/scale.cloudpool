package com.elastisys.scale.cloudpool.openstack.functions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.Machine.Builder;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.openstack.driver.Constants;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ip.IsPrivateIp;

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

        Builder builder = Machine.builder();
        builder.id(server.getId());
        builder.cloudProvider(this.cloudProvider);
        builder.region(this.region);
        builder.machineSize(server.getFlavor().getName());
        builder.machineState(new StatusToMachineState().apply(server.getStatus()));
        builder.requestTime(new DateTime(server.getCreated(), DateTimeZone.UTC));
        builder.launchTime(new DateTime(server.getLaunchedAt(), DateTimeZone.UTC));

        Addresses serverAddresses = server.getAddresses();
        if (serverAddresses != null) {
            collectIps(builder, server.getAddresses());
        }

        // extract membership status if tag has been set on server
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        if (server.getMetadata().containsKey(Constants.MEMBERSHIP_STATUS_TAG)) {
            membershipStatus = JsonUtils.toObject(
                    JsonUtils.parseJsonString(server.getMetadata().get(Constants.MEMBERSHIP_STATUS_TAG)),
                    MembershipStatus.class);
        }
        builder.membershipStatus(membershipStatus);

        // extract service state if tag has been set on server
        ServiceState serviceState = ServiceState.UNKNOWN;
        if (server.getMetadata().containsKey(Constants.SERVICE_STATE_TAG)) {
            serviceState = ServiceState.valueOf(server.getMetadata().get(Constants.SERVICE_STATE_TAG));
        }
        builder.serviceState(serviceState);

        builder.metadata(JsonUtils.toJson(server).getAsJsonObject());

        return builder.build();

    }

    private void collectIps(Builder builder, Addresses serverAddresses) {
        Map<String, List<? extends Address>> networkAddresses = serverAddresses.getAddresses();
        if (networkAddresses != null) {
            for (Entry<String, List<? extends Address>> networkIps : networkAddresses.entrySet()) {
                List<? extends Address> ips = networkIps.getValue();
                for (Address ip : ips) {
                    String addressType = ip.getType();
                    // The "OS-EXT-IPS:type" field may be set to indicate if
                    // this is a fixed (private) or floating (public) IP
                    if (addressType != null) {
                        if (addressType.equals(FLOATING)) {
                            builder.publicIp(ip.getAddr());
                        } else {
                            builder.privateIp(ip.getAddr());
                        }
                    } else {
                        // If no address type is assigned, we fall back to
                        // checking if the IP address is in a private
                        // address range
                        if (new IsPrivateIp().test(ip.getAddr())) {
                            builder.privateIp(ip.getAddr());
                        } else {
                            builder.publicIp(ip.getAddr());
                        }
                    }
                }
            }
        }

    }
}
