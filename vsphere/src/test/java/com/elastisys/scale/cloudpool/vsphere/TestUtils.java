package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

public class TestUtils {
    public static DriverConfig loadDriverConfig(String resourcePath) {
        BaseCloudPoolConfig baseConfig = JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
                BaseCloudPoolConfig.class);
        JsonObject cloudApiSettings = JsonUtils.toJson(baseConfig.getCloudApiSettings()).getAsJsonObject();
        JsonObject provisioningTemplate = JsonUtils.toJson(baseConfig.getProvisioningTemplate()).getAsJsonObject();
        DriverConfig config = new DriverConfig(baseConfig.getName(), cloudApiSettings, provisioningTemplate);
        return config;
    }
}
