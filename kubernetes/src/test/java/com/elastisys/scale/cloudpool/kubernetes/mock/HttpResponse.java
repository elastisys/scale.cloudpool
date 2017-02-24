package com.elastisys.scale.cloudpool.kubernetes.mock;

/**
 * Simple HTTP response produced by {@link FakeServlet}.
 * <p/>
 * <b>Note: intended for testing purposes</b>.
 *
 * @see FakeServlet
 */
public class HttpResponse {
    /** The status code of the HTTP response. */
    private final int statusCode;
    /** The message body of the HTTP response. */
    private final String responseBody;

    public HttpResponse(int statusCode, String responseBody) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
