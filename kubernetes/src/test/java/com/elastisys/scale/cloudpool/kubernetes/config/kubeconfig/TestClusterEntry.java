package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link ClusterEntry} and its validation.
 *
 */
public class TestClusterEntry {
    /**
     * Exercise basic properties of {@link ClusterEntry}. A valid cluster should
     * pass validation and getters should return expected values.
     */
    @Test
    public void validUserEntry() {
        ClusterEntry clusterEntry = clusterEntry("foo",
                TestCluster.clusterCaCertPath("https://apiserver", TestCluster.CA_CERT_PATH));
        clusterEntry.validate();

        assertThat(clusterEntry.getName(), is("foo"));
        assertThat(clusterEntry.getCluster(),
                is(TestCluster.clusterCaCertPath("https://apiserver", TestCluster.CA_CERT_PATH)));
    }

    /**
     * One must specify a name for the cluster entry.
     */
    @Test
    public void missingName() {
        ClusterEntry clusterEntry = clusterEntry(null,
                TestCluster.clusterCaCertPath("https://apiserver", TestCluster.CA_CERT_PATH));
        try {
            clusterEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("clusters: cluster entry missing name field"));
        }
    }

    /**
     * One must specify a {@link Cluster} for the {@link ClusterEntry}.
     */
    @Test
    public void missingCluster() {
        ClusterEntry clusterEntry = clusterEntry("foo", null);
        try {
            clusterEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("clusters: cluster entry 'foo' missing cluster field"));
        }
    }

    /**
     * {@link ClusterEntry} validation should call through to {@link Cluster}'s
     * validate.
     */
    @Test
    public void validateCallThrough() {
        Cluster invalidCluster = TestCluster.clusterCaCertPath("https://apiserver", "bad/ca/path.pem");
        ClusterEntry clusterEntry = clusterEntry("foo", invalidCluster);
        try {
            clusterEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("clusters: cluster entry 'foo': cluster: failed to load ca cert"));
        }
    }

    public static ClusterEntry clusterEntry(String name, Cluster cluster) {
        ClusterEntry clusterEntry = new ClusterEntry();
        clusterEntry.name = name;
        clusterEntry.cluster = cluster;
        return clusterEntry;
    }
}
