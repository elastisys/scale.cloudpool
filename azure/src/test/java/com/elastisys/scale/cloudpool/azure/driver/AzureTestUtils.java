package com.elastisys.scale.cloudpool.azure.driver;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

public class AzureTestUtils {

    public static BaseCloudPoolConfig validConfig() {
        return JsonUtils.toObject(JsonUtils.parseJsonResource("config/valid-config.json"), BaseCloudPoolConfig.class);
    }

    public static BaseCloudPoolConfig invalidConfig() {
        return JsonUtils.toObject(JsonUtils.parseJsonResource("config/invalid-config.json"), BaseCloudPoolConfig.class);
    }
}
