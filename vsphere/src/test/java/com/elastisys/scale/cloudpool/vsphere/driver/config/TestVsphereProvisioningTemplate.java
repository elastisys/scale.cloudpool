package com.elastisys.scale.cloudpool.vsphere.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

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
    public void onlyMandatoryArguments() {
        VsphereProvisioningTemplate config = new VsphereProvisioningTemplate(template, null, null);
        config.validate();

        assertThat(config.getTemplate(), is(template));
        assertThat(config.getResourcePool(), is(nullValue()));
        assertThat(config.getFolder(), is(nullValue()));
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
