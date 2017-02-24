package com.elastisys.scale.cloudpool.kubernetes.apiserver.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.http.entity.ContentType;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.mock.FakeServlet;
import com.elastisys.scale.cloudpool.kubernetes.mock.HttpResponse;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link StandardApiServerClient} against a fake API server.
 */
public class TestStandardApiServerClient {

    private static final String AUTH_TOKEN_PATH = "src/test/resources/ssl/auth-token";

    private int port = HostUtils.findFreePorts(1).get(0);

    /** Server hosting the fake api server. */
    private Server server;

    private FakeServlet fakeApiServer = new FakeServlet(new HttpResponse(200, null));

    /** Object under test. Set up to talk to {@link #fakeApiServer}. */
    private ApiServerClient apiServerClient;

    @Before
    public void beforeTest() throws Exception {
        ServletDefinition servletDef = new ServletDefinition.Builder().servlet(this.fakeApiServer).build();
        this.server = ServletServerBuilder.create().httpPort(this.port).addServlet(servletDef).build();
        this.server.start();

        this.apiServerClient = new StandardApiServerClient().configure(apiServerUrl(), tokenAuth());
    }

    @After
    public void afterTest() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * It should be possible to make a {@code GET} for a given path on the API
     * server. The response body should be converted to a {@link JsonObject}.
     */
    @Test
    public void get() throws Exception {
        JsonElement responseObject = JsonUtils.parseJsonString("{\"a\": 1}");
        String responseBody = JsonUtils.toString(responseObject);

        this.fakeApiServer.setResponse(new HttpResponse(200, responseBody));

        JsonObject response = this.apiServerClient.get("/some/path");
        assertThat(response, is(responseObject));

        assertThat(this.fakeApiServer.getRequests().size(), is(1));
        assertThat(this.fakeApiServer.getRequests().get(0).getPath(), is("/some/path"));
        assertThat(this.fakeApiServer.getRequests().get(0).getMethod(), is("GET"));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Accept-Encoding"), is("application/json"));
        assertThat(this.fakeApiServer.getRequests().get(0).getBody(), is(""));
    }

    /**
     * On a missing/empty response body, a null {@link JsonObject} should be
     * returned.
     */
    @Test
    public void getWithEmptyResponseBody() throws Exception {
        String responseBody = null;
        this.fakeApiServer.setResponse(new HttpResponse(200, responseBody));

        JsonObject response = this.apiServerClient.get("/some/path");
        assertThat(response, is(nullValue()));
    }

    /**
     * An {@link IOException} should be thrown when the api server does not
     * exist.
     */
    @Test(expected = IOException.class)
    public void getWithUrlToNonExistentServer() throws Exception {
        this.server.stop();

        this.apiServerClient.get("/some/path");
    }

    /**
     * It should be possible to make a {@code PUT} to a given path on the API
     * server.
     */
    @Test
    public void put() throws Exception {
        this.fakeApiServer.setResponse(new HttpResponse(200, ""));

        JsonObject response = this.apiServerClient.put("/some/path", asJson("{\"a\": 1}"));
        assertThat(response, is(nullValue()));

        assertThat(this.fakeApiServer.getRequests().size(), is(1));
        assertThat(this.fakeApiServer.getRequests().get(0).getPath(), is("/some/path"));
        assertThat(this.fakeApiServer.getRequests().get(0).getMethod(), is("PUT"));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Content-Type"),
                is(ContentType.APPLICATION_JSON.getMimeType()));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Accept-Encoding"), is("application/json"));
        assertThat(this.fakeApiServer.getRequests().get(0).getBody(), is("{\"a\":1}"));
    }

    /**
     * It should be possible to make a {@code POST} to a given path on the API
     * server.
     */
    @Test
    public void post() throws Exception {
        this.fakeApiServer.setResponse(new HttpResponse(200, ""));

        JsonObject response = this.apiServerClient.post("/some/path", asJson("{\"a\": 1}"));
        assertThat(response, is(nullValue()));

        assertThat(this.fakeApiServer.getRequests().size(), is(1));
        assertThat(this.fakeApiServer.getRequests().get(0).getPath(), is("/some/path"));
        assertThat(this.fakeApiServer.getRequests().get(0).getMethod(), is("POST"));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Content-Type"),
                is(ContentType.APPLICATION_JSON.getMimeType()));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Accept-Encoding"), is("application/json"));
        assertThat(this.fakeApiServer.getRequests().get(0).getBody(), is("{\"a\":1}"));
    }

    /**
     * It should be possible to make a {@code PATCH} to a given path on the API
     * server. It should produce a {@code application/merge-patch+json} encoded
     * request.
     */
    @Test
    public void patch() throws Exception {
        this.fakeApiServer.setResponse(new HttpResponse(200, ""));

        JsonObject response = this.apiServerClient.patch("/some/path", asJson("{\"a\": 1}"));
        assertThat(response, is(nullValue()));

        assertThat(this.fakeApiServer.getRequests().size(), is(1));
        assertThat(this.fakeApiServer.getRequests().get(0).getPath(), is("/some/path"));
        assertThat(this.fakeApiServer.getRequests().get(0).getMethod(), is("PATCH"));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Content-Type"),
                is("application/merge-patch+json"));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Accept-Encoding"), is("application/json"));
        assertThat(this.fakeApiServer.getRequests().get(0).getBody(), is("{\"a\":1}"));
    }

    private String apiServerUrl() {
        return String.format("http://localhost:%d", this.port);
    }

    private static JsonObject asJson(String jsonString) {
        return JsonUtils.parseJsonString(jsonString).getAsJsonObject();
    }

    /**
     * Creates an {@link AuthConfig} with token credentials.
     *
     * @return
     */
    private AuthConfig tokenAuth() {
        AuthConfig noAuthConfig = AuthConfig.builder().tokenPath(AUTH_TOKEN_PATH).build();
        return noAuthConfig;
    }

}
