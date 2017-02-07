package com.elastisys.scale.cloudpool.google.container.client;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;

/**
 * A client that provides access to a subset of Google Cloud APIs intended to
 * support management of a container cluster and the instance groups it consists
 * of.
 */
public interface ContainerClusterClient {
    /**
     * Configures the {@link ContainerClusterClient}.
     *
     * @param config
     *            Google Cloud Platform API credentials and settings.
     * @throws IllegalArgumentException
     * @throws CloudPoolException
     */
    void configure(CloudApiSettings config) throws IllegalArgumentException, CloudPoolException;

    /**
     * Retrieves metadata about a particular container {@link Cluster}.
     * <p/>
     * Note that the {@link InstanceGroupClient} and {@link ComputeClient} can
     * be used to retrieve additional metadata and manage the {@link NodePool}
     * instance groups and instances that make up the {@link Cluster}.
     *
     * @param project
     *            The project under which the instance was created.
     * @param zone
     *            The zone in which the {@link Cluster} is located. For example,
     *            {@code europe-west-1d}.
     * @param clusterName
     *            The short name of the {@link Cluster}. For example,
     *            {@code my-cluster}.
     * @return
     * @throws NotFoundException
     *             If the cluster could not be found.
     * @throws CloudPoolException
     */
    Cluster getCluster(String project, String zone, String clusterName) throws NotFoundException, CloudPoolException;

    /**
     * Returns a {@link InstanceGroupClient} which can be used to access a
     * particular instance group.
     *
     * @param instanceGroupUrl
     *            The URL of a single-zone instance group. For example
     *            {@code https://www.googleapis.com/compute/v1/projects/myproject/zones/europe-west1-d/instanceGroupManagers/my-instance-group}.
     * @return
     */
    InstanceGroupClient instanceGroup(String instanceGroupUrl) throws NotFoundException, CloudPoolException;

    /**
     * Returns a {@link ComputeClient} which can be used to access the Compute
     * service API (GCE).
     *
     * @return
     */
    ComputeClient computeClient();
}
