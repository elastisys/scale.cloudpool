package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * An Azure request that, when called, fetches available VM sizes in a given
 * region.
 *
 */
public class ListVmSizesRequest extends AzureRequest<List<VirtualMachineSize>> {

    /** The Azure region of interest. */
    private final Region region;

    /**
     * Creates a {@link ListVmSizesRequest} for a particular region.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     * @param regionName
     *            The Azure region of interest.
     */
    public ListVmSizesRequest(AzureApiAccess apiAccess, Region region) {
        super(apiAccess);
        this.region = region;
    }

    @Override
    public List<VirtualMachineSize> doRequest(Azure api) throws AzureException {
        PagedList<VirtualMachineSize> sizes;
        try {
            sizes = api.virtualMachines().sizes().listByRegion(this.region);
        } catch (Exception e) {
            throw new AzureException("failed to get vm sizes: " + e.getMessage(), e);
        }

        return new ArrayList<>(sizes);
    }

}
