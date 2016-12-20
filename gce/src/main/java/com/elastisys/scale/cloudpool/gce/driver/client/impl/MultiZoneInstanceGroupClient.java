package com.elastisys.scale.cloudpool.gce.driver.client.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.elastisys.scale.cloudpool.gce.driver.client.GceException;
import com.elastisys.scale.cloudpool.gce.driver.client.InstanceGroupClient;
import com.elastisys.scale.cloudpool.gce.util.GceErrors;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.RegionInstanceGroupManagersAbandonInstancesRequest;
import com.google.api.services.compute.model.RegionInstanceGroupManagersDeleteInstancesRequest;

/**
 * A management interface for a particular multi-zone instance group.
 */
public class MultiZoneInstanceGroupClient implements InstanceGroupClient {

    /** GCE API client. */
    private final Compute apiClient;
    /** The project under which the instance group was created. */
    private final String project;
    /**
     * The name of the region where the instance group is located. For example,
     * "europe-west1".
     */
    private final String region;
    /** The name of the instance group. */
    private final String instanceGroup;

    /**
     *
     * Creates a {@link MultiZoneInstanceGroupClient}.
     *
     * @param apiClient
     *            GCE API client.
     * @param project
     *            The project under which the instance group was created.
     * @param region
     *            The name of the region where the instance group is located.
     *            For example, "europe-west1".
     * @param instanceGroup
     *            The name of the instance group.
     */
    public MultiZoneInstanceGroupClient(Compute apiClient, String project, String region, String instanceGroup) {
        this.apiClient = apiClient;
        this.project = project;
        this.region = region;
        this.instanceGroup = instanceGroup;
    }

    @Override
    public InstanceGroupManager getInstanceGroup() throws GceException {
        try {
            return this.apiClient.regionInstanceGroupManagers().get(this.project, this.region, this.instanceGroup)
                    .execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("failed to get instance group %s in project %s and region %s: %s",
                    this.instanceGroup, this.project, this.region, e.getMessage()), e);
        }
    }

    @Override
    public List<ManagedInstance> listInstances() throws GceException {
        try {
            List<ManagedInstance> instances = this.apiClient.regionInstanceGroupManagers()
                    .listManagedInstances(this.project, this.region, this.instanceGroup).execute()
                    .getManagedInstances();
            return instances != null ? instances : Collections.emptyList();
        } catch (IOException e) {
            throw GceErrors
                    .wrap(String.format("failed to get members of instance group %s in project %s and region %s: %s",
                            this.instanceGroup, this.project, this.region, e.getMessage()), e);
        }
    }

    @Override
    public Operation resize(int targetSize) throws GceException {
        try {
            return this.apiClient.regionInstanceGroupManagers()
                    .resize(this.project, this.region, this.instanceGroup, targetSize).execute();
        } catch (IOException e) {
            throw GceErrors
                    .wrap(String.format("failed to resize instance group %s in project %s and region %s to size %d: %s",
                            this.instanceGroup, this.project, this.region, targetSize, e.getMessage()), e);
        }
    }

    @Override
    public Operation deleteInstances(List<String> instanceUrls) {
        try {
            RegionInstanceGroupManagersDeleteInstancesRequest deleteRequest = new RegionInstanceGroupManagersDeleteInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.regionInstanceGroupManagers()
                    .deleteInstances(this.project, this.region, this.instanceGroup, deleteRequest).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format(
                    "failed to delete instances %s from instance group %s in project %s and region %s: %s",
                    instanceUrls, this.instanceGroup, this.project, this.region, e.getMessage()), e);
        }
    }

    @Override
    public Operation abandonInstances(List<String> instanceUrls) throws GceException {
        try {
            RegionInstanceGroupManagersAbandonInstancesRequest abandonRequest = new RegionInstanceGroupManagersAbandonInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.regionInstanceGroupManagers()
                    .abandonInstances(this.project, this.region, this.instanceGroup, abandonRequest).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format(
                    "failed to abandon instances %s from instance group %s in project %s and region %s: %s",
                    instanceUrls, this.instanceGroup, this.project, this.region, e.getMessage()), e);
        }
    }

}
