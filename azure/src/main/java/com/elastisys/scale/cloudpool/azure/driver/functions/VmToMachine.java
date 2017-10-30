package com.elastisys.scale.cloudpool.azure.driver.functions;

import static com.elastisys.scale.cloudpool.azure.driver.Constants.MEMBERSHIP_STATUS_TAG;
import static com.elastisys.scale.cloudpool.azure.driver.Constants.SERVICE_STATE_TAG;

import java.util.function.Function;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.Machine.Builder;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;

public class VmToMachine implements Function<VirtualMachine, Machine> {
    static Logger LOG = LoggerFactory.getLogger(VmToMachine.class);

    @Override
    public Machine apply(VirtualMachine vm) {
        Builder builder = Machine.builder();

        builder.id(vm.id());
        builder.cloudProvider(CloudProviders.AZURE);
        builder.region(vm.regionName());
        builder.machineSize(vm.size().toString());

        NetworkInterface nic = vm.getPrimaryNetworkInterface();
        if (nic != null) {
            builder.privateIp(nic.primaryPrivateIP());
            NicIPConfiguration primaryIpConfig = nic.primaryIPConfiguration();
            if (primaryIpConfig.publicIPAddressId() != null) {
                String publicIp = primaryIpConfig.getPublicIPAddress().ipAddress();
                if (publicIp != null) {
                    builder.publicIp(publicIp);
                }
            }
        }

        DateTime launchTime = extractProvisioningTime(vm);
        builder.launchTime(launchTime);
        if (launchTime == null) {
            LOG.warn("failed to determine provisioning time for VM {}", vm.id());
        }

        builder.machineState(new VmToMachineState().apply(vm));

        // extract membership status if tag has been set on server
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        if (vm.tags().containsKey(MEMBERSHIP_STATUS_TAG)) {
            membershipStatus = JsonUtils.toObject(JsonUtils.parseJsonString(vm.tags().get(MEMBERSHIP_STATUS_TAG)),
                    MembershipStatus.class);
        }
        builder.membershipStatus(membershipStatus);

        // extract service state if tag has been set on server
        ServiceState serviceState = ServiceState.UNKNOWN;
        if (vm.tags().containsKey(SERVICE_STATE_TAG)) {
            serviceState = ServiceState.valueOf(vm.tags().get(SERVICE_STATE_TAG));
        }
        builder.serviceState(serviceState);

        return builder.build();
    }

    /**
     * Tries to determine the start time of an Azure VM by looking at the time
     * stamp of its <code>ProvisioningState</code>. <code>null</code> will be
     * returned on failure to determine this time stamp.
     * <p/>
     * Note, however, that this timestamp may not always be the time at which
     * the VM was launched, as it appears to be updated, for example, when
     * updating a VM tag.
     * <p/>
     * See
     * http://stackoverflow.com/questions/41036371/what-is-the-correct-way-of-determining-the-launch-time-of-an-azure-vm
     * <p/>
     * The correct way is probably to query the Azure activity log, but the
     * monitoring manager part is not yet included in the official java SDK.
     *
     * @param vm
     * @return
     */
    private DateTime extractProvisioningTime(VirtualMachine vm) {
        // since there is no obvious way of determining the VM's launch time,
        // look at the latest provisioning time stamp
        for (InstanceViewStatus status : vm.instanceView().statuses()) {
            if (status.code().toLowerCase().contains("provisioningstate")) {
                if (status.time() != null) {
                    return status.time();
                }
            }
        }
        return null;
    }

}
