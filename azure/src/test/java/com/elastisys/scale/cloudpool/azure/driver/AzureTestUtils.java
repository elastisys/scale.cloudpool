package com.elastisys.scale.cloudpool.azure.driver;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

public class AzureTestUtils {

    /**
     * Creates a {@link DriverConfig} from a {@link BaseCloudPoolConfig}.
     *
     * @param config
     * @return
     */
    public static DriverConfig driverConfig(BaseCloudPoolConfig config) {
        return new DriverConfig(config.getName(), config.getCloudApiSettings(), config.getProvisioningTemplate());
    }

    public static BaseCloudPoolConfig loadPoolConfig(String resourcePath) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath), BaseCloudPoolConfig.class);
    }

}
