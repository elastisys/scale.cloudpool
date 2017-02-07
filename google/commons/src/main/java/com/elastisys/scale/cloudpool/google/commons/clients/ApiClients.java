package com.elastisys.scale.cloudpool.google.commons.clients;

import java.util.Arrays;
import java.util.Collections;

import com.elastisys.scale.cloudpool.google.commons.errors.GceErrors;
import com.elastisys.scale.cloudpool.google.commons.errors.GceException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.container.Container;

/**
 * Convenience class for acquiring raw API clients for different Google Cloud
 * Platform Services.
 *
 */
public class ApiClients {
    /** The UserAgent header to set for requests created by clients. */
    private static final String APPLICATION_NAME = "Elastisys";

    /**
     * Authentication scope for the Compute service. See
     * https://developers.google.com/identity/protocols/googlescopes#computev1
     */
    private static final String GCE_AUTH_SCOPE = "https://www.googleapis.com/auth/compute";
    /**
     * Authentication scope for the Container service. See
     * https://developers.google.com/identity/protocols/googlescopes#containerv1
     */
    private static final String GKE_AUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    /**
     * Acquires a thread-safe authenticated Google Compute Engine (GCE) API
     * client for a given service account.
     *
     * @param serviceAccountKey
     *            A service account key.
     * @return
     * @throws GceException
     */
    public static Compute compute(GoogleCredential serviceAccountKey) throws GceException {
        GoogleCredential credential = serviceAccountKey.createScoped(Collections.singletonList(GCE_AUTH_SCOPE));

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Compute computeApi = new Compute.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME).build();
            return computeApi;
        } catch (Exception e) {
            throw GceErrors.wrap("failed to acquire authenticated client: " + e.getMessage(), e);
        }
    }

    /**
     * Acquires a thread-safe authenticated Google Container Engine (GKE) API
     * client for the given service account.
     *
     * @param serviceAccountKey
     *            A service account key.
     * @return
     * @throws GceException
     */
    public static Container container(GoogleCredential serviceAccountKey) throws GceException {
        GoogleCredential credential = serviceAccountKey.createScoped(Arrays.asList(GKE_AUTH_SCOPE, GCE_AUTH_SCOPE));

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Container containerApi = new Container.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME).build();
            return containerApi;
        } catch (Exception e) {
            throw GceErrors.wrap("failed to acquire authenticated client: " + e.getMessage(), e);
        }
    }
}
