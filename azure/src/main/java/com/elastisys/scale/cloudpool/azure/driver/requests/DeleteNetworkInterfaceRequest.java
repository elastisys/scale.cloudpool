package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;

/**
 * An Azure request that, when called, deletes a network interface and any
 * public IP address associated with that network interface.
 */
public class DeleteNetworkInterfaceRequest extends AzureRequest<Void> {

    /**
     * The fully qualified id of the network interface to delete. Typically of
     * form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Network/networkInterfaces/<name>}
     */
    private final String networkInterfaceId;

    /**
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param networkInterfaceId
     *            The fully qualified id of the network interface to delete.
     *            Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Network/networkInterfaces/<name>}
     */
    public DeleteNetworkInterfaceRequest(AzureApiAccess apiAccess, String networkInterfaceId) {
        super(apiAccess);
        this.networkInterfaceId = networkInterfaceId;
    }

    @Override
    public Void doRequest(Azure api) throws NotFoundException, AzureException {
        NetworkInterface nic = new GetNetworkInterfaceRequest(apiAccess(), this.networkInterfaceId).call();

        PublicIPAddress publicIp = nic.primaryIPConfiguration().getPublicIPAddress();
        if (publicIp != null) {
            try {
                LOG.debug("disassociating public ip {} ({}) from network interface {}", publicIp.name(),
                        publicIp.ipAddress(), nic.name());
                nic.update().withoutPrimaryPublicIPAddress().apply();
                LOG.debug("deleting public ip {} ...", publicIp.id());
                api.publicIPAddresses().deleteById(publicIp.id());
            } catch (Exception e) {
                LOG.warn("failed to disassociate and/or delete public IP from network interface {}: {}",
                        this.networkInterfaceId, e.getMessage());
            }
        }

        LOG.debug("deleting network interface {} ...", nic.name());
        try {
            api.networkInterfaces().deleteById(nic.id());
        } catch (Exception e) {
            throw new AzureException(
                    String.format("failed to delete network interface {}: {}", nic.id(), e.getMessage()), e);
        }
        LOG.debug("network interface deleted.", nic.name());

        return null;
    }

}
