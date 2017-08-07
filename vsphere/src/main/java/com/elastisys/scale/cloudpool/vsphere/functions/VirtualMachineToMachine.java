package com.elastisys.scale.cloudpool.vsphere.functions;

import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.elastisys.scale.cloudpool.vsphere.tagger.TaggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.VirtualMachine;
import jersey.repackaged.com.google.common.base.Function;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;

/**
 * Convert a VirtualMachine to a {@link Machine}.
 */
public class VirtualMachineToMachine implements Function<VirtualMachine, Machine> {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachineToMachine.class);
    // To be used for MembershipStatus and ServiceState
    private Tagger tagger = TaggerFactory.getTagger();

    @Override
    public Machine apply(VirtualMachine vm) {
        Preconditions.checkArgument(vm != null, "received null instance");

        String id;
        DateTime launchTime;
        MachineState state;
        String cloudProvider = CloudProviders.VSPHERE;
        String region;
        String machineSize;
        Collection<String> publicIps;

        // A terminating VM can disappear at any time, so we need to check for this RuntimeException
        try {
            id = vm.getName();
            launchTime = extractDateTime(vm);
            state = extractMachineState(vm);
            region = extractRegion(vm);
            machineSize = extractMachineSize(vm);
            publicIps = extractPublicIps(vm);
        } catch (RuntimeException e) {
            if (!e.getMessage().contains("ManagedObjectNotFound")) {
                throw e;
            }
            LOG.debug("ManagedObjectNotFound: VM is gone");
            id = "unknown";
            launchTime = null;
            state = MachineState.TERMINATED;
            region = "unknown";
            machineSize = "unknown";
            publicIps = Lists.newArrayList();
        }

        // TODO: Only partially implemented
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        ServiceState serviceState = ServiceState.UNKNOWN;

        Machine machine = Machine.builder()
                .id(id)
                .launchTime(launchTime)
                .machineState(state)
                .membershipStatus(membershipStatus)
                .serviceState(serviceState)
                .cloudProvider(cloudProvider)
                .region(region)
                .machineSize(machineSize)
                .publicIps(publicIps)
                .build();

        return machine;
    }

    private Collection<String> extractPublicIps(VirtualMachine vm) {
        GuestInfo guestInfo = vm.getGuest();
        Collection<String> publicIps = Lists.newArrayList();
        if (guestInfo != null) {
            String ip = guestInfo.getIpAddress();
            if (ip != null) {
                publicIps.add(ip);
            }
        }

        return publicIps;
    }

    private DateTime extractDateTime(VirtualMachine vm) {
        VirtualMachineRuntimeInfo runtime = vm.getRuntime();
        if (runtime == null) {
            return null;
        }
        Calendar calendar = runtime.getBootTime();
        if(calendar != null){
            return DateTime.parse(calendar.toInstant().toString());
        }
        return null;
    }

    private MachineState extractMachineState(VirtualMachine vm) {
        MachineState state;
        VirtualMachineRuntimeInfo runtime = vm.getRuntime();
        if (runtime == null) {
            return MachineState.PENDING;
        }
        VirtualMachinePowerState powerState = runtime.getPowerState();

        if (powerState.equals(VirtualMachinePowerState.poweredOn)) {
            state = MachineState.RUNNING;
        } else if (powerState.equals(VirtualMachinePowerState.poweredOff)) {
            state = MachineState.TERMINATED;
        } else {
            // The VM must be suspended, see VirtualMachinePowerState.
            state = MachineState.RUNNING;
        }
        return state;
    }

    private String extractRegion(VirtualMachine vm) {
        String region;
        try {
            region = vm.getResourcePool().getName();
        } catch (RemoteException e) {
            LOG.warn("failed to extract region for {}: " + "no resource pool information available", vm.getName());
            region = "unknown";
        }
        return region;
    }

    private String extractMachineSize(VirtualMachine vm) {
        String machineSize;
        VirtualMachineConfigInfo configInfo = vm.getConfig();
        if (configInfo == null) {
            return "unknown";
        }
        VirtualHardware hardware = configInfo.getHardware();
        if (hardware == null) {
            return "unknown";
        }
        machineSize = String.format("cpu-%d-mem-%d", hardware.getNumCPU(), hardware.getMemoryMB());
        return machineSize;
    }

}
