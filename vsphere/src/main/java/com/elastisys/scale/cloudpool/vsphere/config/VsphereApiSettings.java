package com.elastisys.scale.cloudpool.vsphere.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.MalformedURLException;
import java.net.URL;

public class VsphereApiSettings {

    private final URL url;
    private final String username;
    private final String password;

    public VsphereApiSettings(String url, String username, String password) throws MalformedURLException {
        this.url = new URL(url);
        this.username = username;
        this.password = password;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.username != null, "missing username");
        checkArgument(this.password != null, "missing password");
    }

    public URL getUrl() {
        return this.url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}