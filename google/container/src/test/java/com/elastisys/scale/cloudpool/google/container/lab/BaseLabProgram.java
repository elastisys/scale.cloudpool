package com.elastisys.scale.cloudpool.google.container.lab;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.clients.ApiClients;
import com.google.api.services.compute.Compute;
import com.google.api.services.container.Container;

/**
 * A base class that sets up credentials when constructing lab programs that
 * experiment against the Google Cloud Platform.
 * <p/>
 * Before running this sample, <a href=
 * "https://cloud.google.com/iam/docs/creating-managing-service-accounts#creating_a_service_account">create
 * a service account</a> and, since the application needs to both read and
 * modify cloud resources, choose `Project Editor` as the service account's
 * `Role`. Then, <a href=
 * "https://cloud.google.com/iam/docs/managing-service-account-keys">create a
 * service account key</a> and store it in a local file, which you should point
 * out with the {@code ${GOOGLE_SERVICE_ACCOUNT_KEY}} environment variable.
 */
public class BaseLabProgram {

    /** The service account key. */
    protected static final String serviceAccountKeyPath = System.getenv("GOOGLE_SERVICE_ACCOUNT_KEY");

    /**
     * Creates an authenticated Google Container Engine client for the service
     * account whose key is specified by the
     * {@code ${GOOGLE_SERVICE_ACCOUNT_KEY}}.
     *
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws FileNotFoundException
     */
    public static Container containerApiClient() throws IOException, GeneralSecurityException {
        return ApiClients.container(new CloudApiSettings(serviceAccountKeyPath, null).getApiCredential());
    }

    /**
     * Creates an authenticated Google Compute Engine client for the service
     * account whose key is specified by the
     * {@code ${GOOGLE_SERVICE_ACCOUNT_KEY}}.
     *
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws FileNotFoundException
     */
    public static Compute computeApiClient() throws IOException, GeneralSecurityException {
        return ApiClients.compute(new CloudApiSettings(serviceAccountKeyPath, null).getApiCredential());
    }
}
