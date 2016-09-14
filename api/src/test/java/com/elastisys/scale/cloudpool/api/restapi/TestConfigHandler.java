package com.elastisys.scale.cloudpool.api.restapi;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;

/**
 * Tests for the {@link ConfigHandler} response handler.
 */
public class TestConfigHandler {

    private final CloudPool poolMock = mock(CloudPool.class);

    private final static String storageDir = "target/test/cloudpool/storage";

    @Before
    public void beforeTestMethod() throws IOException {
        FileUtils.deleteRecursively(new File(storageDir));
    }

    /**
     * Create a {@link ConfigHandler} with complete constructor argument list.
     */
    @Test
    public void createWithExplicitArguments() {
        ConfigHandler configHandler = new ConfigHandler(this.poolMock, storageDir, "testconfig.json");
        assertThat(configHandler.getCloudPoolConfigPath(), is(Paths.get(storageDir, "testconfig.json")));
    }

    /**
     * Make sure a default configuration file name is used when no config file
     * name is explicitly given.
     */
    @Test
    public void createWithDefaultConfigFileName() {
        ConfigHandler configHandler = new ConfigHandler(this.poolMock, storageDir);
        assertThat(configHandler.getCloudPoolConfigPath(),
                is(Paths.get(storageDir, ConfigHandler.DEFAULT_CONFIG_FILE_NAME)));

    }

    /**
     * If storage directory does not exist, it is created.
     */
    @Test
    public void verifyThatStorageDirectoryGetsCreated() {
        // storage dir created
        assertFalse(new File(storageDir).exists());
        new ConfigHandler(this.poolMock, storageDir);
        assertTrue(new File(storageDir).exists());

        // If storage directory already exists, that should work fine as well.
        new ConfigHandler(this.poolMock, storageDir);
    }

    /**
     * An {@link Exception} should be raised on failure to create the storage
     * directory.
     */
    @Test
    public void failureToCreateStorageDirectory() {
        String privilegedAccessDir = "/root/cloudpool/storage";
        assertFalse(new File(privilegedAccessDir).exists());
        try {
            new ConfigHandler(this.poolMock, privilegedAccessDir);
            fail("unexpectedly managed to create directory under /root");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertFalse(new File(privilegedAccessDir).exists());
    }

    /**
     * Make sure {@link ConfigHandler#storeConfig(JsonObject)} correctly stores
     * a configuration object under the storage directory.
     */
    @Test
    public void storeConfig() throws IOException {
        File destinationFile = new File(storageDir, "conf.json");
        assertFalse(destinationFile.exists());

        String config = "{\"someKey\": \"someValue\"}";
        JsonObject jsonConfig = JsonUtils.parseJsonString(config).getAsJsonObject();
        ConfigHandler configHandler = new ConfigHandler(this.poolMock, storageDir, "conf.json");
        configHandler.storeConfig(jsonConfig);
        assertTrue(destinationFile.exists());
        assertThat(JsonUtils.parseJsonFile(destinationFile).getAsJsonObject(), is(jsonConfig));
    }

    /**
     * Make sure that the {@link ConfigHandler#setAndStoreConfig(JsonObject)}
     * both sets the config on the {@link CloudPool} and stores the
     * configuration under the storage directory.
     */
    @Test
    public void setAndStoreConfig() {
        File destinationFile = new File(storageDir, "conf.json");
        assertFalse(destinationFile.exists());

        String config = "{\"someKey\": \"someValue\"}";
        JsonObject jsonConfig = JsonUtils.parseJsonString(config).getAsJsonObject();
        ConfigHandler configHandler = new ConfigHandler(this.poolMock, storageDir, "conf.json");
        configHandler.setAndStoreConfig(jsonConfig);

        // verify that config was set on cloud pool
        verify(this.poolMock).configure(jsonConfig);
        // verify that config was saved
        assertTrue(destinationFile.exists());
        assertThat(JsonUtils.parseJsonFile(destinationFile).getAsJsonObject(), is(jsonConfig));
    }

    /**
     * If the {@link CloudPool#configure(JsonObject)} call fails, the
     * configuration is probably invalid and should not be stored.
     */
    @Test
    public void verifyThatConfigIsNotStoredOnCloudPoolError() {
        File destinationFile = new File(storageDir, "conf.json");
        assertFalse(destinationFile.exists());

        String config = "{\"someKey\": \"someValue\"}";
        JsonObject jsonConfig = JsonUtils.parseJsonString(config).getAsJsonObject();
        // cloud pool set up to fail on configure()
        doThrow(new RuntimeException("unexpected failure")).when(this.poolMock).configure(jsonConfig);

        ConfigHandler configHandler = new ConfigHandler(this.poolMock, storageDir, "conf.json");
        Response response = configHandler.setAndStoreConfig(jsonConfig);
        assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));

        // verify that configure was called on cloud pool
        verify(this.poolMock).configure(jsonConfig);
        // verify that config was not saved
        assertFalse(destinationFile.exists());
    }

}
