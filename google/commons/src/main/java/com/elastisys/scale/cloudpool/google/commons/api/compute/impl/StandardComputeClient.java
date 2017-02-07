package com.elastisys.scale.cloudpool.google.commons.api.compute.impl;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.io.IOException;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.clients.ApiClients;
import com.elastisys.scale.cloudpool.google.commons.errors.GceErrors;
import com.elastisys.scale.cloudpool.google.commons.errors.GceException;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceTemplateUrl;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;

/**
 * A {@link ComputeClient} that accesses the Google Compute Engine API client
 * for a given service account.
 */
public class StandardComputeClient implements ComputeClient {

    /** Authenticated Google Compute Engine API client. */
    private Compute apiClient;

    @Override
    public void configure(CloudApiSettings config) throws IllegalArgumentException, CloudPoolDriverException {
        config.validate();

        GoogleCredential serviceAccountKey = config.getApiCredential();
        this.apiClient = ApiClients.compute(serviceAccountKey);
    }

    @Override
    public Instance getInstance(String instanceUrl) throws NotFoundException, GceException {
        ensureConfigured();

        InstanceUrl url = new InstanceUrl(instanceUrl);
        try {
            return this.apiClient.instances().get(url.getProject(), url.getZone(), url.getName()).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to get instance %s in project %s and zone %s: %s", url.getName(),
                    url.getProject(), url.getZone(), e.getMessage()), e);
        }
    }

    @Override
    public InstanceTemplate getInstanceTemplate(String instanceTemplateUrl) throws NotFoundException, GceException {
        ensureConfigured();

        InstanceTemplateUrl template = new InstanceTemplateUrl(instanceTemplateUrl);
        try {
            return this.apiClient.instanceTemplates().get(template.getProject(), template.getName()).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to get instance template %s in project %s: %s",
                    template.getName(), template.getProject(), e.getMessage()), e);
        }
    }

    @Override
    public Operation setMetadata(String instanceUrl, Metadata metadata) throws GceException {
        ensureConfigured();

        InstanceUrl url = new InstanceUrl(instanceUrl);
        try {
            return this.apiClient.instances().setMetadata(url.getProject(), url.getZone(), url.getName(), metadata)
                    .execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to set metadata for instance %s in project %s: %s",
                    url.getName(), url.getProject(), e.getMessage()), e);
        }
    }

    @Override
    public MultiZoneInstanceGroupClient multiZoneInstanceGroup(String instanceGroupUrl) {
        ensureConfigured();
        return new MultiZoneInstanceGroupClient(this.apiClient, instanceGroupUrl);
    }

    @Override
    public SingleZoneInstanceGroupClient singleZoneInstanceGroup(String instanceGroupUrl) {
        ensureConfigured();
        return new SingleZoneInstanceGroupClient(this.apiClient, instanceGroupUrl);
    }

    void ensureConfigured() {
        checkArgument(isConfigured(), "attempt to use GCE client before being configured");
    }

    boolean isConfigured() {
        return this.apiClient != null;
    }

}
