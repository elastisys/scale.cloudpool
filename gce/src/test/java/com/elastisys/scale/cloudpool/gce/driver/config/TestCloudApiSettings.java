package com.elastisys.scale.cloudpool.gce.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Exercise {@link CloudApiSettings}.
 */
public class TestCloudApiSettings {

    /**
     * It should be possible to pass serviceAccountKey as an inline JSON
     * document.
     */
    @Test
    public void withServiceAccountKey() {
        CloudApiSettings settings = new CloudApiSettings(null,
                loadServiceAccountKey("config/valid-service-account-key.json"));

        settings.validate();

        assertThat(settings.getApiCredential(), is(notNullValue()));
    }

    /**
     * It should be possible to pass serviceAccountKey as a file system path.
     */
    @Test
    public void withServiceAccountKeyPath() {
        CloudApiSettings settings = new CloudApiSettings("src/test/resources/config/valid-service-account-key.json",
                null);

        settings.validate();

        assertThat(settings.getApiCredential(), is(notNullValue()));

    }

    /**
     * It should not be allowed to pass both an inline service account key and a
     * file path.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withBothKeyAndKeyPath() {
        String serviceAccountKeyPath = "src/test/resources/config/valid-service-account-key.json";
        JsonObject loadServiceAccountKey = loadServiceAccountKey("config/valid-service-account-key.json");
        new CloudApiSettings(serviceAccountKeyPath, loadServiceAccountKey).validate();
    }

    /**
     * At least one of service account key and path must be given.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNoKey() {
        String serviceAccountKeyPath = null;
        JsonObject loadServiceAccountKey = null;
        new CloudApiSettings(serviceAccountKeyPath, loadServiceAccountKey).validate();
    }

    /**
     * {@link CloudApiSettings} should not pass validation if the service
     * account key path does not exist.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNonExistingServiceAccountKeyPath() {
        new CloudApiSettings("/illegal/path/service-account-key.json", null).validate();
    }

    /**
     * {@link CloudApiSettings} should not pass validation if the key is
     * illegal.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withInvalidKey() {
        new CloudApiSettings("src/test/resources/config/illegal-service-account-key.json", null).validate();
    }

    public static JsonObject loadServiceAccountKey(String resourcePath) {
        return JsonUtils.parseJsonResource(resourcePath).getAsJsonObject();
    }
}
