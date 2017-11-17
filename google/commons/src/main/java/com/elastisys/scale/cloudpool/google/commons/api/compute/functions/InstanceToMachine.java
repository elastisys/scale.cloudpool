package com.elastisys.scale.cloudpool.google.commons.api.compute.functions;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.Machine.Builder;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.google.commons.api.compute.metadata.MetadataKeys;
import com.elastisys.scale.cloudpool.google.commons.utils.MetadataUtil;
import com.elastisys.scale.cloudpool.google.commons.utils.ZoneUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.NetworkInterface;

/**
 * Function for translating GCE {@link ManagedInstance}s into their
 * {@link Machine} counterparts.
 *
 */
public class InstanceToMachine implements Function<Instance, Machine> {

    @Override
    public Machine apply(Instance instance) {
        Builder builder = Machine.builder();

        // instance URL is machine id: For example,
        // https://www.googleapis.com/compute/v1/projects/<project>/zones/europe-west1-d/instances/webservers-d58p
        builder.id(instance.getSelfLink());

        builder.cloudProvider(CloudProviders.GCE);
        builder.region(ZoneUtils.regionName(ZoneUtils.zoneName(instance.getZone())));
        builder.machineSize(UrlUtils.basename(instance.getMachineType()));

        builder.machineState(new InstanceStatusToMachineStatus().apply(instance.getStatus()));
        builder.launchTime(UtcTime.parse(instance.getCreationTimestamp()));

        // only one network interface per instance appears to be supported
        List<NetworkInterface> networkInterfaces = instance.getNetworkInterfaces();
        if (networkInterfaces != null && !networkInterfaces.isEmpty()) {
            NetworkInterface networkInterface = networkInterfaces.get(0);
            String privateIp = networkInterface.getNetworkIP();
            if (privateIp != null) {
                builder.privateIp(privateIp);
            }
            List<AccessConfig> accessConfigs = networkInterface.getAccessConfigs();
            if (accessConfigs != null && !accessConfigs.isEmpty()) {
                String publicIp = accessConfigs.get(0).getNatIP();
                if (publicIp != null) {
                    builder.publicIp(publicIp);
                }
            }
        }

        ServiceState serviceState = extractServiceState(instance);
        if (serviceState != null) {
            builder.serviceState(serviceState);
        }
        MembershipStatus membershipStatus = extractMembershipStatus(instance);
        if (membershipStatus != null) {
            builder.membershipStatus(membershipStatus);
        }

        return builder.build();
    }

    private MembershipStatus extractMembershipStatus(Instance instance) {
        Map<String, String> metadata = MetadataUtil.toMap(instance.getMetadata());
        if (metadata.containsKey(MetadataKeys.MEMBERSHIP_STATUS)) {
            return JsonUtils.toObject(JsonUtils.parseJsonString(metadata.get(MetadataKeys.MEMBERSHIP_STATUS)),
                    MembershipStatus.class);
        }
        return null;
    }

    private ServiceState extractServiceState(Instance instance) {
        Map<String, String> metadata = MetadataUtil.toMap(instance.getMetadata());
        if (metadata.containsKey(MetadataKeys.SERVICE_STATE)) {
            return JsonUtils.toObject(JsonUtils.parseJsonString(metadata.get(MetadataKeys.SERVICE_STATE)),
                    ServiceState.class);
        }
        return null;
    }

}
