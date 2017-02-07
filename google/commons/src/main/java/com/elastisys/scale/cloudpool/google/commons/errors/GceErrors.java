package com.elastisys.scale.cloudpool.google.commons.errors;

import org.apache.http.HttpStatus;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

/**
 * Utility class for handling GCE client library {@link Exception}s.
 *
 */
public class GceErrors {

    /**
     * Wraps an {@link Exception} returned by the GCE client library with a
     * {@link NotFoundException} if the error represents a 404 response and a
     * {@link GceException} otherwise.
     *
     * @param message
     *            The message to include
     * @param cause
     *            The original error.
     * @return A {@link NotFoundException} if the error represents a 404
     *         response, a {@link GceException} otherwise.
     */
    public static RuntimeException wrap(String message, Exception cause) {

        if (GoogleJsonResponseException.class.isAssignableFrom(cause.getClass())) {
            GoogleJsonResponseException responseError = GoogleJsonResponseException.class.cast(cause);
            if (responseError.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return new NotFoundException(message, cause);
            }
        }

        return new GceException(message, cause);
    }
}
