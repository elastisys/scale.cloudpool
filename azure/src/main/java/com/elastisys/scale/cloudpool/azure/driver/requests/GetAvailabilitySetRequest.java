package com.elastisys.scale.cloudpool.azure.driver.requests;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.CloudException;
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
     * @param availabilitySetName
     *            The name of the availability set to get.
     * @param resourceGroup
     *            The resource group under which the network is assumed to
     *            exist.
     */
    public GetAvailabilitySetRequest(AzureApiAccess apiAccess, String availabilitySetName, String resourceGroup) {
        super(apiAccess);
        this.availabilitySetName = availabilitySetName;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public AvailabilitySet doRequest(Azure api) throws NotFoundException, CloudException {
        try {
            LOG.debug("retrieving availability set {} ...", this.availabilitySetName);
            return api.availabilitySets().getByResourceGroup(this.resourceGroup, this.availabilitySetName);
        } catch (CloudException e) {
            if (e.body().code().equals("ResourceNotFound")) {
                throw new NotFoundException("no such availability set: " + this.availabilitySetName, e);
            }
            throw e;
        }
    }

}
