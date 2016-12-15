package com.elastisys.scale.cloudpool.commons.basepool.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Exercise {@link DriverConfig}.
 */
public class TestDriverConfig {
    private static final String NAME = "webserver-pool";
    private static final JsonObject CLOUD_API_SETTINGS = JsonUtils
            .parseJsonString("{\"apiUser\": \"foo\", \"apiPassword\": \"secret\"}").getAsJsonObject();
    private static final JsonObject PROVISIONING_TEMPLATE = JsonUtils
            .parseJsonString("{\"size\": \"medium\", \"image\": \"ubuntu-16.04\"}").getAsJsonObject();;

    @Test
    public void basicSanity() {
        DriverConfig config = new DriverConfig(NAME, CLOUD_API_SETTINGS, PROVISIONING_TEMPLATE);
        config.validate();

        assertThat(config.getPoolName(), is(NAME));
        assertThat(config.getCloudApiSettings(), is(CLOUD_API_SETTINGS));
        assertThat(config.getProvisioningTemplate(), is(PROVISIONING_TEMPLATE));
    }

    /**
     * name is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingName() {
        new DriverConfig(null, CLOUD_API_SETTINGS, PROVISIONING_TEMPLATE).validate();
    }

    /**
     * cloudApiSettings is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingCloudApiSettings() {
        new DriverConfig(NAME, null, PROVISIONING_TEMPLATE).validate();
    }

    /**
     * provisioningTemplate is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingProvisioningTemplate() {
        new DriverConfig(NAME, CLOUD_API_SETTINGS, null).validate();
    }

    /**
     * The {@link DriverConfig} is capable of parsing the provisioningTemplate
     * into a given Java type.
     */
    @Test
    public void parseProvisioingTemplate() {
        DriverConfig driverConfig = new DriverConfig(NAME, CLOUD_API_SETTINGS, PROVISIONING_TEMPLATE);

        SampleProvisioningTemplateType parsedObject = driverConfig
                .parseProvisioningTemplate(SampleProvisioningTemplateType.class);
        assertThat(parsedObject.size, is("medium"));
        assertThat(parsedObject.image, is("ubuntu-16.04"));
    }

    /**
     * The {@link DriverConfig} is capable of parsing the cloudApiSettings into
     * a given Java type.
     */
    @Test
    public void parseCloudApiSettings() {
        DriverConfig driverConfig = new DriverConfig(NAME, CLOUD_API_SETTINGS, PROVISIONING_TEMPLATE);

        SampleCloudApiSettingsType parsedObject = driverConfig.parseCloudApiSettings(SampleCloudApiSettingsType.class);
        assertThat(parsedObject.apiUser, is("foo"));
        assertThat(parsedObject.apiPassword, is("secret"));
    }

    private static class SampleCloudApiSettingsType {
        private String apiUser;
        private String apiPassword;
    }

    private static class SampleProvisioningTemplateType {
        private String size;
        private String image;
    }

}
