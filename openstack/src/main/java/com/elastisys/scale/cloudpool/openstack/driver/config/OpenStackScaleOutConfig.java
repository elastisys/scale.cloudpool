package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * OpenStack-specific server provisioning template.
 *
 * @see BaseCloudPoolConfig#getScaleOutConfig()
 */
public class OpenStackScaleOutConfig {
    /** The name of the server type to launch. For example, m1.medium. */
    private final String size;
    /** The name of the machine image used to boot new servers. */
    private final String image;
    /**
     * The name of the key pair to use for new machine instances. May be
     * <code>null</code>.
     */
    private final String keyPair;
    /**
     * The security group(s) to use for new machine instances. May be
     * <code>null</code>.
     */
    private final List<String> securityGroups;
    /**
     * A <a href="http://tools.ietf.org/html/rfc4648">base 64-encoded</a> blob
     * of data used to pass custom data to started machines. It is supported by
     * many cloud providers and is typically used to pass a boot-up shell script
     * or cloud-init parameters to launched machines. May be <code>null</code>.
     */
    private final String encodedUserData;

    /**
     * Creates a new {@link OpenStackScaleOutConfig} without cloud-specific
     * {@link #extensions}.
     *
     * @param size
     *            The name of the server type to launch. For example, m1.medium.
     * @param image
     *            The name of the machine image used to boot new servers.
     * @param keyPair
     *            The name of the key pair to use for new machine instances. May
     *            be <code>null</code>.
     * @param securityGroups
     *            The security group(s) to use for new machine instances. May be
     *            <code>null</code>.
     * @param encodedUserData
     *            A <a href="http://tools.ietf.org/html/rfc4648">base
     *            64-encoded</a> blob of data used to pass custom data to
     *            started machines. It is supported by many cloud providers and
     *            is typically used to pass a boot-up shell script or cloud-init
     *            parameters to launched machines. May be <code>null</code>.
     */
    public OpenStackScaleOutConfig(String size, String image, String keyPair, List<String> securityGroups,
            String encodedUserData) {
        this.size = size;
        this.image = image;
        this.keyPair = keyPair;
        List<String> emptyList = ImmutableList.of();
        this.securityGroups = Optional.fromNullable(securityGroups).or(emptyList);
        this.encodedUserData = encodedUserData;
    }

    /**
     * The name of the server type to launch. For example, m1.medium.
     *
     * @return
     */
    public String getSize() {
        return this.size;
    }

    /**
     * The name of the machine image used to boot new servers.
     *
     * @return
     */
    public String getImage() {
        return this.image;
    }

    /**
     * The name of the key pair to use for new machine instances. May be
     * <code>null</code>.
     *
     * @return
     */
    public String getKeyPair() {
        return this.keyPair;
    }

    /**
     * The security group(s) to use for new machine instances. May be
     * <code>null</code>.
     *
     * @return
     */
    public List<String> getSecurityGroups() {
        return this.securityGroups;
    }

    /**
     * A <a href="http://tools.ietf.org/html/rfc4648">base 64-encoded</a> blob
     * of data used to pass custom data to started machines. It is supported by
     * many cloud providers and is typically used to pass a boot-up shell script
     * or cloud-init parameters to launched machines. May be <code>null</code>.
     *
     * @return
     */
    public String getEncodedUserData() {
        return this.encodedUserData;
    }

    /**
     * Performs basic validation of this configuration.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.size != null, "scaleOutConfig: missing size");
        checkArgument(this.image != null, "scaleOutConfig: missing image");
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.size, this.image, this.keyPair, this.securityGroups, this.encodedUserData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OpenStackScaleOutConfig) {
            OpenStackScaleOutConfig that = (OpenStackScaleOutConfig) obj;
            return equal(this.size, that.size) && equal(this.image, that.image) && equal(this.keyPair, that.keyPair)
                    && equal(this.securityGroups, that.securityGroups)
                    && equal(this.encodedUserData, that.encodedUserData);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}