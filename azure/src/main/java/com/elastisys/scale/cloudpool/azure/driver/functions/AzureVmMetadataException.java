package com.elastisys.scale.cloudpool.azure.driver.functions;

import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;

/**
 * Thrown to indicate a problem to extract metadata from an Azure VM.
 */
public class AzureVmMetadataException extends CloudPoolDriverException {

    public AzureVmMetadataException() {
        super();
    }

    public AzureVmMetadataException(String message, Throwable cause) {
        super(message, cause);
    }

    public AzureVmMetadataException(String message) {
        super(message);
    }

    public AzureVmMetadataException(Throwable cause) {
        super(cause);
    }

}
