package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;

/**
 * An Azure request that, when called, deletes a network interface and any
 * public IP address associated with that network interface.
 *
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
    public Void doRequest(Azure api) throws RuntimeException {
        NetworkInterface nic = api.networkInterfaces().getById(this.networkInterfaceId);
        if (nic == null) {
            LOG.debug("no network interface found with id {}", this.networkInterfaceId);
            return null;
        }

        LOG.debug("found network interface {}", nic.id());
        PublicIPAddress publicIp = nic.primaryIPConfiguration().getPublicIPAddress();
        if (publicIp != null) {
            LOG.debug("disassociating public ip {} ({}) from network interface {}", publicIp.name(),
                    publicIp.ipAddress(), nic.name());
            nic.update().withoutPrimaryPublicIPAddress().apply();
            LOG.debug("deleting public ip {} ...", publicIp.id());
            api.publicIPAddresses().deleteById(publicIp.id());
        }

        LOG.debug("deleting network interface {} ...", nic.name());
        api.networkInterfaces().deleteById(nic.id());
        LOG.debug("network interface deleted.", nic.name());

        return null;
    }

}
