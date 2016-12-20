package com.elastisys.scale.cloudpool.gce.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.gce.driver.client.GceException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;

public class TestGceErrors {

    /**
     * A 404 {@link GoogleJsonResponseException} should be wrapped by a
     * {@link NotFoundException}.
     */
    @Test
    public void wrap404ResponseError() {
        GoogleJsonResponseException googleJsonResponseException = sampleResponseError(404);

        RuntimeException wrappedError = GceErrors.wrap("something was not found", googleJsonResponseException);
        assertTrue(wrappedError.getClass() == NotFoundException.class);
        assertThat(wrappedError.getMessage(), is("something was not found"));
    }

    /**
     * A non-404 {@link GoogleJsonResponseException} should be wrapped by a
     * {@link GceException}.
     */
    @Test
    public void wrapNon404ResponseError() {
        GoogleJsonResponseException googleJsonResponseException = sampleResponseError(500);

        RuntimeException wrappedError = GceErrors.wrap("something failed", googleJsonResponseException);
        assertTrue(wrappedError.getClass() == GceException.class);
        assertThat(wrappedError.getMessage(), is("something failed"));
    }

    /**
     * Other errors than {@link GoogleJsonResponseException} should be wrapped
     * by a {@link GceException}.
     */
    @Test
    public void wrapOtherError() {
        RuntimeException wrappedError = GceErrors.wrap("some error occured", new RuntimeException());
        assertThat(wrappedError.getMessage(), is("some error occured"));
        assertTrue(wrappedError.getClass() == GceException.class);
    }

    private static GoogleJsonResponseException sampleResponseError(int statusCode) {
        GoogleJsonError googleJsonError = new GoogleJsonError();
        googleJsonError.setCode(statusCode);

        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(
                new Builder(statusCode, "error message", new HttpHeaders().setContentEncoding("application/json")),
                googleJsonError);
        return googleJsonResponseException;
    }
}
