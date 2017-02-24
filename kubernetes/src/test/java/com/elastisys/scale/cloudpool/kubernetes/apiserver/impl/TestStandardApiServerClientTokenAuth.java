package com.elastisys.scale.cloudpool.kubernetes.apiserver.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.mock.FakeServlet;
import com.elastisys.scale.cloudpool.kubernetes.mock.HttpResponse;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Verifies proper behavior of {@link StandardApiServerClient} when told to
 * authenticate against the API server with a JWT auth token.
 */
public class TestStandardApiServerClientTokenAuth {
    /** Path to client token. */
    private static final String CLIENT_TOKEN_PATH = "src/test/resources/ssl/auth-token";

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
    }

    @After
    public void afterTest() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * Make sure that {@link StandardApiServerClient} can be told to
     * authenticate with a JWT auth token.
     */
    @Test
    public void useTokenAuth() throws Exception {
        String token = Files.toString(new File(CLIENT_TOKEN_PATH), Charsets.UTF_8);

        // should
        this.apiServerClient = new StandardApiServerClient().configure(apiServerUrl(), tokenAuth(token));
        this.apiServerClient.get("/some/path");

        // verify that the expected auth header was included in request
        assertThat(this.fakeApiServer.getRequests().size(), is(1));
        assertThat(this.fakeApiServer.getRequests().get(0).getHeaders().get("Authorization"), is("Bearer " + token));
    }

    private AuthConfig tokenAuth(String token) {
        return AuthConfig.builder().token(token).build();
    }

    private String apiServerUrl() {
        return String.format("http://localhost:%d", this.port);
    }

}
