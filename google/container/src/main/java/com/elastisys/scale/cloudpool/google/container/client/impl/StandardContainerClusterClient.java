package com.elastisys.scale.cloudpool.google.container.client.impl;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.StandardComputeClient;
import com.elastisys.scale.cloudpool.google.commons.clients.ApiClients;
import com.elastisys.scale.cloudpool.google.commons.errors.GceErrors;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;

public class StandardContainerClusterClient implements ContainerClusterClient {

    /** Google Container Service API client. */
    private Container api;
    /** Google Compute Service API client. */
    private ComputeClient computeClient;

    @Override
    public void configure(CloudApiSettings config) throws IllegalArgumentException, CloudPoolException {
        config.validate();
        this.api = ApiClients.container(config.getApiCredential());
        this.computeClient = new StandardComputeClient();
        this.computeClient.configure(config);
    }

    @Override
    public Cluster getCluster(String project, String zone, String clusterName)
            throws NotFoundException, CloudPoolException {
        ensureConfigured();
        try {
            return this.api.projects().zones().clusters().get(project, zone, clusterName).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to get cluster %s in project %s and zone %s: %s", clusterName,
                    project, zone, e.getMessage()), e);
        }
    }

    @Override
    public InstanceGroupClient instanceGroup(String instanceGroupUrl) {
        ensureConfigured();
        return this.computeClient.singleZoneInstanceGroup(instanceGroupUrl);
    }

    @Override
    public ComputeClient computeClient() {
        ensureConfigured();
        return this.computeClient;
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.api != null, "cannot use unconfigured client");
    }
}
