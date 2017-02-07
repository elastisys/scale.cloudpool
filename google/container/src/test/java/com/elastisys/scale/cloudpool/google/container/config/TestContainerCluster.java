package com.elastisys.scale.cloudpool.google.container.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link ContainerCluster} configuration.
 */
public class TestContainerCluster {

    private static final String ZONE = "europe-west-1b";
    private static final String PROJECT_NAME = "my-project";
    private static final String CLUSTER_NAME = "my-cluster";

    /**
     * Should be possible to give explicit values for all fields.
     */
    @Test
    public void complete() {
        ContainerCluster config = new ContainerCluster(CLUSTER_NAME, PROJECT_NAME, ZONE);
        config.validate();

        assertThat(config.getName(), is(CLUSTER_NAME));
        assertThat(config.getProject(), is(PROJECT_NAME));
        assertThat(config.getZone(), is(ZONE));
    }

    /**
     * Cluster name is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingClusterName() {
        new ContainerCluster(null, PROJECT_NAME, ZONE).validate();
    }

    /**
     * Google Cloud project is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingProject() {
        new ContainerCluster(CLUSTER_NAME, null, ZONE).validate();
    }

    /**
     * Google Cloud zone is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingZone() {
        new ContainerCluster(CLUSTER_NAME, PROJECT_NAME, null).validate();
    }

}
