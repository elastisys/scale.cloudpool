package com.elastisys.scale.cloudpool.gce.lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.elastisys.scale.cloudpool.gce.driver.client.impl.StandardGceClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.compute.Compute;

/**
 * A base class that sets up credentials when constructing lab programs that
 * experiment against the GCE API.
 * <p/>
 * Before running this sample, <a href=
 * "https://cloud.google.com/iam/docs/creating-managing-service-accounts#creating_a_service_account">create
 * a service account</a> and, since the application needs to both read and
 * modify cloud resources, choose `Project Editor` as the service account's
 * `Role`. Then, <a href=
 * "https://cloud.google.com/iam/docs/managing-service-account-keys">create a
 * service account key</a> and store it in a local file, which you should point
 * out with the {@code ${GCE_SERVICE_ACCOUNT_KEY}} environment variable.
 */
public class BaseLabProgram {

    /** The service account key. */
    protected static final File serviceAccountKey = new File(System.getenv("GCE_SERVICE_ACCOUNT_KEY"));

    /**
     * Creates an authenticated Google Compute Engine client for the service
     * account whose key is specified by the {@code ${GCE_SERVICE_ACCOUNT_KEY}}.
     *
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws FileNotFoundException
     */
    public static Compute authenticatedApiClient() throws IOException, GeneralSecurityException {
        return StandardGceClient
                .acquireAuthenticatedApiClient(GoogleCredential.fromStream(new FileInputStream(serviceAccountKey)));
    }
}
