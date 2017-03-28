package com.elastisys.scale.cloudpool.azure.driver.client;

/**
 * Thrown to indicate an Azure API error.
 */
public class AzureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AzureException() {
        super();
    }

    public AzureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AzureException(String message, Throwable cause) {
        super(message, cause);
    }

    public AzureException(String message) {
        super(message);
    }

    public AzureException(Throwable cause) {
        super(cause);
    }

}
