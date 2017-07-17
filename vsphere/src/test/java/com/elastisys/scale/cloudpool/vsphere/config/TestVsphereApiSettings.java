package com.elastisys.scale.cloudpool.vsphere.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestVsphereApiSettings {

    private static String url;
    private static String username;
    private static String password;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        url = "https://example.com";
        username = "Username";
        password = "passw0rd";
    }

    @Test
    public void completeConfig() throws MalformedURLException {
        VsphereApiSettings config = new VsphereApiSettings(url, username, password);
        config.validate();

        assertThat(config.getUrl().toString(), is(url));
        assertThat(config.getUsername(), is(username));
        assertThat(config.getPassword(), is(password));
    }

    @Test(expected = MalformedURLException.class)
    public void invalidUrl() throws MalformedURLException {
        String badUrl = "nothtps://bad#@broken.org";
        new VsphereApiSettings(badUrl, username, password);
    }

    @Test
    public void missingUrl() {
        try {
            new VsphereApiSettings(null, username, password);
            fail("expected to fail");
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void missingUsername() {
        try {
            new VsphereApiSettings(url, null, password).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("username"));
        } catch (MalformedURLException e) {
            fail("expected to succeed");
        }
    }

    @Test
    public void missingPassword() {
        try {
            new VsphereApiSettings(url, username, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("password"));
        } catch (MalformedURLException e) {
            fail("expected to succeed");
        }
    }
}
