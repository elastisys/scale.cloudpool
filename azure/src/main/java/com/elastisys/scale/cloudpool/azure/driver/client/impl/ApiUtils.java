package com.elastisys.scale.cloudpool.azure.driver.client.impl;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.google.common.base.Preconditions;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;

/**
 * Azure API utilities.
 */
public class ApiUtils {

    /**
     * Acquires an authenticated API client.
     *
     * @return
     * @throws CloudException
     *             Thrown on failure to acquire the API client.
     */
    public static Azure acquireApiClient(AzureApiAccess apiAccess) throws CloudException {
        Preconditions.checkArgument(apiAccess != null, "apiAccess cannot be null");

        try {
            return Azure.configure()
                    .withConnectionTimeout(apiAccess.getConnectionTimeout().getTime(),
                            apiAccess.getConnectionTimeout().getUnit())
                    .withReadTimeout(apiAccess.getReadTimeout().getTime(), apiAccess.getReadTimeout().getUnit())
                    .withLogLevel(apiAccess.getAzureSdkLogLevel()) //
                    .authenticate(apiAccess.getAuth().toTokenCredentials()) //
                    .withSubscription(apiAccess.getSubscriptionId());
        } catch (Exception e) {
            throw new CloudException("failed to acquire API client: " + e.getMessage(), e);
        }
    }
}
