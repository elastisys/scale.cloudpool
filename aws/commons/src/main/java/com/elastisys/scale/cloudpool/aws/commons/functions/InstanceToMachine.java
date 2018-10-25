package com.elastisys.scale.cloudpool.aws.commons.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.precond.Preconditions;
import com.google.gson.JsonObject;

/**
 * {@link Function} that translates an EC2 API {@link Instance} representation
 * to its corresponding {@link Machine} representation.
 */
public class InstanceToMachine implements Function<Instance, Machine> {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceToMachine.class);

    /**
     * Converts a {@link Instance} to its {@link Machine} representation.
     *
     * @param instance
     * @return
     */
    public static Machine convert(Instance instance) {
        return new InstanceToMachine().apply(instance);
    }

    /**
     * Converts an {@link Instance} to a {@link Machine}. The request time is
     * set to null, since AWS does not report it, and attempting to manually
     * keep track of it is awkward and brittle. It will have to stay this way
     * until either they add support for it, or we figure out a way to do it
     * that is not brittle.
     *
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public Machine apply(Instance instance) {
        Preconditions.checkArgument(instance != null, "received null instance");

        String id = instance.getInstanceId();
        MachineState machineState = new InstanceStateToMachineState().apply(instance.getState());
        DateTime launchtime = new DateTime(instance.getLaunchTime(), DateTimeZone.UTC);
        List<String> publicIps = new ArrayList<>();
        List<String> privateIps = new ArrayList<>();
        String publicIp = instance.getPublicIpAddress();
        if (publicIp != null) {
            publicIps.add(publicIp);
        }
        String privateIp = instance.getPrivateIpAddress();
        if (privateIp != null) {
            privateIps.add(privateIp);
        }

        // extract membership status if status tag is present on instance
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        Optional<String> membershipStatusTag = getTagValue(instance, ScalingTags.MEMBERSHIP_STATUS_TAG);
        if (membershipStatusTag.isPresent()) {
            membershipStatus = JsonUtils.toObject(JsonUtils.parseJsonString(membershipStatusTag.get()),
                    MembershipStatus.class);
        }

        // set the service state if a service state tag is present on instance
        ServiceState serviceState = ServiceState.UNKNOWN;
        Optional<String> serviceStateTag = getTagValue(instance, ScalingTags.SERVICE_STATE_TAG);
        if (serviceStateTag.isPresent()) {
            serviceState = ServiceState.valueOf(serviceStateTag.get());
        }

        // if this is a spot request instance, the provider should be AWS_SPOT
        // rather than AWS_EC2
        String cloudProvider = CloudProviders.AWS_EC2;
        if (instance.getSpotInstanceRequestId() != null) {
            cloudProvider = CloudProviders.AWS_SPOT;
        }
        String region = extractRegion(instance);

        JsonObject metadata = JsonUtils.toJson(instance).getAsJsonObject();
        return Machine.builder().id(id).machineState(machineState).cloudProvider(cloudProvider).region(region)
                .machineSize(instance.getInstanceType()).membershipStatus(membershipStatus).serviceState(serviceState)
                .launchTime(launchtime).publicIps(publicIps).privateIps(privateIps).metadata(metadata).build();
    }

    /**
     * Returns the region (for example, {@code eu-west-1}) that the
     * {@link Instance} was launched in by looking at the availability zone.
     *
     * @param instance
     * @return
     */
    private String extractRegion(Instance instance) {
        if (instance.getPlacement() == null || instance.getPlacement().getAvailabilityZone() == null) {
            LOG.warn("failed to extract region for {}: " + "no placement/availability zone information available",
                    instance.getInstanceId());
            return "unknown";
        }
        // availability zone is region + letter, for instance 'eu-west-1a'
        String availabilityZone = instance.getPlacement().getAvailabilityZone();
        String region = availabilityZone.substring(0, availabilityZone.length() - 1);
        return region;
    }

    /**
     * Retrieves a certain tag value from an {@link Instance}.
     *
     * @param instance
     * @param tagKey
     * @return
     */
    private Optional<String> getTagValue(Instance instance, String tagKey) {
        List<Tag> tags = instance.getTags();
        for (Tag tag : tags) {
            if (tag.getKey().equals(tagKey)) {
                return Optional.of(tag.getValue());
            }
        }
        return Optional.empty();
    }
}
