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
import com.elastisys.scale.cloudpool.api.restapi.impl.CloudPoolRestApiImpl;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link CloudPoolRestApiImpl} correctly persists a config
 * when set.
 */
public class TestCloudPoolConfigPersistence {

    private final CloudPool poolMock = mock(CloudPool.class);

    private final static String storageDir = "target/test/cloudpool/storage";

    @Before
    public void beforeTestMethod() throws IOException {
        FileUtils.deleteRecursively(new File(storageDir));
    }

    /**
     * Create a {@link CloudPoolRestApiImpl} with complete constructor argument
     * list.
     */
    @Test
    public void createWithCompleteArgs() {
        CloudPoolRestApiImpl cloudPoolApi = new CloudPoolRestApiImpl(this.poolMock, storageDir, "testconfig.json");
        assertThat(cloudPoolApi.getCloudPoolConfigPath(), is(Paths.get(storageDir, "testconfig.json")));
    }

    /**
     * Make sure a default configuration file name is used when no config file
     * name is explicitly given.
     */
    @Test
    public void createWithDefaults() {
        CloudPoolRestApiImpl cloudPoolApi = new CloudPoolRestApiImpl(this.poolMock, storageDir);
        assertThat(cloudPoolApi.getCloudPoolConfigPath(),
                is(Paths.get(storageDir, CloudPoolRestApiImpl.DEFAULT_CONFIG_FILE_NAME)));

    }

    /**
     * If storage directory does not exist, it is created.
     */
    @Test
    public void verifyThatStorageDirectoryGetsCreated() {
        // storage dir created
        assertFalse(new File(storageDir).exists());
        new CloudPoolRestApiImpl(this.poolMock, storageDir);
        assertTrue(new File(storageDir).exists());

        // If storage directory already exists, that should work fine as well.
        new CloudPoolRestApiImpl(this.poolMock, storageDir);
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
            new CloudPoolRestApiImpl(this.poolMock, privilegedAccessDir);
            fail("unexpectedly managed to create directory under /root");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertFalse(new File(privilegedAccessDir).exists());
    }

    /**
     * Make sure {@link CloudPoolRestApiImpl#storeConfig(JsonObject)} correctly
     * stores a configuration object under the storage directory.
     */
    @Test
    public void storeConfig() throws IOException {
        File destinationFile = new File(storageDir, "conf.json");
        assertFalse(destinationFile.exists());

        String config = "{\"someKey\": \"someValue\"}";
        JsonObject jsonConfig = JsonUtils.parseJsonString(config).getAsJsonObject();
        CloudPoolRestApiImpl cloudPoolApi = new CloudPoolRestApiImpl(this.poolMock, storageDir, "conf.json");
        cloudPoolApi.storeConfig(jsonConfig);
        assertTrue(destinationFile.exists());
        assertThat(JsonUtils.parseJsonFile(destinationFile).getAsJsonObject(), is(jsonConfig));
    }

    /**
     * Make sure that the {@link CloudPoolRestApiImpl#setConfig(JsonObject)}
     * both sets the config on the {@link CloudPool} and stores the
     * configuration under the storage directory.
     */
    @Test
    public void setConfig() {
        File destinationFile = new File(storageDir, "conf.json");
        assertFalse(destinationFile.exists());

        String config = "{\"someKey\": \"someValue\"}";
        JsonObject jsonConfig = JsonUtils.parseJsonString(config).getAsJsonObject();
        CloudPoolRestApiImpl cloudPoolApi = new CloudPoolRestApiImpl(this.poolMock, storageDir, "conf.json");
        cloudPoolApi.setConfig(jsonConfig);

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

        CloudPoolRestApiImpl cloudPoolApi = new CloudPoolRestApiImpl(this.poolMock, storageDir, "conf.json");
        Response response = cloudPoolApi.setConfig(jsonConfig);
        assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));

        // verify that configure was called on cloud pool
        verify(this.poolMock).configure(jsonConfig);
        // verify that config was not saved
        assertFalse(destinationFile.exists());
    }
}
