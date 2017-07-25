package com.elastisys.scale.cloudpool.vsphere.util;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

public class TestUtils {

    public static DriverConfig loadDriverConfig(String resourcePath) {
        BaseCloudPoolConfig baseCloudPoolConfig = JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
                BaseCloudPoolConfig.class);
        DriverConfig driverConfig = new DriverConfig(baseCloudPoolConfig.getName(),
                JsonUtils.toJson(baseCloudPoolConfig.getCloudApiSettings()).getAsJsonObject(),
                JsonUtils.toJson(baseCloudPoolConfig.getProvisioningTemplate()).getAsJsonObject());
        return driverConfig;
    }
}
