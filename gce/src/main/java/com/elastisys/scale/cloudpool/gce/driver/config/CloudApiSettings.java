package com.elastisys.scale.cloudpool.gce.driver.config;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.gce.driver.GcePoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.JsonObject;

/**
 * API access credentials and settings for a {@link GcePoolDriver}.
 * <p/>
 *
 */
public class CloudApiSettings {

    /**
     * Local file system path to a JSON-formatted service account key. May be
     * <code>null</code>, if {@link #serviceAccountKey} is given.
     */
    private final String serviceAccountKeyPath;
    /**
     * A JSON-formatted service account key. May be <code>null</code>, if
     * {@link #serviceAccountKeyPath} is given.
     */
    private final JsonObject serviceAccountKey;

    /**
     * Creates {@link CloudApiSettings}.
     *
     * @param serviceAccountKeyPath
     *            Local file system path to a JSON-formatted service account
     *            key. May be <code>null</code>, if {@link #serviceAccountKey}
     *            is given.
     * @param serviceAccountKey
     *            A JSON-formatted service account key. May be
     *            <code>null</code>, if {@link #serviceAccountKeyPath} is given.
     */
    public CloudApiSettings(String serviceAccountKeyPath, JsonObject serviceAccountKey) {
        this.serviceAccountKeyPath = serviceAccountKeyPath;
        this.serviceAccountKey = serviceAccountKey;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.serviceAccountKey != null || this.serviceAccountKeyPath != null,
                "cloudApiSettings: either serviceAccountKey or serviceAccountKeyPath must be given");
        checkArgument(this.serviceAccountKey != null ^ this.serviceAccountKeyPath != null,
                "cloudApiSettings: only one of serviceAccountKey or serviceAccountKeyPath may be specified");

        ensureParseableServiceAccountKey();
    }

    private void ensureParseableServiceAccountKey() {
        try {
            parseCredential();
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read service account key: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the service account key provided at creation-time and returns the
     * corresponding {@link GoogleCredential}.
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private GoogleCredential parseCredential() throws FileNotFoundException, IOException {
        InputStream keyStream = null;
        if (this.serviceAccountKeyPath != null) {
            File keyFile = new File(this.serviceAccountKeyPath);
            checkArgument(keyFile.isFile(), "serviceAccountKeyPath does not exist: %s", this.serviceAccountKeyPath);
            keyStream = new FileInputStream(keyFile);
        } else {
            keyStream = new ByteArrayInputStream(JsonUtils.toString(this.serviceAccountKey).getBytes());
        }

        return GoogleCredential.fromStream(keyStream);
    }

    /**
     * Returns a {@link GoogleCredential} for the configured service account
     * key.
     *
     * @return
     */
    public GoogleCredential getApiCredential() {
        try {
            return parseCredential();
        } catch (Exception e) {
            throw new CloudPoolDriverException("failed to extract service account key: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serviceAccountKey, this.serviceAccountKeyPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudApiSettings) {
            CloudApiSettings that = (CloudApiSettings) obj;
            return Objects.equals(this.serviceAccountKeyPath, that.serviceAccountKeyPath) //
                    && Objects.equals(this.serviceAccountKey, that.serviceAccountKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
