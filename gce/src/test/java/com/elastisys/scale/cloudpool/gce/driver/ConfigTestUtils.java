package com.elastisys.scale.cloudpool.gce.driver;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.gce.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.gce.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;

public class ConfigTestUtils {

    /** Sample project name. */
    public static final String PROJECT = "my-project";
    /** Sample zone name. */
    public static final String ZONE = "eu-west1-b";
    /** Sample instance group name. */
    public static final String INSTANCE_GROUP = "my-instance-group";

    /** Sample instance template short name. */
    public static final String INSTANCE_TEMPLATE_NAME = "webserver-template";
    /** Sample instance template URL. */
    public static final String INSTANCE_TEMPLATE = String.format(
            "https://www.googleapis.com/compute/v1/projects/%s/global/instanceTemplates/%s", PROJECT,
            INSTANCE_TEMPLATE_NAME);
    /** Sample machine type. */
    private static final String MACHINE_TYPE = "n1-standard-1";

    public static CloudApiSettings validCloudApiSettings() {
        return new CloudApiSettings("src/test/resources/config/valid-service-account-key.json", null);
    }

    public static Object invalidCloudApiSettings() {
        return new CloudApiSettings("/non/existing/service-account-key.json", null);
    }

    public static ProvisioningTemplate validProvisioningTemplate() {
        return new ProvisioningTemplate(INSTANCE_GROUP, PROJECT, null, ZONE);
    }

    public static DriverConfig validDriverConfig(String poolName) {
        return new DriverConfig(poolName, JsonUtils.toJson(validCloudApiSettings()).getAsJsonObject(),
                JsonUtils.toJson(validProvisioningTemplate()).getAsJsonObject());
    }

    public static DriverConfig invalidDriverConfig(String poolName) {
        return new DriverConfig(poolName, JsonUtils.toJson(invalidCloudApiSettings()).getAsJsonObject(),
                JsonUtils.toJson(validProvisioningTemplate()).getAsJsonObject());
    }
}
