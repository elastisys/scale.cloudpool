package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Network settings template for an Azure VM. These settings define how to set
 * up the VM's primary network interface.
 *
 * @see ScaleOutExtConfig
 */
public class NetworkSettings {
    /** Default value for {@link #assignPublicIp}. */
    static final Boolean DEFAULT_ASSIGN_PUBLIC_IP = false;

    /**
     * An existing virtual network that created VMs will be attached to (the
     * VM's primary network interface will receive a private IP address from
     * this network). Required.
     */
    private final String virtualNetwork;

    /**
     * The subnet within the virtual network, from which a (private) IP address
     * will be assigned to created VMs. Required.
     */
    private final String subnetName;

    /**
     * Set to <code>true</code> to assign a public IP address to created VMs.
     * May be {@code null}. Default: {@link #DEFAULT_ASSIGN_PUBLIC_IP}.
     */
    private final Boolean assignPublicIp;

    /**
     * A set of existing network security groups to associate with created VMs.
     * May be {@code null}, which means that no security groups get associated
     * with the primary network interface of created VMs. The default behavior
     * is to allow all inbound traffic from inside the VM's virtual network and
     * to allow all outbound traffic from a VM.
     */
    private final List<String> networkSecurityGroups;

    /**
     * Creates {@link NetworkSettings} for a VM.
     *
     * @param virtualNetwork
     *            An existing virtual network that created VMs will be attached
     *            to (the VM's primary network interface will receive a private
     *            IP address from this network). Required.
     * @param subnetName
     *            The subnet within the virtual network, from which a (private)
     *            IP address will be assigned to created VMs. Required.
     * @param assignPublicIp
     *            Set to <code>true</code> to assign a public IP address to
     *            created VMs. May be {@code null}. Default:
     *            {@link #DEFAULT_ASSIGN_PUBLIC_IP}.
     * @param networkSecurityGroups
     *            A set of existing network security groups to associate with
     *            created VMs. May be {@code null}, which means that no security
     *            groups get associated with the primary network interface of
     *            created VMs. The default behavior is to allow all inbound
     *            traffic from inside the VM's virtual network and to allow all
     *            outbound traffic from a VM.
     */
    public NetworkSettings(String virtualNetwork, String subnetName, Boolean assignPublicIp,
            List<String> networkSecurityGroups) {
        this.virtualNetwork = virtualNetwork;
        this.subnetName = subnetName;
        this.assignPublicIp = assignPublicIp;
        this.networkSecurityGroups = networkSecurityGroups;
    }

    /**
     * A virtual network that created VMs will be attached to (the VM's primary
     * network interface will receive a private IP address from this network).
     *
     * @return
     */
    public String getVirtualNetwork() {
        return this.virtualNetwork;
    }

    /**
     * The subnet within the virtual network, from which a (private) IP address
     * will be assigned to created VMs.
     *
     * @return
     */
    public String getSubnetName() {
        return this.subnetName;
    }

    /**
     * Set to <code>true</code> to assign a public IP address to created VMs.
     *
     * @return the assignPublicIp
     */
    public Boolean getAssignPublicIp() {
        return Optional.ofNullable(this.assignPublicIp).orElse(DEFAULT_ASSIGN_PUBLIC_IP);
    }

    /**
     * A set of existing network security groups to associate with created VMs.
     * May be {@code null}, which means that no security groups get associated
     * with the primary network interface of created VMs. The default behavior
     * is to allow all inbound traffic from inside the VM's virtual network and
     * to allow all outbound traffic from a VM.
     *
     * @return the networkSecurityGroups
     */
    public List<String> getNetworkSecurityGroups() {
        return Optional.ofNullable(this.networkSecurityGroups).orElse(Collections.emptyList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.virtualNetwork, this.subnetName, this.assignPublicIp, this.networkSecurityGroups);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NetworkSettings) {
            NetworkSettings that = (NetworkSettings) obj;
            return Objects.equals(this.virtualNetwork, that.virtualNetwork)
                    && Objects.equals(this.subnetName, that.subnetName)
                    && Objects.equals(this.assignPublicIp, that.assignPublicIp)
                    && Objects.equals(this.networkSecurityGroups, that.networkSecurityGroups);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.virtualNetwork != null, "network: no virtualNetwork given");
        checkArgument(this.subnetName != null, "network: no subnetName given");
    }
}
