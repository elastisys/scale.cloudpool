package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;

/**
 * An Azure request that, when executed, retrieves metadata about a particular
 * network interface card (NIC).
 */
public class GetNetworkInterfaceRequest extends AzureRequest<NetworkInterface> {

    /**
     * The fully qualified id of the network interface to delete. Typically of
     * form
     * {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Network/networkInterfaces/<name>}
     */
    private final String networkInterfaceId;

    /**
     * Creates a {@link GetNetworkInterfaceRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param networkInterfaceId
     *            The fully qualified id of the network interface to delete.
     *            Typically of form
     *            {@code /subscriptions/<subscription-id>/resourceGroups/<group>/providers/Microsoft.Network/networkInterfaces/<name>}
     */
    public GetNetworkInterfaceRequest(AzureApiAccess apiAccess, String networkInterfaceId) {
        super(apiAccess);
        this.networkInterfaceId = networkInterfaceId;
    }

    @Override
    public NetworkInterface doRequest(Azure api) throws NotFoundException, AzureException {
        NetworkInterface nic;
        try {
            nic = api.networkInterfaces().getById(this.networkInterfaceId);
        } catch (Exception e) {
            throw new AzureException("failed to get network interface: " + e.getMessage(), e);
        }

        return Optional.ofNullable(nic).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("network interface not found: %s", this.networkInterfaceId));
    }

}
