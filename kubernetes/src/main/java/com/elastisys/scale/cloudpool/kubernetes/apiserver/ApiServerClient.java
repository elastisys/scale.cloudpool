package com.elastisys.scale.cloudpool.kubernetes.apiserver;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;

import com.google.gson.JsonObject;

/**
 * A Kubernetes API server HTTP client which can be used to send (authenticated)
 * HTTP requests to a particular Kubernetes API server.
 */
public interface ApiServerClient {

    /**
     * Configures this {@link ApiServerClient} to operate against a certain API
     * server with given credentials.
     *
     * @param clientConfig
     *            Specifies how to connect to the targeted Kubernetes apiserver.
     *
     * @return The object itself (to allow method chaining).
     */
    public ApiServerClient configure(ClientConfig clientConfig);

    /**
     * Performs a {@code GET} for a particular apiserver path. Returns the
     * resulting JSON response if one was received, otherwise returns
     * <code>null</code>.
     *
     * @param path
     *            API server destination path. For example,
     *            {@code /api/v1/namespaces/default/pods}
     * @return
     */
    JsonObject get(String path) throws HttpResponseException, IOException;

    /**
     * Performs a {@code PUT} against a particular apiserver path and with a
     * given (optional) body. Returns the resulting JSON response if one was
     * received, otherwise returns <code>null</code>.
     *
     * @param path
     *            API server destination path. For example,
     *            {@code /api/v1/namespaces/default/pods}
     * @param body
     *            The request body or <code>null</code> to send without body.
     * @return
     */
    JsonObject put(String path, JsonObject body) throws HttpResponseException, IOException;

    /**
     * Performs a {@code DELETE} against a particular apiserver path and with a
     * given (optional) body. Returns the resulting JSON response if one was
     * received, otherwise returns <code>null</code>.
     *
     * @param path
     *            API server destination path. For example,
     *            {@code /api/v1/namespaces/default/pods}
     * @return
     */
    JsonObject delete(String path) throws HttpResponseException, IOException;

    /**
     * Performs a {@code POST} against a particular apiserver path and with a
     * given (optional) body. Returns the resulting JSON response if one was
     * received, otherwise returns <code>null</code>.
     *
     * @param path
     *            API server destination path. For example,
     *            {@code /api/v1/namespaces/default/pods}
     * @param body
     *            The request body or <code>null</code> to send without body.
     * @return
     */
    JsonObject post(String path, JsonObject body) throws HttpResponseException, IOException;

    /**
     * Performs a {@code PATCH} against a particular apiserver path and with a
     * given (optional) body. Returns the resulting JSON response if one was
     * received, otherwise returns <code>null</code>.
     *
     * @param path
     *            API server destination path. For example,
     *            {@code /api/v1/namespaces/default/pods}
     * @param body
     *            The request body or <code>null</code> to send without body.
     * @return
     */
    JsonObject patch(String path, JsonObject body) throws HttpResponseException, IOException;
}
