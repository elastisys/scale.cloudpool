package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.Optional;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;

/**
 * An Azure request that, when executed, retrieves metadata about an
 * availability set.
 */
public class GetAvailabilitySetRequest extends AzureRequest<AvailabilitySet> {
    /** The name of the availability set to get. */
    private final String availabilitySetName;

    /** The resource group under which the network is assumed to exist. */
    private final String resourceGroup;

    /**
     * Creates a {@link GetAvailabilitySetRequest}.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param resourceGroup
     *            The resource group under which the network is assumed to
     *            exist.
     * @param availabilitySetName
     *            The name of the availability set to get.
     */
    public GetAvailabilitySetRequest(AzureApiAccess apiAccess, String resourceGroup, String availabilitySetName) {
        super(apiAccess);
        this.availabilitySetName = availabilitySetName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public AvailabilitySet doRequest(Azure api) throws NotFoundException, AzureException {
        AvailabilitySet as;
        try {
            LOG.debug("retrieving availability set {} ...", this.availabilitySetName);
            as = api.availabilitySets().getByResourceGroup(this.resourceGroup, this.availabilitySetName);
        } catch (Exception e) {
            throw new AzureException("failed to get availability set: " + e.getMessage(), e);
        }

        return Optional.ofNullable(as).orElseThrow(() -> notFoundError());
    }

    private NotFoundException notFoundError() {
        throw new NotFoundException(String.format("availability set not found: resourceGroup: %s, name: %s",
                this.resourceGroup, this.availabilitySetName));
    }
}
