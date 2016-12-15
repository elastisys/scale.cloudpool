package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link CloudPoolConfig}.
 */
public class TestCloudPoolConfig {

    @Test
    public void basicSanity() {
        JsonObject driverConfig = JsonUtils.parseJsonString("{'config': 'value'}").getAsJsonObject();
        CloudPoolConfig config = new CloudPoolConfig("poolName", driverConfig);
        config.validate();
        assertThat(config.getName(), is("poolName"));
        assertThat(config.getDriverConfig(), is(driverConfig));
    }

    /**
     * Should fail validation on missing cloud pool name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingPoolName() {
        JsonObject driverConfig = JsonUtils.parseJsonString("{'config': 'value'}").getAsJsonObject();
        CloudPoolConfig config = new CloudPoolConfig(null, driverConfig);
        config.validate();
    }

    /**
     * Should fail validation on missing driver config.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingDriverConfig() {
        JsonObject driverConfig = null;
        CloudPoolConfig config = new CloudPoolConfig("poolName", driverConfig);
        config.validate();
    }
}
