package com.elastisys.scale.cloudpool.vsphere.driver.config;

import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Cloud API settings for an {@link VspherePoolDriver}.
 */
public class VsphereApiSettings {

    private final URL url;
    private final String username;
    private final String password;

    /**
     * Create VsphereApiSettings.
     * @param url   The url to the vCenter Server.
     * @param username  The username for logging in to vCenter.
     * @param password  The password for logging in to vCenter.
     * @throws MalformedURLException
     */
    public VsphereApiSettings(String url, String username, String password) throws MalformedURLException {
        this.url = new URL(url);
        this.username = username;
        this.password = password;
    }

    /**
     * Do basic validation of the VsphereApiSettings and throw an exception if validation fails.
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.url != null, "missing URL");
        checkArgument(this.username != null, "missing username");
        checkArgument(this.password != null, "missing password");
    }

    /**
     * The URL to the vCenter Server.
     * @return
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * The username for logging in to vCenter.
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * The password for logging in to vCenter.
     * @return
     */
    public String getPassword() {
        return password;
    }
}