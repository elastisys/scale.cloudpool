package com.elastisys.scale.cloudpool.aws.autoscaling.driver.config;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * {@link AwsAsPoolDriver}-specific server provisioning template.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class ProvisioningTemplate {

    /**
     * The name of an existing Auto Scaling Group whose size is to be managed.
     * May be <code>null</code>. Default: the name of the cloud pool
     * ({@link BaseCloudPoolConfig#getName()}).
     */
    private final String autoScalingGroup;

    /**
     * Creates a new {@link ProvisioningTemplate}.
     *
     * @param autoScalingGroup
     *            The name of an existing Auto Scaling Group whose size is to be
     *            managed. May be <code>null</code>. Default: the name of the
     *            cloud pool ({@link BaseCloudPoolConfig#getName()}).
     */
    public ProvisioningTemplate(String autoScalingGroup) {
        this.autoScalingGroup = autoScalingGroup;
    }

    /**
     * The name of an existing Auto Scaling Group whose size is to be managed.
     * If absent, the name of the cloud pool
     * ({@link BaseCloudPoolConfig#getName()}) should be used as the name for
     * the Auto Scaling Group.
     *
     * @return
     */
    public Optional<String> getAutoScalingGroup() {
        return Optional.ofNullable(this.autoScalingGroup);
    }

    /**
     * Validates that this {@link ProvisioningTemplate} contains all mandatory
     * field.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.autoScalingGroup);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningTemplate) {
            ProvisioningTemplate that = (ProvisioningTemplate) obj;
            return Objects.equals(this.autoScalingGroup, that.autoScalingGroup);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}