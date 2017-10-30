package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.google.common.collect.Sets;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * An Azure request that, when called, deletes a group of VMs together with
 * their network interfaces, any associated public IP addresses, and OS disks.
 */
public class PurgeVmsRequest extends AzureRequest<Void> {
    /**
     * The fully qualified ids of the VMs to delete. Typically of form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    private final List<String> vmIds;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param vmIds
     *            The fully qualified ids of the VMs to delete. Typically of
     *            form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Compute/virtualMachines/<name>}
     */
    public PurgeVmsRequest(AzureApiAccess apiAccess, List<String> vmIds) {
        super(apiAccess);
        this.vmIds = vmIds;
    }

    @Override
    public Void doRequest(Azure api) throws TerminateMachinesException, AzureException {
        Map<String, Throwable> failures = new HashMap<>();

        List<String> vmNames = this.vmIds.stream().map(UrlUtils::basename).collect(Collectors.toList());
        LOG.debug("purging vms {} ...", vmNames);

        List<VirtualMachine> victims = new ArrayList<>();
        for (String vmId : this.vmIds) {
            try {
                victims.add(new GetVmRequest(apiAccess(), vmId).call());
            } catch (Exception e) {
                failures.put(vmId, e);
            }
        }

        // need to delete a VM before its network interface
        new DeleteVmsRequest(apiAccess(), this.vmIds).call();

        for (VirtualMachine vm : victims) {
            if (vm.primaryNetworkInterfaceId() != null) {
                try {
                    new DeleteNetworkInterfaceRequest(apiAccess(), vm.primaryNetworkInterfaceId()).call();
                } catch (Exception e) {
                    failures.put(vm.id(), e);
                }
            }
        }

        for (VirtualMachine vm : victims) {
            try {
                new DeleteVmOsDiskRequest(apiAccess(), vm).call();
            } catch (Exception e) {
                failures.put(vm.id(), e);
            }
        }

        if (!failures.isEmpty()) {
            Set<String> terminatedVmIds = Sets.difference(Sets.newHashSet(this.vmIds), failures.keySet());
            throw new TerminateMachinesException(terminatedVmIds, failures);
        }

        LOG.debug("vms purged: {}", vmNames);
        return null;
    }

}
