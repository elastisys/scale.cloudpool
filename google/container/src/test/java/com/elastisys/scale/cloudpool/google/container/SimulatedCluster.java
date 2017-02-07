package com.elastisys.scale.cloudpool.google.container;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceGroupUrl;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceUrl;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.config.ContainerCluster;
import com.elastisys.scale.cloudpool.google.container.testutils.NodePoolUrl;
import com.elastisys.scale.cloudpool.google.container.testutils.ZoneUrl;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;

public class SimulatedCluster {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedCluster.class);

    public static final String MACHINE_TYPE = "n1-standard-8";
    public static final String MACHINE_STATUS = "RUNNING";
    public static final DateTime MACHINE_CREATION_TIME = UtcTime.now();

    private final String project;
    private final String zone;
    private final String ZONE_URL;
    private final String clusterName;

    private final Cluster clusterMetadata;
    private final List<NodePool> nodePoolsMetadata;
    private final List<InstanceGroupManager> instanceGroupsMetadata;
    private final Map<InstanceGroupManager, List<ManagedInstance>> managedInstances;
    private final List<Instance> instancesMetadata;

    /**
     * Creates a {@link SimulatedCluster} with a given set of node pools with a
     * given size. Each simulated node pool will consist of a single instance
     * group with the given number of instances.
     *
     * @param containerCluster
     *            Describes basic cluster properties.
     * @param nodePoolSizes
     *            Describes the layout of the cluster. Keys are node pool names
     *            and values are the sizes of each node pool.
     */
    public SimulatedCluster(ContainerCluster containerCluster, Map<String, Integer> nodePoolSizes) {
        this.project = containerCluster.getProject();
        this.zone = containerCluster.getZone();
        this.ZONE_URL = ZoneUrl.from(this.project, this.zone).getUrl();
        this.clusterName = containerCluster.getName();

        this.nodePoolsMetadata = new ArrayList<>();
        this.instanceGroupsMetadata = new ArrayList<>();
        this.managedInstances = new HashMap<>();
        this.instancesMetadata = new ArrayList<>();

        for (String nodePoolName : nodePoolSizes.keySet()) {
            int nodePoolSize = nodePoolSizes.get(nodePoolName);

            String instanceGroupName = nodePoolName + "-instanceGroup";

            String instanceGroupUrl = InstanceGroupUrl.managedZonal(this.project, this.zone, instanceGroupName)
                    .getUrl();
            InstanceGroupManager instanceGroupMetadata = new InstanceGroupManager().setName(instanceGroupName)
                    .setSelfLink(instanceGroupUrl).setTargetSize(nodePoolSize);
            this.instanceGroupsMetadata.add(instanceGroupMetadata);

            this.managedInstances.put(instanceGroupMetadata, new ArrayList<>());
            for (int i = 0; i < nodePoolSize; i++) {
                String instanceName = instanceGroupName + "-i" + i;
                String instanceUrl = InstanceUrl.from(this.project, this.zone, instanceName).getUrl();
                this.managedInstances.get(instanceGroupMetadata).add(new ManagedInstance().setInstance(instanceUrl));
                Instance instance = new Instance().setName(instanceName).setSelfLink(instanceUrl).setZone(this.ZONE_URL)
                        .setMachineType(MACHINE_TYPE).setStatus(MACHINE_STATUS)
                        .setCreationTimestamp(MACHINE_CREATION_TIME.toString());
                this.instancesMetadata.add(instance);
            }

            String nodePoolUrl = NodePoolUrl.from(this.project, this.zone, this.clusterName, nodePoolName).getUrl();
            NodePool nodePoolMetadata = new NodePool().setName(nodePoolName).setSelfLink(nodePoolUrl)
                    .setInstanceGroupUrls(asList(instanceGroupUrl));

            this.nodePoolsMetadata.add(nodePoolMetadata);
        }

        this.clusterMetadata = new Cluster().setNodePools(this.nodePoolsMetadata);
    }

    /**
     * Prepares a mocked {@link ComputeClient} with responses that will fake API
     * calls for the simulated cluster.
     *
     * @param mockedClient
     *            {@link ContainerClusterClient} mock that is to be prepared.
     *
     * @return
     */
    public void prepareMock(ContainerClusterClient mockedClient) {
        reset(mockedClient);
        ComputeClient mockedComputeClient = mock(ComputeClient.class);

        // prepare mocked api calls to respond with the simulated cluster

        when(mockedClient.getCluster(this.project, this.zone, this.clusterName)).thenReturn(this.clusterMetadata);
        when(mockedClient.computeClient()).thenReturn(mockedComputeClient);

        // set up a mocked instance group API client for each instance group
        for (InstanceGroupManager instanceGroup : this.instanceGroupsMetadata) {
            InstanceGroupClient mockedInstanceGroupClient = mock(InstanceGroupClient.class);
            when(mockedClient.instanceGroup(instanceGroup.getSelfLink())).thenReturn(mockedInstanceGroupClient);

            LOG.debug("adding instance group: '{}'", instanceGroup.getSelfLink());
            when(mockedInstanceGroupClient.getInstanceGroup()).thenReturn(instanceGroup);
            when(mockedInstanceGroupClient.listInstances()).thenReturn(this.managedInstances.get(instanceGroup));
            for (ManagedInstance managedInstance : this.managedInstances.get(instanceGroup)) {
                String instanceUrl = managedInstance.getInstance();
                when(mockedComputeClient.getInstance(instanceUrl)).thenReturn(findInstance(instanceUrl));
            }
        }
    }

    private Instance findInstance(String instanceUrl) {
        for (Instance instance : this.instancesMetadata) {
            if (instance.getSelfLink().equals(instanceUrl)) {
                return instance;
            }
        }
        throw new NotFoundException("count not find simulated instance with url: " + instanceUrl);
    }

    /**
     * Returns metadata about the {@link Cluster}.
     *
     * @return
     */
    public Cluster cluster() {
        return this.clusterMetadata;
    }

    /**
     * Returns metadata about all cluster {@link NodePool}s.
     *
     * @return
     */
    public List<NodePool> nodePools() {
        return this.nodePoolsMetadata;
    }

    /**
     * Returns metadata about all cluster {@link InstanceGroupManager}s.
     *
     * @return
     */
    public List<InstanceGroupManager> instanceGroups() {
        return this.instanceGroupsMetadata;
    }

    /**
     * Returns a {@link Map} that shows which {@link ManagedInstance}s belong to
     * which {@link InstanceGroupManager}.
     *
     * @return
     */
    public Map<InstanceGroupManager, List<ManagedInstance>> managedInstances() {
        return this.managedInstances;
    }

    /**
     * Returns metadata about all cluster {@link Instance}s.
     *
     * @return
     */
    public List<Instance> instances() {
        return this.instancesMetadata;
    }

    /**
     * Returns metadata about all {@link Instance}s that belong to a given
     * {@link InstanceGroupManager}.
     * 
     * @param instanceGroup
     * @return
     */
    public List<Instance> instances(InstanceGroupManager instanceGroup) {
        List<Instance> groupInstances = new ArrayList<>();

        List<ManagedInstance> groupMembers = this.managedInstances.get(instanceGroup);
        for (ManagedInstance groupMember : groupMembers) {
            groupInstances.add(findInstance(groupMember.getInstance()));
        }
        return groupInstances;
    }

}
