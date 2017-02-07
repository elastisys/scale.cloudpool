package com.elastisys.scale.cloudpool.google.container.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.SingleZoneInstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.StandardComputeClient;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;

public class GetCluster extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetCluster.class);

    /** The project under which the instance group was created. */
    private static String project = System.getenv("GOOGLE_PROJECT");
    /** TODO: the name of the zone where the instance is located. */
    private static String zone = "europe-west1-c";

    /** TODO: the name of the cluster. */
    private static String clusterName = "single-zone-cluster";

    public static void main(String[] args) throws Exception {

        Container containerApi = containerApiClient();
        Compute computeApi = computeApiClient();

        Cluster cluster = containerApi.projects().zones().clusters().get(project, zone, clusterName).execute();
        LOG.info("cluster: {}", cluster.toPrettyString());

        LOG.info("node count: {}", cluster.getCurrentNodeCount());
        LOG.info("initial node count: {}", cluster.getInitialNodeCount());
        LOG.info("locations: {}", cluster.getLocations());

        if (cluster.getNodePools() == null) {
            LOG.info("cluster doesn't have any node pools");
            return;
        }
        LOG.info("num node pools: {}", cluster.getNodePools().size());
        for (NodePool nodePool : cluster.getNodePools()) {
            LOG.info("  node pool: {}", nodePool.getSelfLink());
            LOG.info("    initialNodeCount: {}", nodePool.getInitialNodeCount());
            LOG.info("    metadata: {}", nodePool);

            if (nodePool.getInstanceGroupUrls() == null) {
                LOG.info("    node pool doesn't have any instance groups");
                continue;
            }

            for (String instanceGroupUrl : nodePool.getInstanceGroupUrls()) {
                LOG.info("    instance group URL: {}", instanceGroupUrl);
                SingleZoneInstanceGroupClient instanceGroupClient = new SingleZoneInstanceGroupClient(computeApi,
                        instanceGroupUrl);
                InstanceGroupManager instanceGroup = instanceGroupClient.getInstanceGroup();
                LOG.info("      instance group size: {}", instanceGroup.getTargetSize());
                for (ManagedInstance memberInstance : instanceGroupClient.listInstances()) {
                    StandardComputeClient computeClient = new StandardComputeClient();
                    computeClient.configure(new CloudApiSettings(serviceAccountKeyPath, null));
                    Instance instance = computeClient.getInstance(memberInstance.getInstance());
                    LOG.info("      instance group member: {}", instance.getSelfLink());
                }

            }
        }
    }
}