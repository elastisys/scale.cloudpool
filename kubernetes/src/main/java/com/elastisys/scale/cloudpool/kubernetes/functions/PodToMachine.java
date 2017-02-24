package com.elastisys.scale.cloudpool.kubernetes.functions;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.Machine.Builder;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A {@link Function} that takes a {@link Pod} API object and converts it to a
 * {@link Machine} instance.
 */
public class PodToMachine implements Function<Pod, Machine> {
    /**
     * Value to use for the {@link Machine} {@code cloudProvider} field (which
     * is mandatory, but not quite applicable to the
     * {@link KubernetesCloudPool}).
     */
    public static final String CLOUD_PROVIDER = "Kubernetes";
    /**
     * Value to use for the {@link Machine} {@code region} field (which is
     * mandatory, but not quite applicable to the {@link KubernetesCloudPool}).
     */
    public static final String REGION = "N/A";
    /**
     * Value to use for the {@link Machine} {@code machineSize} field (which is
     * mandatory, but not quite applicable to the {@link KubernetesCloudPool}).
     */
    public static final String MACHINE_SIZE = "Pod";

    @Override
    public Machine apply(Pod pod) {
        Builder builder = Machine.builder();

        builder.id(pod.metadata.name);
        builder.machineState(new PodStateToMachineState().apply(pod.status.phase));
        builder.cloudProvider(CLOUD_PROVIDER);
        builder.region(REGION);
        builder.machineSize(MACHINE_SIZE);

        if (pod.metadata != null && pod.metadata.creationTimestamp != null) {
            builder.requestTime(pod.metadata.creationTimestamp);
        }

        if (pod.status != null) {
            if (pod.status.startTime != null) {
                // note: if pod isn't running, no startTime will be available
                builder.launchTime(pod.status.startTime);
            }

            // if pod isn't running, no IPs will be available
            if (pod.status.hostIP != null) {
                builder.publicIp(pod.status.hostIP);
            }
            if (pod.status.podIP != null) {
                builder.privateIp(pod.status.podIP);
            }
        }
        builder.serviceState(ServiceState.UNKNOWN);
        builder.membershipStatus(MembershipStatus.defaultStatus());
        builder.metadata(JsonUtils.toJson(pod));
        return builder.build();
    }

}
