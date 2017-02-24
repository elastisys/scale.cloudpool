package com.elastisys.scale.cloudpool.kubernetes.testutils;

import com.elastisys.scale.cloudpool.kubernetes.types.Status;
import com.elastisys.scale.cloudpool.kubernetes.types.StatusDetails;

/**
 * Produces fake Kubernetes API error messages to be used in tests.
 */
public class FakeKubeErrors {

    /**
     * Creates a fake {@link Status} message body for a Kubernetes API error of
     * the given kind.
     *
     * @param statusCode
     * @param message
     * @return
     */
    public static Status errorStatus(int statusCode, String message) {
        Status status = new Status();
        status.apiVersion = "v1";
        status.kind = "Status";
        status.status = "Failure";
        status.message = message;
        status.reason = "Reason";
        status.details = new StatusDetails();
        status.message = message;
        status.code = statusCode;
        return status;
    }
}
