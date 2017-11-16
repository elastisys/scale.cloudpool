package com.elastisys.scale.cloudpool.aws.commons.poolclient;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.amazonaws.services.ec2.model.InstanceType;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * EC2-specific server provisioning template.
 *
 * @see BaseCloudPoolConfig#getProvisioningTemplate()
 */
public class Ec2ProvisioningTemplate {
    /**
     * The name of the instance type to launch. For example, {@code m1.medium}.
     */
    private final String instanceType;
    /**
     * The name of the Amazon machine image used to boot new instances. For
     * example, {@code ami-da05a4a0}.
     */
    private final String amiId;
    /**
     * The IDs of one or more VPC subnets into which created instances are to be
     * launched. The subnets also implicitly determine the availability zones
     * that instances can be launched into. At least one subnet must be
     * specified. A best effort attempt will be made to place instances evenly
     * across the subnets. <i>All subnets are assumed to belong to the same
     * VPC</i>.
     */
    private final List<String> subnetIds;
    /**
     * Indicates if created instances are to be assigned a public IP address.
     */
    private final Boolean assignPublicIp;

    /**
     * The name of the key pair to use for new instances. May be
     * <code>null</code>. If you do not specify a key pair, you can't connect to
     * the instance unless you choose an AMI that is configured to allow users
     * another way to log in.
     */
    private final String keyPair;

    /**
     * The ARN of an IAM instance profile to assign to created instances. May be
     * <code>null</code> if no IAM role is to be delegated to instances. For
     * example,
     * {@code arn:aws:iam::123456789012:instance-profile/my-iam-profile}
     */
    private final String iamInstanceProfileARN;

    /**
     * Zero or more security group IDs to apply to created instances. May be
     * <code>null</code>, in which case EC2 assigns a default security group.
     * <i>All security groups are assumed to belong to the VPC of the
     * {@link #subnetIds}.</i>.
     */
    private final List<String> securityGroupIds;
    /**
     * A <a href="http://tools.ietf.org/html/rfc4648">base 64-encoded</a> blob
     * of data used to pass custom data (such as a boot script) to started
     * instances. Refer to the <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html">AWS
     * documentation</a> for details. May be <code>null</code>.
     */
    private final String encodedUserData;

    /**
     * Indicates whether created instances are to be optimized for Amazon EBS
     * I/O. This optimization provides dedicated throughput to Amazon EBS and an
     * optimized configuration stack to provide optimal Amazon EBS I/O
     * performance. Note that this optimization isn't available with all
     * instance types. Additional usage charges apply when using an
     * EBS-optimized instance.
     */
    private final Boolean ebsOptimized;

    /**
     * Tags that are to be set on created instances in addition to the
     * {@link ScalingTags#CLOUD_POOL_TAG} and
     * {@link ScalingTags#INSTANCE_NAME_TAG} that are always set by the
     * cloudpool.
     */
    private final Map<String, String> tags;

