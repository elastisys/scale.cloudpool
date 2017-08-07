package com.elastisys.scale.cloudpool.vsphere.driver.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TestVsphereProvisioningTemplate {
    private String template = "template";
    private String resourcePool = "pool";
    private String folder = "folder";

    @Test
    public void basicSanity() {
        VsphereProvisioningTemplate config = new VsphereProvisioningTemplate(template, resourcePool, folder);
        config.validate();

        assertThat(config.getTemplate(), is(template));
        assertThat(config.getResourcePool(), is(resourcePool));
        assertThat(config.getFolder(), is(folder));
    }

    @Test
    public void missingTemplate() {
        try {
            new VsphereProvisioningTemplate(null, null, null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("template"));
        }
    }
}
