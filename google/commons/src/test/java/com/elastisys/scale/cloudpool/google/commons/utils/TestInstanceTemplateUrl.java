package com.elastisys.scale.cloudpool.google.commons.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link InstanceTemplateUrl} class.
 *
 */
public class TestInstanceTemplateUrl {

    /** Sample instance URL. */
    private static final String INSTANCE_TEMPLATE_URL = "https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/webserver-template";

    @Test
    public void parseProject() {
        assertThat(new InstanceTemplateUrl(INSTANCE_TEMPLATE_URL).getProject(), is("my-project"));
    }

    @Test
    public void parseName() {
        assertThat(new InstanceTemplateUrl(INSTANCE_TEMPLATE_URL).getName(), is("webserver-template"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInstanceTemplateUrlMissingProject() {
        new InstanceTemplateUrl(
                "https://www.googleapis.com/compute/v1/projects/global/instanceTemplates/webserver-template");
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInstanceTemplateUrlMissingName() {
        new InstanceTemplateUrl("https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates");
    }

    @Test
    public void fromProjectZoneAndName() {
        assertThat(InstanceTemplateUrl.from("my-project", "webserver-template"), //
                is(new InstanceTemplateUrl(
                        "https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/webserver-template")));
    }
}
