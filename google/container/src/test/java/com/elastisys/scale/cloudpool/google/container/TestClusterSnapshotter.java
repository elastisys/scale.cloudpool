package com.elastisys.scale.cloudpool.google.container;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.elastisys.scale.cloudpool.google.container.config.ContainerCluster;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises {@link ClusterSnapshotter}.
 */
public class TestClusterSnapshotter {
    private static final Logger LOG = LoggerFactory.getLogger(TestClusterSnapshotter.class);

    private static final String PROJECT = "my-project";
    private static final String ZONE1 = "europe-west1-b";
    private static final String CLUSTER_NAME = "container-cluster";
    private static final String NODEPOOL_NAME = "nodepool1";

    private ContainerClusterClient mockedClient = mock(ContainerClusterClient.class);

    @Before
    public void beforeTestMethods() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
    }

    /**
     * {@link ClusterSnapshotter} requires a {@link ContainerClusterClient}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullClient() {
        ContainerClusterClient nullClient = null;
        Cluster clusterMetadata = new Cluster();
        new ClusterSnapshotter(nullClient, clusterMetadata);
    }

    /**
     * {@link ClusterSnapshotter} requires a {@link Cluster}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullClusterMetadata() {
        Cluster nullClusterMetadata = null;
        new ClusterSnapshotter(this.mockedClient, nullClusterMetadata);
    }

    /**
     * {@link Cluster} can lack node pools (for example, while it is being
     * created). In such cases, the snapshotter should just return an empty
     * {@link ClusterSnapshot}.
     */
    @Test
    public void snapshotClusterWithNullNodePools() {
        Cluster emptyCluster = new Cluster().setNodePools(null);

        ClusterSnapshot snapshot = new ClusterSnapshotter(this.mockedClient, emptyCluster).call();

        assertThat(snapshot, is(new ClusterSnapshot(emptyCluster, null, UtcTime.now())));

        // verify that no API calls were needed
        verifyZeroInteractions(this.mockedClient);
    }

    /**
     * {@link Cluster} can lack node pools (for example, if the user has
     * temporarily removed them from the cluster). In such cases, the
     * snapshotter should just return an empty {@link ClusterSnapshot}.
     */
    @Test
    public void snapshotClusterWithEmptyNodePools() {
        Cluster emptyCluster = new Cluster().setNodePools(Collections.emptyList());

        ClusterSnapshot snapshot = new ClusterSnapshotter(this.mockedClient, emptyCluster).call();

        assertThat(snapshot, is(new ClusterSnapshot(emptyCluster, null, UtcTime.now())));

        // verify that no API calls were needed
        verifyZeroInteractions(this.mockedClient);
    }

    /**
     * Verify that expected API calls are made when a cluster snapshot is taken,
     * which means that cluster metadata is fetched as well as node pools,
     * instance groups and instances.
     */
    @Test
    public void snapshotNonEmptyCluster() {
        // set up a simulated container cluster
        SimulatedCluster fakeCluster = new SimulatedCluster(new ContainerCluster(CLUSTER_NAME, PROJECT, ZONE1),
                ImmutableMap.of(NODEPOOL_NAME, 1));

        // prepare mocked api calls to respond with the simulated cluster
        fakeCluster.prepareMock(this.mockedClient);

        // take cluster snapshot
        ClusterSnapshot snapshot = new ClusterSnapshotter(this.mockedClient, fakeCluster.cluster()).call();

        // set up expectations
        InstanceGroupManager instanceGroupMetadata = fakeCluster.instanceGroups().get(0);
        InstanceGroupSnapshot expectedInstanceGroupSnapshot = new InstanceGroupSnapshot(instanceGroupMetadata,
                fakeCluster.instances(instanceGroupMetadata));
        NodePool nodePoolMetadata = fakeCluster.nodePools().get(0);
        NodePoolSnapshot expectedNodePoolSnapshot = new NodePoolSnapshot(nodePoolMetadata,
                asList(expectedInstanceGroupSnapshot));
        ClusterSnapshot expectedSnapshot = new ClusterSnapshot(fakeCluster.cluster(), asList(expectedNodePoolSnapshot));

        // verify the taken snapshot
        LOG.debug("snapshot: {}", snapshot);
        assertThat(snapshot, is(expectedSnapshot));

        // verify that expected API calls were made
        verify(this.mockedClient).instanceGroup(instanceGroupMetadata.getSelfLink());
        // should call the instance group to get metadata
        verify(this.mockedClient.instanceGroup(instanceGroupMetadata.getSelfLink())).getInstanceGroup();
        // should call the instance group list instances
        verify(this.mockedClient.instanceGroup(instanceGroupMetadata.getSelfLink())).listInstances();
        verify(this.mockedClient).computeClient();
        // should fetch metadata about each instance
        verify(this.mockedClient.computeClient()).getInstance(fakeCluster.instances().get(0).getSelfLink());
    }
}
