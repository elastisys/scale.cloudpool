package com.elastisys.scale.cloudpool.azure.driver.requests;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.ApiUtils;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.google.common.base.Preconditions;
import com.microsoft.azure.management.Azure;

/**
 * An abstract base class for Azure request clients.
 * <p/>
 * Sub-classes need to implement the {@link #doRequest(Azure)} method.
 *
 * @param <R>
 *            The response type.
 */
abstract public class AzureRequest<R> implements Callable<R> {
    protected static final Logger LOG = LoggerFactory.getLogger(AzureRequest.class);

    /** Azure API access credentials and settings. */
    private final AzureApiAccess apiAccess;

    /**
     * Creates an {@link AzureRequest} with the given API access settings.
     *
     * @param apiAccess
     *            Azure API access credentials and settings.
     */
    public AzureRequest(AzureApiAccess apiAccess) {
        Preconditions.checkArgument(apiAccess != null, "apiAccess cannot be null");
        this.apiAccess = apiAccess;
    }

    @Override
    public R call() throws AzureException {
        return doRequest(ApiUtils.acquireApiClient(this.apiAccess));
    }

    /**
     * Azure API access credentials and settings.
     *
     * @return
     */
    protected AzureApiAccess apiAccess() {
        return this.apiAccess;
    }

    /**
     * Perform a request against the Azure API and return a response. On failure
     * to complete the request, an exception is thrown.
     *
     * @param api
     * @return
     * @throws AzureException
     */
    abstract public R doRequest(Azure api) throws AzureException;
}
