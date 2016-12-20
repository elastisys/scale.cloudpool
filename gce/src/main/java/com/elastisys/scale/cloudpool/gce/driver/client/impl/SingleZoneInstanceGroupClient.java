package com.elastisys.scale.cloudpool.gce.driver.client.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.elastisys.scale.cloudpool.gce.driver.client.GceException;
import com.elastisys.scale.cloudpool.gce.driver.client.InstanceGroupClient;
import com.elastisys.scale.cloudpool.gce.util.GceErrors;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.InstanceGroupManagersAbandonInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupManagersDeleteInstancesRequest;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.Operation;

/**
 * * A management interface for a particular single-zone instance group.
 */
public class SingleZoneInstanceGroupClient implements InstanceGroupClient {

    /** GCE API client. */
    private final Compute apiClient;
    /** The project under which the instance group was created. */
    private final String project;
    /**
     * The name of the zone where the instance group is located. For example,
     * "europe-west1-d".
     */
    private final String zone;
    /** The name of the instance group. */
    private final String instanceGroup;

    /**
     * Creates a {@link SingleZoneInstanceGroupClient}.
     *
     * @param apiClient
     *            GCE API client.
     * @param project
     *            The project under which the instance group was created.
     * @param zone
     *            The name of the zone where the instance group is located. For
     *            example, "europe-west1-d".
     * @param instanceGroup
     *            The name of the instance group.
     */
    public SingleZoneInstanceGroupClient(Compute apiClient, String project, String zone, String instanceGroup) {
        this.apiClient = apiClient;
        this.project = project;
        this.zone = zone;
        this.instanceGroup = instanceGroup;
    }

    @Override
    public InstanceGroupManager getInstanceGroup() throws GceException {
        try {
            return this.apiClient.instanceGroupManagers().get(this.project, this.zone, this.instanceGroup).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("failed to get instance group %s in project %s and zone %s: %s",
                    this.instanceGroup, this.project, this.zone, e.getMessage()), e);
        }
    }

    @Override
    public List<ManagedInstance> listInstances() throws GceException {
        try {
            List<ManagedInstance> instances = this.apiClient.instanceGroupManagers()
                    .listManagedInstances(this.project, this.zone, this.instanceGroup).execute().getManagedInstances();
            return instances != null ? instances : Collections.emptyList();
        } catch (IOException e) {
            throw GceErrors
                    .wrap(String.format("failed to get members of instance group %s in project %s and zone %s: %s",
                            this.instanceGroup, this.project, this.zone, e.getMessage()), e);
        }
    }

    @Override
    public Operation resize(int targetSize) throws GceException {
        try {
            return this.apiClient.instanceGroupManagers()
                    .resize(this.project, this.zone, this.instanceGroup, targetSize).execute();
        } catch (IOException e) {
            throw GceErrors
                    .wrap(String.format("failed to resize instance group %s in project %s and zone %s to size %d: %s",
                            this.instanceGroup, this.project, this.zone, targetSize, e.getMessage()), e);
        }
    }

    @Override
    public Operation deleteInstances(List<String> instanceUrls) {
        try {
            InstanceGroupManagersDeleteInstancesRequest deleteRequest = new InstanceGroupManagersDeleteInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.instanceGroupManagers()
                    .deleteInstances(this.project, this.zone, this.instanceGroup, deleteRequest).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(
                    String.format("failed to delete instances %s from instance group %s in project %s and zone %s: %s",
                            instanceUrls, this.instanceGroup, this.project, this.zone, e.getMessage()),
                    e);
        }
    }

    @Override
    public Operation abandonInstances(List<String> instanceUrls) throws GceException {
        try {
            InstanceGroupManagersAbandonInstancesRequest abandonRequest = new InstanceGroupManagersAbandonInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.instanceGroupManagers()
                    .abandonInstances(this.project, this.zone, this.instanceGroup, abandonRequest).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(
                    String.format("failed to abandon instances %s from instance group %s in project %s and zone %s: %s",
                            instanceUrls, this.instanceGroup, this.project, this.zone, e.getMessage()),
                    e);
        }
    }

}
