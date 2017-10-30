package com.elastisys.scale.cloudpool.azure.driver.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.microsoft.azure.management.compute.PowerState.DEALLOCATED;
import static com.microsoft.azure.management.compute.PowerState.DEALLOCATING;
import static com.microsoft.azure.management.compute.PowerState.RUNNING;
import static com.microsoft.azure.management.compute.PowerState.STARTING;
import static com.microsoft.azure.management.compute.PowerState.STOPPED;
import static com.microsoft.azure.management.compute.PowerState.STOPPING;
import static com.microsoft.azure.management.compute.PowerState.UNKNOWN;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.microsoft.azure.management.batch.ProvisioningState;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * A {@link Function} that, given a {@link VirtualMachine}, determines its
 * {@link MachineState}.
 *
 * <p/>
 * For more details, refer to the <a href=
 * "https://docs.microsoft.com/en-us/rest/api/compute/virtualmachines/virtualmachines-state">Azure
 * VM state reference</a>.
 *
 */
public class VmToMachineState implements Function<VirtualMachine, MachineState> {
    static Logger LOG = LoggerFactory.getLogger(VmToMachineState.class);

    @Override
    public MachineState apply(VirtualMachine vm) {
        return extractMachineState(vm);
    }

    /**
     * Translates an Azure VM's power state and provisioning state to a
     * {@link MachineState}.
     *
     * @param vm
     * @return
     */
    private MachineState extractMachineState(VirtualMachine vm) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("vm states: provisioningState: {}, powerState: {}", vm.provisioningState(), vm.powerState());
        }

        // It appears to be the case that a VM goes through a sequence of
        // statuses, starting with only a provisioningState
        // (ProvisioningState/creating), and then (possibly)
        // reaching a power state (PowerState/starting). So powerState may be
        // null if provisioning is incomplete or has failed.
        // When a vm is in the process of being deleted, its provisioningState
        // is set to ProvisioningState/deleting and in some cases it does not
        // have a powerState, and in some cases it is in powerState/running.

        // So, the provisioningState should probably be the main indicator of
        // machine state. Only if in provisioningState/succeeded do we look
        // further at powerState (if available)

        ProvisioningState provisioningState = ProvisioningState.fromString(vm.provisioningState());
        if (provisioningState != null && provisioningState != ProvisioningState.SUCCEEDED) {
            return fromProvisioningState(provisioningState);
        }

        // provisioningState/succeeded: we need to look further at powerState
        // (the hypervisor's state)

        if (vm.powerState() != null) {
            return fromPowerState(vm.powerState());
        }

        // no powerState: go by provisioningState/succeeded
        return fromProvisioningState(provisioningState);

    }

    /**
     * Determines a {@link MachineState} solely by looking at a
     * {@code provisioningState}.
     *
     * @param provisioningState
     * @return
     * @throws IllegalArgumentException
     */
    private static MachineState fromProvisioningState(ProvisioningState provisioningState)
            throws IllegalArgumentException {
        checkArgument(provisioningState != null, "provisioningState was null");

        switch (provisioningState) {
        case INVALID:
            // set to pending to prevent cloud pool from launching
            // additional servers which might fail
            return MachineState.PENDING;
        case CREATING:
            return MachineState.PENDING;
        case DELETING:
            return MachineState.TERMINATING;
        case SUCCEEDED:
            return MachineState.RUNNING;
        case FAILED:
            // set to pending to prevent cloud pool from launching
            // additional servers which might fail
            return MachineState.PENDING;
        case CANCELLED:
            return MachineState.TERMINATED;
        }

        throw new IllegalArgumentException("unrecognized provisioningState: " + provisioningState.toString());
    }

    /**
     * Determines a {@link MachineState} solely by looking at
     * {@link PowerState}.
     *
     * @param powerState
     * @return
     * @throws IllegalArgumentException
     */
    private static MachineState fromPowerState(PowerState powerState) throws IllegalArgumentException {
        checkArgument(powerState != null, "powerState was null");

        if (powerState.equals(STARTING)) {
            return MachineState.PENDING;
        } else if (powerState.equals(RUNNING)) {
            return MachineState.RUNNING;
        } else if (powerState.equals(DEALLOCATING)) {
            return MachineState.TERMINATING;
        } else if (powerState.equals(DEALLOCATED)) {
            return MachineState.TERMINATED;
        } else if (powerState.equals(STOPPING)) {
            return MachineState.TERMINATING;
        } else if (powerState.equals(STOPPED)) {
            return MachineState.TERMINATED;
        } else if (powerState.equals(UNKNOWN)) {
            // powerState is unknown, go by provisioningState (which should be
            // succeeded) since we are even considering the powerState)
            return MachineState.RUNNING;
        }

        throw new IllegalArgumentException(
                String.format("failed to determine machineState from powerState: %s", powerState));
    }
}
