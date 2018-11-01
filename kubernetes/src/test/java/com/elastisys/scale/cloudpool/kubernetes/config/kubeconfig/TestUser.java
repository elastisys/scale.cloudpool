package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Exercise the {@link User} class and its validation.
 */
public class TestUser {

    /** Path to client cert. */
    public static final String CLIENT_CERT_PATH = "src/test/resources/ssl/admin.pem";
    /** Path to client key. */
    public static final String CLIENT_KEY_PATH = "src/test/resources/ssl/admin-key.pem";
    /** Path to an auth token. */
    public static final String TOKEN_PATH = "src/test/resources/ssl/auth-token";

    /**
     * It should be possible to pass pem certificate and key as base64-encoded
     * data.
     */
    @Test
    public void certAuth() {
        User user = certAuthUser(IoUtils.toString("ssl/admin.pem.base64", StandardCharsets.UTF_8),
                IoUtils.toString("ssl/admin-key.pem.base64", StandardCharsets.UTF_8));
        user.validate();

        assertThat(user.hasCertAuth(), is(true));
        assertThat(user.hasTokenAuth(), is(false));
        assertThat(user.hasBasicAuth(), is(false));
    }

    /**
     * It should be possible to pass pem certificate and key as file system
     * paths.
     */
    @Test
    public void certAuthByPath() {
        User user = certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH);
        user.validate();

        assertThat(user.hasCertAuth(), is(true));
        assertThat(user.hasTokenAuth(), is(false));
        assertThat(user.hasBasicAuth(), is(false));
    }

    /**
     * It should be possible to pass a base64-encoded token as auth.
     */
    @Test
    public void tokenAuth() {
        User user = tokenAuthUser(IoUtils.toString("ssl/auth-token", StandardCharsets.UTF_8));
        user.validate();

        assertThat(user.hasCertAuth(), is(false));
        assertThat(user.hasTokenAuth(), is(true));
        assertThat(user.hasBasicAuth(), is(false));
    }

    /**
     * It should be possible to pass a file path to an auth token.
     */
    @Test
    public void tokenAuthByPath() {
        User user = tokenAuthUserByPath(TOKEN_PATH);
        user.validate();

        assertThat(user.hasCertAuth(), is(false));
        assertThat(user.hasTokenAuth(), is(true));
        assertThat(user.hasBasicAuth(), is(false));
    }

    /**
     * It should be possible to specify basic auth.
     */
    @Test
    public void basicAuth() {
        User user = basicAuthUser("foo", "secret");
        user.validate();

        assertThat(user.hasCertAuth(), is(false));
        assertThat(user.hasTokenAuth(), is(false));
        assertThat(user.hasBasicAuth(), is(true));
    }

    /**
     * It should be possible to combine cert auth and token auth.
     */
    @Test
    public void certAndTokenAuth() {
        User user = certAndTokenUser(CLIENT_CERT_PATH, CLIENT_KEY_PATH, TOKEN_PATH);
        user.validate();

        assertThat(user.hasCertAuth(), is(true));
        assertThat(user.hasTokenAuth(), is(true));
        assertThat(user.hasBasicAuth(), is(false));
    }

    /**
     * It should be possible to combine cert auth and basic auth.
     */
    @Test
    public void certAndBasicAuth() {
        User user = certAndBasicAuthUser(CLIENT_CERT_PATH, CLIENT_KEY_PATH, "foo", "secret");
        user.validate();

        assertThat(user.hasCertAuth(), is(true));
        assertThat(user.hasTokenAuth(), is(false));
        assertThat(user.hasBasicAuth(), is(true));
    }

    /**
     * Specyfing a cert requires a key.
     */
    @Test
    public void certRequiresKey() {
        User user = new User();
        user.clientCertificatePath = CLIENT_CERT_PATH;
        try {
            user.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("user: cannot specify a client cert without a client key"));
        }
    }

    /**
     * Specyfing a cert requires a key.
     */
    @Test
    public void keyRequiresCert() {
        User user = new User();
        user.clientKeyPath = CLIENT_KEY_PATH;
        try {
            user.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("user: cannot specify a client key without a client cert"));
        }
    }

    /**
     * Specyfing a username requires a password.
     */
    @Test
    public void usernameRequiresPassword() {
        User user = new User();
        user.username = "username";
        try {
            user.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("user: cannot specify a username without a password"));
        }
    }

    /**
     * Specyfing a password requires a username.
     */
    @Test
    public void passwordRequiresUsername() {
        User user = new User();
        user.password = "password";
        try {
            user.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("user: cannot specify a password without a username"));
        }
    }

    /**
     * Cannot use both basic auth and token auth according to
     * https://github.com/eBay/Kubernetes/blob/master/docs/user-guide/kubeconfig-file.md.
     */
    @Test
    public void basicAuthAndTokenAuthAreMutuallyExclusive() {
        try {
            basicAuthAndTokenUser("username", "password", TOKEN_PATH).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("user: token-auth and basic-auth are mutually exclusive"));
        }
    }

    /**
     * A validation of the content of cert data should be performed.
     */
    @Test
    public void badClientCertData() {
        try {
            certAuthUser("ABCDE", IoUtils.toString("ssl/admin-key.pem.base64", StandardCharsets.UTF_8)).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("user: failed to load cert"));
        }
    }

    /**
     * A validation of the content of cert file should be performed.
     */
    @Test
    public void badClientCertPath() {
        try {
            certAuthUserByPath("bad/path.pem", CLIENT_KEY_PATH).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("user: failed to load cert"));
        }
    }

    /**
     * A validation of the content of key data should be performed.
     */
    @Test
    public void badClientKeyData() {
        try {
            certAuthUser(IoUtils.toString("ssl/admin.pem.base64", StandardCharsets.UTF_8), "ABCDE").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("user: failed to load key"));
        }
    }

    /**
     * A validation of the content of key file should be performed.
     */
    @Test
    public void badClientKeyPath() {
        try {
            certAuthUserByPath(CLIENT_CERT_PATH, "bad/path.pem").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("user: failed to load key"));
        }
    }

    public static User certAuthUser(String certBase64Data, String keyBase64Data) {
        User user = new User();
        user.clientCertificateData = certBase64Data;
        user.clientKeyData = keyBase64Data;
        return user;
    }

    public static User certAuthUserByPath(String certPath, String keyPath) {
        User user = new User();
        user.clientCertificatePath = certPath;
        user.clientKeyPath = keyPath;
        return user;
    }

    public static User tokenAuthUser(String tokenBase64Data) {
        User user = new User();
        user.tokenData = tokenBase64Data;
        return user;
    }

    public static User tokenAuthUserByPath(String tokenPath) {
        User user = new User();
        user.tokenPath = tokenPath;
        return user;
    }

    public static User basicAuthUser(String username, String password) {
        User user = new User();
        user.username = username;
        user.password = password;
        return user;
    }

    public static User certAndBasicAuthUser(String certPath, String keyPath, String username, String password) {
        User user = new User();
        user.clientCertificatePath = certPath;
        user.clientKeyPath = keyPath;
        user.username = username;
        user.password = password;
        return user;
    }

    public static User certAndTokenUser(String certPath, String keyPath, String tokenPath) {
        User user = new User();
        user.clientCertificatePath = certPath;
        user.clientKeyPath = keyPath;
        user.tokenPath = tokenPath;
        return user;
    }

    public static User basicAuthAndTokenUser(String username, String password, String tokenPath) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.tokenPath = tokenPath;
        return user;
    }
}
