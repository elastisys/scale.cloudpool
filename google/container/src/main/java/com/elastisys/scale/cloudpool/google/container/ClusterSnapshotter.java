package com.elastisys.scale.cloudpool.google.container;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.errors.GceErrors;
import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;

/**
 * A {@link Callable} task that, when called, fetches a {@link ClusterSnapshot}
 * for the container cluster it has been configured to target.
 */
public class ClusterSnapshotter implements Callable<ClusterSnapshot> {

    /** API client. */
    private final ContainerClusterClient client;
    /** Metadata about the cluster to snapshot. */
    private final Cluster clusterMetadata;

    /**
     * Creates a {@link ClusterSnapshotter}.
     *
     * @param client
     *            API client.
     * @param clusterMetadata
     *            Metadata about the cluster to snapshot.
     */
    public ClusterSnapshotter(ContainerClusterClient client, Cluster clusterMetadata) {
        checkArgument(client != null, "client cannot be null");
        checkArgument(clusterMetadata != null, "clusterMetadata cannot be null");
        this.client = client;
        this.clusterMetadata = clusterMetadata;
    }

    @Override
    public ClusterSnapshot call() throws NotFoundException, CloudPoolException {
        DateTime now = UtcTime.now();
        List<NodePoolSnapshot> nodePools = collectNodePools(this.clusterMetadata);
        return new ClusterSnapshot(this.clusterMetadata, nodePools, now);
    }

    /**
     * Collects {@link NodePool}s (and their instance groups) for a given
     * {@link Cluster}.
     *
     * @param cluster
     * @return
     */
    private List<NodePoolSnapshot> collectNodePools(Cluster cluster) {
        if (cluster.getNodePools() == null) {
            return Collections.emptyList();
        }

        List<NodePoolSnapshot> nodePools = new ArrayList<>();
        for (NodePool nodePool : cluster.getNodePools()) {
            nodePools.add(collectNodePool(nodePool));
        }
        return nodePools;
    }

    /**
     * Collects metadata about all instance groups (and their individual
     * {@link Instance}s) that belong to a given container cluster
     * {@link NodePool}.
     *
     * @param nodePool
     * @return
     */
    private NodePoolSnapshot collectNodePool(NodePool nodePool) throws NotFoundException, CloudPoolException {
        if (nodePool.getInstanceGroupUrls() == null) {
            return new NodePoolSnapshot(nodePool, Collections.emptyList());
        }

        List<InstanceGroupSnapshot> instanceGroups = new ArrayList<>();
        for (String instanceGroupUrl : nodePool.getInstanceGroupUrls()) {
            try {
                InstanceGroupClient instanceGroupClient = this.client.instanceGroup(instanceGroupUrl);
                InstanceGroupManager instanceGroup = instanceGroupClient.getInstanceGroup();
                List<Instance> instances = collectGroupInstances(instanceGroupClient);
                instanceGroups.add(new InstanceGroupSnapshot(instanceGroup, instances));
            } catch (Exception e) {
                throw GceErrors.wrap(String.format("unable to get instance group %s in node pool %s: %s",
                        UrlUtils.basename(instanceGroupUrl), nodePool.getName(), e.getMessage()), e);
            }
        }
        return new NodePoolSnapshot(nodePool, instanceGroups);
    }

    /**
     * Collects metadata about all {@link Instance}s belonging to a certain
     * instance group.
     *
     * @param instanceGroupClient
     *            An API client that makes calls targeting a particular instance
     *            group.
     * @return
     */
    private List<Instance> collectGroupInstances(InstanceGroupClient instanceGroupClient)
            throws NotFoundException, CloudPoolException {
        List<Instance> instances = new ArrayList<>();

        List<ManagedInstance> members = instanceGroupClient.listInstances();
        for (ManagedInstance member : members) {
            try {
                Instance instance = this.client.computeClient().getInstance(member.getInstance());
                instances.add(instance);
            } catch (Exception e) {
                throw GceErrors.wrap(String.format("unable to get metadata for node pool instance %s: %s",
                        member.getInstance(), e.getMessage()), e);
            }
        }

        return instances;
    }

}
