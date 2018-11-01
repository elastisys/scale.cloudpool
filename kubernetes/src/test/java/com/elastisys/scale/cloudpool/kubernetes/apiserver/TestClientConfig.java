package com.elastisys.scale.cloudpool.kubernetes.apiserver;

import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUser.CLIENT_CERT_PATH;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUser.CLIENT_KEY_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercise {@link ClientConfig}.
 */
public class TestClientConfig {

    @Test
    public void create() {
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .build();
        ClientConfig clientConfig = new ClientConfig("https://apiserver", creds);

        assertThat(clientConfig.getApiServerUrl(), is("https://apiserver"));
        assertThat(clientConfig.getCredentials(), is(creds));
    }

    /**
     * Should raise an {@link IllegalArgumentException} if no apiserver is
     * supplied.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithoutApiServer() {
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .build();
        new ClientConfig(null, creds);
    }

    /**
     * Should raise an {@link IllegalArgumentException} if no auth credentials
     * are supplied.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithoutCredentials() {
        new ClientConfig("https://apiserver", null);
    }
}
