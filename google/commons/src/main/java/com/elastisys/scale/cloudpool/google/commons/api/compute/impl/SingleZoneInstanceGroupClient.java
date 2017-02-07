package com.elastisys.scale.cloudpool.google.commons.api.compute.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.errors.GceErrors;
import com.elastisys.scale.cloudpool.google.commons.errors.GceException;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceGroupUrl;
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

    /**
     * The URL of the instance group. For example,
     * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-c/instanceGroupManagers/my-instance-group}.
     */
    private final InstanceGroupUrl group;

    /**
     * Creates a {@link SingleZoneInstanceGroupClient}.
     *
     * @param apiClient
     *            GCE API client.
     * @param instanceGroupUrl
     *            The URL of the instance group. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-c/instanceGroupManagers/my-instance-group}.
     */
    public SingleZoneInstanceGroupClient(Compute apiClient, String instanceGroupUrl) {
        this.apiClient = apiClient;
        this.group = InstanceGroupUrl.parse(instanceGroupUrl);
        checkArgument(this.group.isZonal(), "instance group URL does not refer to a zonal instance group: %s",
                instanceGroupUrl);
        checkArgument(this.group.isManaged(), "instance group URL does not refer to a managed instance group: %s",
                instanceGroupUrl);
    }

    @Override
    public InstanceGroupManager getInstanceGroup() throws GceException {
        try {

            return this.apiClient.instanceGroupManagers()
                    .get(this.group.getProject(), this.group.getZone(), this.group.getName()).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("failed to get instance group %s in project %s and zone %s: %s",
                    this.group.getName(), this.group.getProject(), this.group.getZone(), e.getMessage()), e);
        }
    }

    @Override
    public List<ManagedInstance> listInstances() throws GceException {
        try {
            List<ManagedInstance> instances = this.apiClient.instanceGroupManagers()
                    .listManagedInstances(this.group.getProject(), this.group.getZone(), this.group.getName()).execute()
                    .getManagedInstances();
            return instances != null ? instances : Collections.emptyList();
        } catch (IOException e) {
            throw GceErrors
                    .wrap(String.format("failed to get members of instance group %s in project %s and zone %s: %s",
                            this.group.getName(), this.group.getProject(), this.group.getZone(), e.getMessage()), e);
        }
    }

    @Override
    public Operation resize(int targetSize) throws GceException {
        try {
            return this.apiClient.instanceGroupManagers()
                    .resize(this.group.getProject(), this.group.getZone(), this.group.getName(), targetSize).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format(
                    "failed to resize instance group %s in project %s and zone %s to size %d: %s", this.group.getName(),
                    this.group.getProject(), this.group.getZone(), targetSize, e.getMessage()), e);
        }
    }

    @Override
    public Operation deleteInstances(List<String> instanceUrls) {
        try {
            InstanceGroupManagersDeleteInstancesRequest deleteRequest = new InstanceGroupManagersDeleteInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.instanceGroupManagers()
                    .deleteInstances(this.group.getProject(), this.group.getZone(), this.group.getName(), deleteRequest)
                    .execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format(
                    "failed to delete instances %s from instance group %s in project %s and zone %s: %s", instanceUrls,
                    this.group.getName(), this.group.getProject(), this.group.getZone(), e.getMessage()), e);
        }
    }

    @Override
    public Operation abandonInstances(List<String> instanceUrls) throws GceException {
        try {
            InstanceGroupManagersAbandonInstancesRequest abandonRequest = new InstanceGroupManagersAbandonInstancesRequest()
                    .setInstances(instanceUrls);
            return this.apiClient.instanceGroupManagers().abandonInstances(this.group.getProject(),
                    this.group.getZone(), this.group.getName(), abandonRequest).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format(
                    "failed to abandon instances %s from instance group %s in project %s and zone %s: %s", instanceUrls,
                    this.group.getName(), this.group.getProject(), this.group.getZone(), e.getMessage()), e);
        }
    }

}
