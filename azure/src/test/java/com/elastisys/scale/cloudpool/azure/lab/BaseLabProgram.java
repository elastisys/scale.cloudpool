package com.elastisys.scale.cloudpool.azure.lab;

import com.elastisys.scale.cloudpool.azure.driver.config.AzureAuth;

/**
 * Base class to use for lab programs, which sets application credentials from
 * environment variables.
 */
public class BaseLabProgram {

    protected static final AzureAuth AZURE_AUTH = new AzureAuth(System.getenv("AZURE_CLIENT_ID"),
            System.getenv("AZURE_DOMAIN_ID"), System.getenv("AZURE_SECRET"), null);

    protected static final String SUBSCRIPTION_ID = System.getenv("AZURE_SUBSCRIPTION_ID");
}
