package com.elastisys.scale.cloudpool.kubernetes.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;

/**
 * Exercises the {@link PodPoolConfig}.
 */
public class TestPodPoolConfig {

    /** Sample replicationController name. */
    private static final String REPLICATION_CONTROLLER = "nginx-rc";
    /** Sample replicaSet name. */
    private static final String REPLICA_SET = "nginx-rs";
    /** Sample deployment name. */
    private static final String DEPLOYMENT = "nginx-deployment";
    /** Sample Kubernetes namespace. */
    private static final String NAMESPACE = "my-ns";

    private static final String nullReplicationController = null;
    private static final String nullReplicaSet = null;
    private static final String nullDeployment = null;

    /**
     * It should be possible to configure a {@link PodPool} to manage a
     * Kubernetes replication controller.
     */
    @Test
    public void configureToManageReplicationController() {
        PodPoolConfig config = new PodPoolConfig(NAMESPACE, REPLICATION_CONTROLLER, nullReplicaSet, nullDeployment);
        config.validate();
        assertThat(config.getNamespace(), is(NAMESPACE));
        assertThat(config.getReplicationController(), is(REPLICATION_CONTROLLER));
        assertThat(config.getReplicaSet(), is(nullValue()));
        assertThat(config.getDeployment(), is(nullValue()));
        assertThat(config.getApiObject(), is("rc/" + REPLICATION_CONTROLLER));
    }

    /**
     * It should be possible to configure a {@link PodPool} to manage a
     * Kubernetes replica set.
     */
    @Test
    public void configureToManageReplicaSet() {
        PodPoolConfig config = new PodPoolConfig(NAMESPACE, nullReplicationController, REPLICA_SET, nullDeployment);
        config.validate();
        assertThat(config.getNamespace(), is(NAMESPACE));
        assertThat(config.getReplicationController(), is(nullValue()));
        assertThat(config.getReplicaSet(), is(REPLICA_SET));
        assertThat(config.getDeployment(), is(nullValue()));
        assertThat(config.getApiObject(), is("rs/" + REPLICA_SET));
    }

    /**
     * It should be possible to configure a {@link PodPool} to manage a
     * Kubernetes deployment.
     */
    @Test
    public void configureToManageDeployment() {
        PodPoolConfig config = new PodPoolConfig(NAMESPACE, nullReplicationController, nullReplicaSet, DEPLOYMENT);
        config.validate();
        assertThat(config.getNamespace(), is(NAMESPACE));
        assertThat(config.getReplicationController(), is(nullValue()));
        assertThat(config.getReplicaSet(), is(nullValue()));
        assertThat(config.getDeployment(), is(DEPLOYMENT));
        assertThat(config.getApiObject(), is("deployment/" + DEPLOYMENT));
    }

    /**
     * If no namespace is given, a default value of {@code default} is used as
     * namespace.
     */
    @Test
    public void defaultNamespace() {
        PodPoolConfig config = new PodPoolConfig(null, nullReplicationController, nullReplicaSet, DEPLOYMENT);
        config.validate();
        assertThat(config.getNamespace(), is(PodPoolConfig.DEFAULT_NAMESPACE));
    }

    /**
     * One must specify one of {@code replicationController}, {@code replicaSet}
     * and {@code deployment}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void mustSpecifyAnApiConstruct() {
        new PodPoolConfig(NAMESPACE, nullReplicationController, nullReplicaSet, nullDeployment).validate();
    }

    /**
     * Exactly one of {@code replicationController}, {@code replicaSet} and
     * {@code deployment} must be specified.
     */
    @Test
    public void mustSpecifyOnlyOneApiConstruct() {
        try {
            new PodPoolConfig(NAMESPACE, REPLICATION_CONTROLLER, REPLICA_SET, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new PodPoolConfig(NAMESPACE, REPLICATION_CONTROLLER, null, DEPLOYMENT).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new PodPoolConfig(NAMESPACE, null, REPLICA_SET, DEPLOYMENT).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new PodPoolConfig(NAMESPACE, REPLICATION_CONTROLLER, REPLICA_SET, DEPLOYMENT).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
