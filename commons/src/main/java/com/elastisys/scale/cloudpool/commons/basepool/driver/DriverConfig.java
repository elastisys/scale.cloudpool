package com.elastisys.scale.cloudpool.commons.basepool.driver;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Represents settings for a {@link CloudPoolDriver}. These settings get passed
 * to a {@link CloudPoolDriver} implementation whenever a new configuration has
 * been set for its parent {@link BaseCloudPool}.
 *
 * @see CloudPoolDriver#configure(DriverConfig)
 */
public class DriverConfig {

    /**
     * The logical name of the managed group of machines. It is intended to be
     * used for purposes of identifying pool members. The exact way of
     * identifying pool members may differ between {@link CloudPoolDriver}
     * implementations, but machine tags could, for example, be used to mark
     * pool membership. Required.
     */
    private final String poolName;

    /**
     * API access credentials and settings required to communicate with the
     * targeted cloud. The structure of this document is cloud-specific and its
     * contents need to be validated by the {@link CloudPoolDriver}. Required.
     */
    private final JsonObject cloudApiSettings;

    /**
     * Describes how to provision additional servers (on scale-out). The
     * structure of this document is cloud-specific and its contents need to be
     * validated by the {@link CloudPoolDriver}. Required.
     */
    private final JsonObject provisioningTemplate;

    /**
     * Creates a {@link DriverConfig}.
     *
     * @param poolName
     *            The logical name of the managed group of machines. It is
     *            intended to be used for purposes of identifying pool members.
     *            The exact way of identifying pool members may differ between
     *            {@link CloudPoolDriver} implementations, but machine tags
     *            could, for example, be used to mark pool membership. Required.
     * @param cloudApiSettings
     *            API access credentials and settings required to communicate
     *            with the targeted cloud. The structure of this document is
     *            cloud-specific and its contents need to be validated by the
     *            {@link CloudPoolDriver}. Required.
     * @param provisioningTemplate
     *            Describes how to provision additional servers (on scale-out).
     *            The structure of this document is cloud-specific and its
     *            contents need to be validated by the {@link CloudPoolDriver}.
     *            Required.
     */
    public DriverConfig(String poolName, JsonObject cloudApiSettings, JsonObject provisioningTemplate) {
        this.poolName = poolName;
        this.cloudApiSettings = cloudApiSettings;
        this.provisioningTemplate = provisioningTemplate;
    }

    /**
     * The logical name of the managed group of machines. It is intended to be
     * used for purposes of identifying pool members. The exact way of
     * identifying pool members may differ between {@link CloudPoolDriver}
     * implementations, but machine tags could, for example, be used to mark
     * pool membership.
     *
     * @return
     */
    public String getPoolName() {
        return this.poolName;
    }

    /**
     * API access credentials and settings required to communicate with the
     * targeted cloud. The structure of this document is cloud-specific and its
     * contents need to be validated by the {@link CloudPoolDriver}.
     *
     * @return
     */
    public JsonObject getCloudApiSettings() {
        return this.cloudApiSettings;
    }

    /**
     * Parses and deserializes the JSON {@link #cloudApiSettings} into a given
     * Java type. Note: the client code is responsible for validating the
     * contents of the returned object.
     *
     * @param provisioningTemplateType
     * @return
     * @throws IllegalArgumentException
     *             on parse failure
     */
    public <T> T parseCloudApiSettings(Class<T> cloudApiSettingsType) throws IllegalArgumentException {
        try {
            return JsonUtils.toObject(getCloudApiSettings(), cloudApiSettingsType);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to parse provisioningTemplate: " + e.getMessage(), e);
        }
    }

    /**
     * Describes how to provision additional servers (on scale-out). The
     * structure of this document is cloud-specific and its contents need to be
     * validated by the {@link CloudPoolDriver}.
     *
     * @return
     */
    public JsonObject getProvisioningTemplate() {
        return this.provisioningTemplate;
    }

    /**
     * Parses and deserializes the JSON {@link #provisioningTemplate} into a
     * given Java type. Note: the client code is responsible for validating the
     * contents of the returned object.
     *
     * @param provisioningTemplateType
     * @return
     * @throws IllegalArgumentException
     *             on parse failure
     *
     */
    public <T> T parseProvisioningTemplate(Class<T> provisioningTemplateType) throws IllegalArgumentException {
        try {
            return JsonUtils.toObject(getProvisioningTemplate(), provisioningTemplateType);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to parse provisioningTemplate: " + e.getMessage(), e);
        }
    }

    /**
     * Performs basic validation of this configuration. <i>Note that the
     * contents of the cloud-specific parts of the {@link DriverConfig} need to
     * be validated by the {@link CloudPoolDriver} implementation.</i>
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.poolName != null, "driverConfig: missing name");
        checkArgument(this.cloudApiSettings != null, "driverConfig: missing cloudApiSettings");
        checkArgument(this.provisioningTemplate != null, "driverConfig: missing provisioningTemplate");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.poolName, this.cloudApiSettings, this.provisioningTemplate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DriverConfig) {
            DriverConfig that = (DriverConfig) obj;
            return Objects.equals(this.poolName, that.poolName) //
                    && Objects.equals(this.cloudApiSettings, that.cloudApiSettings) //
                    && Objects.equals(this.provisioningTemplate, that.provisioningTemplate);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}