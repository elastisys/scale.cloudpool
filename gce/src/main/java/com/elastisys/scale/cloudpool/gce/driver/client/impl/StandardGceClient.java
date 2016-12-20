package com.elastisys.scale.cloudpool.gce.driver.client.impl;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Collections;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.gce.driver.client.GceClient;
import com.elastisys.scale.cloudpool.gce.driver.client.GceException;
import com.elastisys.scale.cloudpool.gce.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.gce.util.GceErrors;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;

/**
 * A {@link GceClient} that accesses the Google Compute Engine API client for a
 * given service account.
 */
public class StandardGceClient implements GceClient {

    private static final String GCE_AUTH_SCOPE = "https://www.googleapis.com/auth/compute";
    /** Authenticated Google Compute Engine API client. */
    private Compute apiClient;

    @Override
    public void configure(CloudApiSettings config) throws IllegalArgumentException, CloudPoolDriverException {
        config.validate();

        GoogleCredential serviceAccountKey = config.getApiCredential();
        this.apiClient = acquireAuthenticatedApiClient(serviceAccountKey);
    }

    @Override
    public Instance getInstance(String project, String zone, String instanceName)
            throws NotFoundException, GceException {
        ensureConfigured();

        try {
            return this.apiClient.instances().get(project, zone, instanceName).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to get instance %s in project %s and zone %s: %s", instanceName,
                    project, zone, e.getMessage()), e);
        }
    }

    @Override
    public InstanceTemplate getInstanceTemplate(String project, String instanceTemplateName)
            throws NotFoundException, GceException {

        try {
            return this.apiClient.instanceTemplates().get(project, instanceTemplateName).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to get instance template %s in project %s: %s",
                    instanceTemplateName, project, e.getMessage()), e);
        }
    }

    @Override
    public Operation setMetadata(String project, String zone, String instanceName, Metadata metadata)
            throws GceException {
        try {
            return this.apiClient.instances().setMetadata(project, zone, instanceName, metadata).execute();
        } catch (IOException e) {
            throw GceErrors.wrap(String.format("unable to set metadata for instance %s in project %s: %s", instanceName,
                    project, e.getMessage()), e);
        }
    }

    @Override
    public MultiZoneInstanceGroupClient multiZoneInstanceGroup(String project, String region, String instanceGroup) {
        ensureConfigured();
        return new MultiZoneInstanceGroupClient(this.apiClient, project, region, instanceGroup);
    }

    @Override
    public SingleZoneInstanceGroupClient singleZoneInstanceGroup(String project, String zone, String instanceGroup) {
        ensureConfigured();
        return new SingleZoneInstanceGroupClient(this.apiClient, project, zone, instanceGroup);
    }

    void ensureConfigured() {
        checkArgument(isConfigured(), "attempt to use GCE client before being configured");
    }

    boolean isConfigured() {
        return this.apiClient != null;
    }

    /**
     * Acquires a thread-safe authenticated Google Compute Engine API client for
     * the given service account.
     *
     * @param serviceAccountKey
     *            A service account key.
     * @return
     * @throws GceException
     */
    public static Compute acquireAuthenticatedApiClient(GoogleCredential serviceAccountKey) throws GceException {
        GoogleCredential credential = serviceAccountKey.createScoped(Collections.singletonList(GCE_AUTH_SCOPE));

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Compute computeApi = new Compute.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("Elastisys Google Compute Engine Cloudpool").build();
            return computeApi;
        } catch (Exception e) {
            throw GceErrors.wrap("failed to acquire authenticated client: " + e.getMessage(), e);
        }
    }
}