    /**
     * Creates a new {@link Ec2ProvisioningTemplate} without cloud-specific
     * {@link #extensions}.
     *
     * @param instanceType
     *            The name of the instance type to launch. For example,
     *            {@code m1.medium}.
     * @param amiId
     *            The name of the Amazon machine image used to boot new servers.
     *            For example, {@code ami-da05a4a0}.
     * @param subnetIds
     *            The IDs of one or more VPC subnets into which created
     *            instances are to be launched. The subnets also implicitly
     *            determine the availability zones that instances can be
     *            launched into. At least one must be specified. A best effort
     *            attempt will be made to place instances evenly across the
     *            subnets. <i>All subnets are assumed to belong to the same
     *            VPC</i>.
     * @param assignPublicIp
     *            Indicates if created instances are to be assigned a public IP
     *            address. May be <code>null</code>, which is interpreted as
     *            <code>false</code>.
     * @param keyPair
     *            The name of the key pair to use for new instances. May be
     *            <code>null</code>. If you do not specify a key pair, you can't
     *            connect to the instance unless you choose an AMI that is
     *            configured to allow users another way to log in.
     * @param iamInstanceProfileARN
     *            The ARN of an IAM instance profile to assign to created
     *            instances. May be <code>null</code> if no IAM role is to be
     *            delegated to instances. For example,
     *            {@code arn:aws:iam::123456789012:instance-profile/my-iam-profile}
     * @param securityGroupIds
     *            Zero or more security group IDs to apply to created instances.
     *            May be <code>null</code>, in which case EC2 assigns a default
     *            security group. <i>All security groups are assumed to belong
     *            to the VPC of the {@link #subnetIds}.</i>.
     * @param encodedUserData
     *            A <a href="http://tools.ietf.org/html/rfc4648">base
     *            64-encoded</a> blob of data used to pass custom data (such as
     *            a boot script) to started instances. Refer to the <a href=
     *            "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html">AWS
     *            documentation</a> for details. May be <code>null</code>.
     * @param ebsOptimized
     *            Indicates whether created instances are to be optimized for
     *            Amazon EBS I/O. This optimization provides dedicated
     *            throughput to Amazon EBS and an optimized configuration stack
     *            to provide optimal Amazon EBS I/O performance. Note that this
     *            optimization isn't available with all instance types.
     *            Additional usage charges apply when using an EBS-optimized
     *            instance. May be <code>null</code>, which is interpreted as
     *            <code>false</code>.
     * @param tags
     *            Tags that are to be set on created instances in addition to
     *            the {@link ScalingTags#CLOUD_POOL_TAG} and
     *            {@link ScalingTags#INSTANCE_NAME_TAG} that are always set by
     *            the cloudpool. May be <code>null</code>.
     */
    public Ec2ProvisioningTemplate(String instanceType, String amiId, List<String> subnetIds, Boolean assignPublicIp,
            String keyPair, String iamInstanceProfileARN, List<String> securityGroupIds, String encodedUserData,
            Boolean ebsOptimized, Map<String, String> tags) {
        this.instanceType = instanceType;
        this.amiId = amiId;
        this.subnetIds = subnetIds;
        this.assignPublicIp = assignPublicIp;
        this.keyPair = keyPair;
        this.iamInstanceProfileARN = iamInstanceProfileARN;
        this.securityGroupIds = securityGroupIds;
        this.encodedUserData = encodedUserData;
        this.ebsOptimized = ebsOptimized;
        this.tags = tags;
    }

    /**
     * The name of the instance type to launch. For example, {@code m1.medium}.
     *
     * @return
     */
    public InstanceType getInstanceType() {
        return InstanceType.fromValue(this.instanceType);
    }

    /**
     * The name of the Amazon machine image used to boot new servers. For
     * example, {@code ami-da05a4a0}.
     *
     * @return
     */
    public String getAmiId() {
        return this.amiId;
    }

    /**
     * The IDs of one or more VPC subnets into which created instances are to be
     * launched. The subnets also implicitly determine the availability zones
     * that instances can be launched into. At least one must be specified. A
     * best effort attempt will be made to place instances evenly across the
     * subnets. <i>All subnets are assumed to belong to the same VPC</i>.
     *
     * @return
     */
    public List<String> getSubnetIds() {
        return this.subnetIds;
    }

    /**
     * Indicates if created instances are to be assigned a public IP address.
     *
     * @return
     */
    public boolean isAssignPublicIp() {
        return Optional.ofNullable(this.assignPublicIp).orElse(false);
    }

    /**
     * The name of the key pair to use for new instances. May be
     * <code>null</code>. If none is specified, you can't connect to the
     * instance unless you choose an AMI that is configured to allow users
     * another way to log in.
     *
     * @return
     */
    public String getKeyPair() {
        return this.keyPair;
    }

    /**
     * The ARN of an IAM instance profile to assign to created instances. May be
     * <code>null</code> if no IAM role is to be delegated to instances. For
     * example,
     * {@code arn:aws:iam::123456789012:instance-profile/my-iam-profile}
     *
     * @return
     */
    public String getIamInstanceProfileARN() {
        return this.iamInstanceProfileARN;
    }

