package com.elastisys.scale.cloudpool.vsphere.driver.functions;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.google.common.base.Preconditions;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.mo.VirtualMachine;

import jersey.repackaged.com.google.common.base.Function;

public class VirtualMachineToMachine implements Function<VirtualMachine, Machine> {

    @Override
    public Machine apply(VirtualMachine vm) {
        Preconditions.checkArgument(vm != null, "received null instance");

        String id = vm.getName();
        String stateStr = vm.getGuest().getGuestState();
        MachineState state;
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();

        if (stateStr.equals("notRunning")) {
            state = MachineState.TERMINATED;
            membershipStatus = MembershipStatus.disposable();
        } else if (stateStr.equals("running")) {
            state = MachineState.RUNNING;
        } else {
            System.out.println("Found strange state: " + stateStr);
            state = MachineState.TERMINATED;
        }

        ManagedEntityStatus status = vm.getGuestHeartbeatStatus();
        if (status.compareTo(ManagedEntityStatus.green) == 0) {
            System.out.println("Found vm: " + vm.getName() + " Status: green");
        } else if (status.compareTo(ManagedEntityStatus.gray) == 0) {
            System.out.println("Found vm: " + vm.getName() + " Status: gray");
        } else {
            System.out.println("Found vm: " + vm.getName() + " Status: OTHER");
        }

        ServiceState serviceState = ServiceState.UNKNOWN;
        String cloudProvider = "VMWare";
        String region = "Datacenter region";
        // String machineSize = vm.getConfig().getHardware().toString();
        String machineSize = "fake machineSize";

        Machine machine = Machine.builder().id(id).machineState(state).membershipStatus(membershipStatus)
                .serviceState(serviceState).cloudProvider(cloudProvider).region(region).machineSize(machineSize)
                .build();

        return machine;
    }

}
