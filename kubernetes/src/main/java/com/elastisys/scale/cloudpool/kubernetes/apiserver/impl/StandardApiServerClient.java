package com.elastisys.scale.cloudpool.kubernetes.apiserver.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.io.IOException;
import java.security.KeyStore;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpBuilder;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.google.gson.JsonObject;

public class StandardApiServerClient implements ApiServerClient {
    private static final Logger LOG = LoggerFactory.getLogger(StandardApiServerClient.class);

    /** The URL of the API server. For example, {@code https://host:443}. */
    private String apiServerUrl;
    /** Authentication credentials to access the API server. */
    private AuthConfig auth;

    @Override
    public ApiServerClient configure(String apiServerUrl, AuthConfig auth) {
        checkArgument(apiServerUrl != null, "apiServerUrl cannot be null");
        checkArgument(auth != null, "auth cannot be null");
        auth.validate();
        this.apiServerUrl = apiServerUrl;
        this.auth = auth;
        return this;
    }

    @Override
    public JsonObject get(String path) throws HttpResponseException, IOException {
        HttpGet request = new HttpGet(url(path));
        HttpRequestResponse response = client().execute(request);
        return bodyAsJsonObjectOrNull(response);
    }

    @Override
    public JsonObject put(String path, JsonObject body) throws HttpResponseException, IOException {
        HttpPut request = new HttpPut(url(path));
        request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.setEntity(new StringEntity(JsonUtils.toString(body)));
        HttpRequestResponse response = client().execute(request);
        return bodyAsJsonObjectOrNull(response);
    }

    @Override
    public JsonObject delete(String path) throws HttpResponseException, IOException {
        HttpDelete request = new HttpDelete(url(path));
        HttpRequestResponse response = client().execute(request);
        return bodyAsJsonObjectOrNull(response);
    }

    @Override
    public JsonObject post(String path, JsonObject body) throws HttpResponseException, IOException {
        HttpPost request = new HttpPost(url(path));
        request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.setEntity(new StringEntity(JsonUtils.toString(body)));
        HttpRequestResponse response = client().execute(request);
        return bodyAsJsonObjectOrNull(response);
    }

    @Override
    public JsonObject patch(String path, JsonObject body) throws HttpResponseException, IOException {
        HttpPatch request = new HttpPatch(url(path));

        // see
        // https://github.com/kubernetes/community/blob/master/contributors/devel/api-conventions.md#patch-operations
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/merge-patch+json");
        request.setEntity(new StringEntity(JsonUtils.toString(body)));
        HttpRequestResponse response = client().execute(request);
        return bodyAsJsonObjectOrNull(response);

    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.apiServerUrl != null && this.auth != null,
                "attempt to use http client before being configured");
    }

    /**
     * Sets up a {@link Http} client according to the configured authentication
     * parameters.
     *
     * @return
     */
    private Http client() throws KubernetesApiException, IllegalStateException {
        ensureConfigured();

        HttpBuilder clientBuilder = Http.builder();
        clientBuilder.verifyHostname(false);

        try {
            // client cert auth
            if (this.auth.hasClientCert()) {
                // password doesn't matter, only used here
                String keyPassword = "secret";
                KeyStore clientCertKeystore = PemUtils.keyStoreFromCertAndKey(this.auth.getClientCert(),
                        this.auth.getClientKey(), keyPassword);
                clientBuilder.clientCertAuth(clientCertKeystore, keyPassword);
            }

            // client token auth
            if (this.auth.hasClientToken()) {
                clientBuilder.clientJwtAuth(this.auth.getClientToken());
            }

            // server cert auth
            if (this.auth.hasServerCert()) {
                clientBuilder.verifyHostCert(true);
                KeyStore trustStore = PemUtils.keyStoreFromCert(this.auth.getServerCert());
                clientBuilder.serverAuthTrustStore(trustStore);
            }
        } catch (Exception e) {
            throw new KubernetesApiException(String.format("failed to set up auth: %s", e.getMessage()), e);
        }

        clientBuilder.contentType(ContentType.APPLICATION_JSON);
        clientBuilder.header(HttpHeaders.ACCEPT_ENCODING, ContentType.APPLICATION_JSON.getMimeType());
        return clientBuilder.build();
    }

    private String url(String path) {
        return this.apiServerUrl + path.replaceAll("//", "/");
    }

    /**
     * Extracts a {@link JsonObject} from a HTTP response, or returns
     * <code>null</code> in case the respone body is empty.
     *
     * @param response
     * @return
     */
    private JsonObject bodyAsJsonObjectOrNull(HttpRequestResponse response) {
        if (response.getResponseBody() == null || response.getResponseBody().isEmpty()) {
            return null;
        }
        return JsonUtils.parseJsonString(response.getResponseBody()).getAsJsonObject();
    }
}
