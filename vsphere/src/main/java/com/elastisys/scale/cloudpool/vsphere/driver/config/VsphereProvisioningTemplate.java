package com.elastisys.scale.cloudpool.vsphere.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

public class VsphereProvisioningTemplate {

    private final String template;
    private final String resourcePool;
    private final String folder;

    public VsphereProvisioningTemplate(String template, String resourcePool, String folder) {
        this.template = template;
        this.resourcePool = resourcePool;
        this.folder = folder;
    }

    public void validate() {
        checkArgument(this.template != null, "missing template");
    }

    public String getTemplate() {
        return template;
    }

    public String getResourcePool() {
        return resourcePool;
    }

    public String getFolder() {
        return folder;
    }
}