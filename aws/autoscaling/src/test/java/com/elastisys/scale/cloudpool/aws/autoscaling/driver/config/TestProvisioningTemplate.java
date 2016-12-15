package com.elastisys.scale.cloudpool.aws.autoscaling.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercise {@link ProvisioningTemplate}.
 */
public class TestProvisioningTemplate {

    /**
     * It should be possible to give an explicity Auto Scaling Group to manage.
     */
    @Test
    public void explicit() {
        ProvisioningTemplate template = new ProvisioningTemplate("autoScalingGroupName");
        template.validate();

        assertThat(template.getAutoScalingGroup().isPresent(), is(true));
        assertThat(template.getAutoScalingGroup().get(), is("autoScalingGroupName"));
    }

    /**
     * autoScalingGroup is optional.
     */
    @Test
    public void defaults() {
        ProvisioningTemplate template = new ProvisioningTemplate(null);
        template.validate();

        assertThat(template.getAutoScalingGroup().isPresent(), is(false));
    }
}
