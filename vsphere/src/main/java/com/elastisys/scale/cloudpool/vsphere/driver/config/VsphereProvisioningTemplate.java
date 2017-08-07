package com.elastisys.scale.cloudpool.vsphere.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Vsphere-specific provisioning template.
 */
public class VsphereProvisioningTemplate {

    private final String template;
    private final String resourcePool;
    private final String folder;

    /**
     * Create a new VsphereProvisioningTemplate.
     * @param template  The name of the vSphere template to use.
     * @param resourcePool  ResourcePool to put new VMs in.
     * @param folder    Folder to put new VMs in.
     */
    public VsphereProvisioningTemplate(String template, String resourcePool, String folder) {
        this.template = template;
        this.resourcePool = resourcePool;
        this.folder = folder;
    }

    /**
     * Preform basic validation of the VsphereProvisioningTemplate.
     */
    public void validate() {
        checkArgument(this.template != null, "missing template");
    }

    /**
     * @return The name of the vSphere template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @return ResourcePool to put new VMs in.
     */
    public String getResourcePool() {
        return resourcePool;
    }

    /**
     * @return  Folder to put new VMs in.
     */
    public String getFolder() {
        return folder;
    }
}