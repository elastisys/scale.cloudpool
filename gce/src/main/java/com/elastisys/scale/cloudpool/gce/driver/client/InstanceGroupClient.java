package com.elastisys.scale.cloudpool.gce.driver.client;

import java.util.List;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.gce.driver.client.impl.MultiZoneInstanceGroupClient;
import com.elastisys.scale.cloudpool.gce.driver.client.impl.SingleZoneInstanceGroupClient;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.Operation;

/**
 * Represents a management interface for a particular instance group.
 *
 * @see SingleZoneInstanceGroupClient
 * @see MultiZoneInstanceGroupClient
 */
public interface InstanceGroupClient {
    /**
     * Retrieves metadata about the instance group.
     *
     * @return
     * @throws GceException
     * @throws NotFoundException
     */
    InstanceGroupManager getInstanceGroup() throws GceException, NotFoundException;

    /**
     * Lists all instances in the instance group.
     *
     * @return
     * @throws GceException
     * @throws NotFoundException
     */
    List<ManagedInstance> listInstances() throws GceException, NotFoundException;

    /**
     * Sets a new target size for the instance group.
     * 
     * @param targetSize
     * @return
     * @throws GceException
     * @throws NotFoundException
     */
    Operation resize(int targetSize) throws GceException, NotFoundException;

    /**
     * Removes and terminates a set of instances from the instance group,
     * thereby decrementing the target size of he instance group.
     *
     * @param instanceUrls
     *            An list of instances to delete from the instance group. For
     *            example
     *            {@code https://www.googleapis.com/compute/v1/projects/<project>/zones/europe-west1-d/instances/webservers-d58p}.
     * @return
     *
     */
    Operation deleteInstances(List<String> instanceUrls) throws GceException, NotFoundException;

    /**
     * Removes a set of instances from the instance group (without terminating
     * the instances), thereby decrementing the target size of he instance
     * group.
     *
     * @param instanceUrls
     *            An list of instances to detach from the instance group. For
     *            example
     *            {@code https://www.googleapis.com/compute/v1/projects/<project>/zones/europe-west1-d/instances/webservers-d58p}.
     * @return
     */
    Operation abandonInstances(List<String> instanceUrls) throws GceException, NotFoundException;

}
