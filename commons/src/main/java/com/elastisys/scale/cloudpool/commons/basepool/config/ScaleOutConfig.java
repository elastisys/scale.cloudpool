package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to provision
 * additional servers (on scale-out).
 *
 * @see BaseCloudPoolConfig
 */
public class ScaleOutConfig {
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
     * An extension element for cloud-specific functionality. A
     * {@link CloudPoolDriver} may choose to parse this section of the
     * provisioning template for cloud-specific behavior. May be
     * <code>null</code>.
     */
    private final JsonObject extensions;

    /**
     * Creates a new {@link ScaleOutConfig} without cloud-specific
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
    public ScaleOutConfig(String size, String image, String keyPair, List<String> securityGroups,
            String encodedUserData) {
        this(size, image, keyPair, securityGroups, encodedUserData, null);
    }

    /**
     * Creates a new {@link ScaleOutConfig} with (optional) cloud-specific
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
     * @param extensions
     *            An extension element for cloud-specific functionality. A
     *            {@link CloudPoolDriver} may choose to parse this section of
     *            the provisioning template for cloud-specific behavior. May be
     *            <code>null</code>.
     */
    public ScaleOutConfig(String size, String image, String keyPair, List<String> securityGroups,
            String encodedUserData, JsonObject extensions) {
        this.size = size;
        this.image = image;
        this.keyPair = keyPair;
        List<String> emptyList = ImmutableList.of();
        this.securityGroups = Optional.fromNullable(securityGroups).or(emptyList);
        this.encodedUserData = encodedUserData;
        this.extensions = extensions;
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
     * An extension element for cloud-specific functionality. A
     * {@link CloudPoolDriver} may choose to parse this section of the
     * provisioning template for cloud-specific behavior. May be
     * <code>null</code>.
     *
     * @return
     */
    public JsonObject getExtensions() {
        return this.extensions;
    }

    /**
     * Performs basic validation of this configuration.
     *
     * @throws CloudPoolException
     */
    public void validate() throws CloudPoolException {
        try {
            checkNotNull(this.size, "missing size");
            checkNotNull(this.image, "missing image");
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to validate scaleOutConfig: %s", e.getMessage()), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.size, this.image, this.keyPair, this.securityGroups, this.encodedUserData,
                this.extensions);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScaleOutConfig) {
            ScaleOutConfig that = (ScaleOutConfig) obj;
            return equal(this.size, that.size) && equal(this.image, that.image) && equal(this.keyPair, that.keyPair)
                    && equal(this.securityGroups, that.securityGroups)
                    && equal(this.encodedUserData, that.encodedUserData) && equal(this.extensions, that.extensions);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}