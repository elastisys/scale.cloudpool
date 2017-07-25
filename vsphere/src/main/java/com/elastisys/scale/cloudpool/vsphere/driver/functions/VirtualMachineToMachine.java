package com.elastisys.scale.cloudpool.vsphere.driver.functions;

import com.elastisys.scale.cloudpool.api.types.*;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.elastisys.scale.cloudpool.vsphere.tagger.TaggerFactory;
import com.google.common.base.Preconditions;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;

import jersey.repackaged.com.google.common.base.Function;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Calendar;

public class VirtualMachineToMachine implements Function<VirtualMachine, Machine> {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachineToMachine.class);
    private Tagger tagger = TaggerFactory.getTagger();

    @Override
    public Machine apply(VirtualMachine vm) {
        Preconditions.checkArgument(vm != null, "received null instance");

        String id = vm.getName();
        DateTime launchTime = extractDateTime(vm);
        MachineState state = extractMachineState(vm);
        String cloudProvider = CloudProviders.VSPHERE;
        String region = extractRegion(vm);
        String machineSize = extractMachineSize(vm);

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
                .build();

        return machine;
    }

    private DateTime extractDateTime(VirtualMachine vm) {
        Calendar calendar = vm.getRuntime().getBootTime();
        return DateTime.parse(calendar.toInstant().toString());
    }

    private MachineState extractMachineState(VirtualMachine vm) {
        MachineState state;
        VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();

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
        VirtualHardware hardware = vm.getConfig().getHardware();
        machineSize = String.format("%dvCPU %dMB RAM", hardware.getNumCPU(), hardware.getMemoryMB());
        return machineSize;
    }

}
