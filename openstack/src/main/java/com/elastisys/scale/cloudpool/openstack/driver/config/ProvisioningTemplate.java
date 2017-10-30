package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * OpenStack-specific server provisioning template.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class ProvisioningTemplate {
    /** Default value when no explicit {@link #assignFloatingIp} is set. */
    static final Boolean DEFAULT_ASSIGN_FLOATING_IP = false;

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
     * The names of the networks to attach launched servers to (for example,
     * {@code private}). Each network creates a separate network interface
     * controller (NIC) on a created server. Typically, this option can be left
     * out, but in rare cases, when an account has more than one network to
     * choose from, the OpenStack API forces us to be explicit about the
     * network(s) we want to use.
     * <p/>
     * If set to <code>null</code> or left empty, the default behavior is to use
     * which ever network is configured by the cloud provider for the
     * user/project. However, if there are multiple choices, this will cause
     * server boot requests to fail.
     */
    private final List<String> networks;
    /**
     * Set to <code>true</code> if a floating IP address should be allocated to
     * launched servers. Default: <code>false</code>.
     */
    private final Boolean assignFloatingIp;

    /**
     * Creates a new {@link ProvisioningTemplate}.
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
    public ProvisioningTemplate(String size, String image, String keyPair, List<String> securityGroups,
            String encodedUserData) {
        this(size, image, keyPair, securityGroups, encodedUserData, null, DEFAULT_ASSIGN_FLOATING_IP);
    }

    /**
     * Creates a new {@link ProvisioningTemplate}.
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
     * @param networks
     *            The names of the networks to attach launched servers to (for
     *            example, {@code private}). Each network creates a separate
     *            network interface controller (NIC) on a created server.
     *            Typically, this option can be left out, but in rare cases,
     *            when an account has more than one network to choose from, the
     *            OpenStack API forces us to be explicit about the network(s) we
     *            want to use.
     *            <p/>
     *            If set to <code>null</code> or left empty, the default
     *            behavior is to use which ever network is configured by the
     *            cloud provider for the user/project. However, if there are
     *            multiple choices, this will cause server boot requests to
     *            fail.
     * @param assignFloatingIp
     *            Set to <code>true</code> if a floating IP address should be
     *            allocated to launched servers. Default: <code>false</code>.
     */
    public ProvisioningTemplate(String size, String image, String keyPair, List<String> securityGroups,
            String encodedUserData, List<String> networks, Boolean assignFloatingIp) {
        this.size = size;
        this.image = image;
        this.keyPair = keyPair;
        this.securityGroups = securityGroups;
        this.encodedUserData = encodedUserData;
        this.networks = networks;
        this.assignFloatingIp = assignFloatingIp;
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
        return Optional.ofNullable(this.securityGroups).orElse(Collections.emptyList());
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
     * The names of the networks to attach launched servers to (for example,
     * {@code private}). Each network creates a separate network interface
     * controller (NIC) on a created server. Typically, this option can be left
     * out, but in rare cases, when an account has more than one network to
     * choose from, the OpenStack API forces us to be explicit about the
     * network(s) we want to use.
     * <p/>
     * If set to <code>null</code> or left empty, the default behavior is to use
     * which ever network is configured by the cloud provider for the
     * user/project. However, if there are multiple choices, this will cause
     * server boot requests to fail.
     *
     * @return
     */
    public List<String> getNetworks() {
        return Optional.ofNullable(this.networks).orElse(Collections.emptyList());
    }

    /**
     * Returns <code>true</code> if a floating IP address should be allocated to
     * launched servers.
     *
     * @return
     */
    public Boolean isAssignFloatingIp() {
        return Optional.ofNullable(this.assignFloatingIp).orElse(DEFAULT_ASSIGN_FLOATING_IP);
    }

    /**
     * Validates that this {@link ProvisioningTemplate} contains all mandatory
     * field.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.size != null, "provisioningTemplate: missing size");
        checkArgument(this.image != null, "provisioningTemplate: missing image");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.size, this.image, this.keyPair, this.securityGroups, this.encodedUserData,
                this.networks, this.assignFloatingIp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningTemplate) {
            ProvisioningTemplate that = (ProvisioningTemplate) obj;
            return Objects.equals(this.size, that.size) //
                    && Objects.equals(this.image, that.image) //
                    && Objects.equals(this.keyPair, that.keyPair) //
                    && Objects.equals(this.securityGroups, that.securityGroups) //
                    && Objects.equals(this.encodedUserData, that.encodedUserData) //
                    && Objects.equals(this.networks, that.networks) //
                    && Objects.equals(this.assignFloatingIp, that.assignFloatingIp);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public JsonObject toJson() {
        return JsonUtils.toJson(this).getAsJsonObject();
    }

    public static Builder builder(String size, String image) {
        return new Builder(size, image);
    }

    /**
     * A builder for constructing {@link ProvisioningTemplate}s.
     */
    public static class Builder {
        private final String size;
        private final String image;
        private String keyPair;
        private List<String> securityGroups = new ArrayList<>();
        private String encodedUserData;
        private List<String> networks = new ArrayList<>();
        private Boolean assignFloatingIp;

        public Builder(String size, String image) {
            this.size = size;
            this.image = image;
        }

        public Builder keyPair(String keyPair) {
            this.keyPair = keyPair;
            return this;
        }

        public Builder securityGroup(String securityGroup) {
            this.securityGroups.add(securityGroup);
            return this;
        }

        public Builder userData(String encodedUserData) {
            this.encodedUserData = encodedUserData;
            return this;
        }

        public Builder network(String network) {
            this.networks.add(network);
            return this;
        }

        public Builder floatingIp(boolean shouldBeAssigned) {
            this.assignFloatingIp = shouldBeAssigned;
            return this;
        }

        public ProvisioningTemplate build() {
            return new ProvisioningTemplate(this.size, this.image, this.keyPair, this.securityGroups,
                    this.encodedUserData, this.networks, this.assignFloatingIp);
        }

    }
}