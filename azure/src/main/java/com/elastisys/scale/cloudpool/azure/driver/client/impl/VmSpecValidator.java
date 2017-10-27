package com.elastisys.scale.cloudpool.azure.driver.client.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetAvailabilitySetRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetNetworkSecurityGroupsRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetStorageAccountRequest;
import com.elastisys.scale.cloudpool.azure.driver.requests.GetVmSizesRequest;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * Validates {@link VmSpec}s prior to launch to make sure referenced Azure
 * assets exist.
 */
public class VmSpecValidator {

    private static final Logger LOG = LoggerFactory.getLogger(VmSpecValidator.class);

    /** Azure API access credentials and settings. */
    private final AzureApiAccess apiAccess;
    /**
     * Azure region where referenced assets are assume to be located. For
     * example, "northeurope".
     */
    private final Region region;
    /** Azure resource group where referenced assets are assumed to exist. */
    private final String resourceGroup;

    /**
     * Creates a {@link VmSpecValidator}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param region
     *            Azure region where referenced assets are assume to be located.
     *            For example, "northeurope".
     * @param resourceGroup
     *            Azure resource group where referenced assets are assumed to
     *            exist.
     */
    public VmSpecValidator(AzureApiAccess apiAccess, Region region, String resourceGroup) {
        this.apiAccess = apiAccess;
        this.region = region;
        this.resourceGroup = resourceGroup;
    }

    /**
     * Tries to validate that all Azure assets referenced from a {@link VmSpec}
     * actually exist in the given resource group and region. Throws an
     * {@link IllegalArgumentException} on failure to validate the existence of
     * some asset.
     *
     * @param vmSpec
     * @throws IllegalArgumentException
     */
    public void validateVmSpec(VmSpec vmSpec) throws IllegalArgumentException {
        validateVmSize(vmSpec.getVmSize());

        validateStorageAccount(vmSpec.getStorageAccountName());

        Network network = validateVirtualNetwork(vmSpec.getNetwork().getVirtualNetwork());
        validateSubnet(network, vmSpec.getNetwork().getSubnetName());
        validateSecurityGroups(vmSpec.getNetwork().getNetworkSecurityGroups());
        validateAvailabilitySet(vmSpec.getAvailabilitySet());
    }

    private void validateAvailabilitySet(Optional<String> availabilitySet) {
        if (!availabilitySet.isPresent()) {
            return;
        }

        LOG.debug("validating availability set {} ...", availabilitySet.get());
        try {
            new GetAvailabilitySetRequest(this.apiAccess, availabilitySet.get(), this.resourceGroup).call();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(String.format("availability set not found in resource group %s: %s",
                    this.resourceGroup, availabilitySet.get()));
        }
    }

    private void validateSecurityGroups(List<String> referencedSecurityGroups) {
        LOG.debug("validating security groups ...");
        List<NetworkSecurityGroup> actualSecurityGroups = new GetNetworkSecurityGroupsRequest(this.apiAccess,
                this.resourceGroup).call();
        List<String> actualNsgNames = actualSecurityGroups.stream().map(NetworkSecurityGroup::name)
                .collect(Collectors.toList());
        for (String referencedNsg : referencedSecurityGroups) {
            if (!actualNsgNames.contains(referencedNsg)) {
                throw new IllegalArgumentException(
                        String.format("no such network security group found in resource group %s: %s",
                                this.resourceGroup, referencedNsg));
            }
        }
    }

    private void validateSubnet(Network network, String subnetName) {
        LOG.debug("validating subnet ...");
        if (!network.subnets().containsKey(subnetName)) {
            throw new IllegalArgumentException(
                    String.format("subnet not not found in network %s: %s", network.name(), subnetName));
        }
    }

    private Network validateVirtualNetwork(String virtualNetwork) {
        LOG.debug("validating network ...");
        try {
            return new GetNetworkRequest(this.apiAccess, virtualNetwork, this.resourceGroup).call();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(
                    String.format("network not found in resource group %s: %s", this.resourceGroup, virtualNetwork));
        }
    }

    private void validateStorageAccount(String storageAccountName) throws IllegalArgumentException {
        LOG.debug("validating storage account ...");
        try {
            new GetStorageAccountRequest(this.apiAccess, storageAccountName, this.resourceGroup).call();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(String.format("storage account not found in resource group %s: %s",
                    this.resourceGroup, storageAccountName));
        }
    }

    private void validateVmSize(String vmSize) throws IllegalArgumentException {
        LOG.debug("validating vm size ...");
        List<VirtualMachineSize> vmSizes = new GetVmSizesRequest(this.apiAccess, this.region).call();
        for (VirtualMachineSize candidateSize : vmSizes) {
            if (candidateSize.name().equals(vmSize)) {
                return;
            }
        }
        throw new IllegalArgumentException(String.format("vm size not found in region %s: %s", this.region, vmSize));
    }
}