    /**
     * Zero or more security group IDs to apply to created instances. If empty
     * EC2 will assign a default security group. <i>All security groups are
     * assumed to belong to the VPC of the {@link #subnetIds}.</i>.
     *
     * @return
     */
    public List<String> getSecurityGroupIds() {
        return Optional.ofNullable(this.securityGroupIds).orElse(Collections.emptyList());
    }

    /**
     * A <a href="http://tools.ietf.org/html/rfc4648">base 64-encoded</a> blob
     * of data used to pass custom data (such as a boot script) to started
     * instances. Refer to the <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html">AWS
     * documentation</a> for details. May be <code>null</code>.
     *
     * @return
     */
    public String getEncodedUserData() {
        return this.encodedUserData;
    }

    /**
     * Indicates whether created instances are to be optimized for Amazon EBS
     * I/O. This optimization provides dedicated throughput to Amazon EBS and an
     * optimized configuration stack to provide optimal Amazon EBS I/O
     * performance. Note that this optimization isn't available with all
     * instance types. Additional usage charges apply when using an
     * EBS-optimized instance.
     *
     * @return
     */
    public boolean isEbsOptimized() {
        return Optional.ofNullable(this.ebsOptimized).orElse(false);
    }

    /**
     * Tags that are to be set on created instances in addition to the
     * {@link ScalingTags#CLOUD_POOL_TAG} and
     * {@link ScalingTags#INSTANCE_NAME_TAG} that are always set by the
     * cloudpool.
     *
     * @return
     */
    public Map<String, String> getTags() {
        return Optional.ofNullable(this.tags).orElse(Collections.emptyMap());
    }

    /**
     * Performs basic validation of this configuration.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.instanceType != null, "provisioningTemplate: missing instanceType");
        try {
            InstanceType.fromValue(this.instanceType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("provisioingTemplate: unrecognized instanceType: %s", this.instanceType));
        }

        checkArgument(this.amiId != null, "provisioningTemplate: missing amiId");
        checkArgument(this.subnetIds != null, "provisioningTemplate: missing subnetIds");
        checkArgument(!this.subnetIds.isEmpty(), "provisioningTemplate: at least one subnet id must be specified");
    }

    /**
     * Returns a copy of this {@link Ec2ProvisioningTemplate} with an additional
     * tag set.
     *
     * @param tagKey
     * @param tagValue
     * @return
     */
    public Ec2ProvisioningTemplate withTag(String tagKey, String tagValue) {
        Map<String, String> tagsCopy = new HashMap<>(this.tags);
        tagsCopy.put(tagKey, tagValue);
        return new Ec2ProvisioningTemplate(this.instanceType, this.amiId, this.subnetIds, this.assignPublicIp,
                this.keyPair, this.iamInstanceProfileARN, this.securityGroupIds, this.encodedUserData,
                this.ebsOptimized, tagsCopy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.instanceType, this.amiId, this.subnetIds, this.assignPublicIp, this.keyPair,
                this.iamInstanceProfileARN, this.securityGroupIds, this.encodedUserData, this.ebsOptimized);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ec2ProvisioningTemplate) {
            Ec2ProvisioningTemplate that = (Ec2ProvisioningTemplate) obj;
            return Objects.equals(this.instanceType, that.instanceType) //
                    && Objects.equals(this.amiId, that.amiId) //
                    && Objects.equals(this.subnetIds, that.subnetIds) //
                    && Objects.equals(this.assignPublicIp, that.assignPublicIp) //
                    && Objects.equals(this.keyPair, that.keyPair) //
                    && Objects.equals(this.iamInstanceProfileARN, that.iamInstanceProfileARN) //
                    && Objects.equals(this.securityGroupIds, that.securityGroupIds) //
                    && Objects.equals(this.encodedUserData, that.encodedUserData) //
                    && Objects.equals(this.ebsOptimized, that.ebsOptimized);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}