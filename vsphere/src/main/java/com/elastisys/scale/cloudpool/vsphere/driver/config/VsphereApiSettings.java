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
     * 
     * @param url
     *            The url to the Vcenter Server.
     * @param username
     *            The username for logging in to Vcenter.
     * @param password
     *            The password for logging in to Vcenter.
     * @throws MalformedURLException
     */
    public VsphereApiSettings(String url, String username, String password) throws MalformedURLException {
        this.url = new URL(url);
        this.username = username;
        this.password = password;
    }

    /**
     * Do basic validation of the VsphereApiSettings and throw an exception if
     * validation fails.
     * 
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.url != null, "missing URL");
        checkArgument(this.username != null, "missing username");
        checkArgument(this.password != null, "missing password");
    }

    /**
     * The URL to the Vcenter Server.
     * 
     * @return the URL
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * The username for logging in to Vcenter.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * The password for logging in to Vcenter.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}