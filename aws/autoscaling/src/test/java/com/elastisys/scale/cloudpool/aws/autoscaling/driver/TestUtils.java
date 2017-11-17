package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

public class TestUtils {

    /**
     * Create a sample {@link DriverConfig}.
     *
     * @param managedAutoScalingGroupName
     *            The managed Auto Scaling Group name to set in template. Mau be
     *            null, in which case the {@link DriverConfig#getPoolName()}
     *            will be used.
     * @return
     */
    public static DriverConfig driverConfig(String managedAutoScalingGroupName) {
        ProvisioningTemplate provisioningTemplate = new ProvisioningTemplate(managedAutoScalingGroupName);
        CloudApiSettings cloudApiSettings = new CloudApiSettings("awsAccessKeyId", "awsSecretAccessKey", "eu-west-1");
        return new DriverConfig("my-cloud-pool", JsonUtils.toJson(cloudApiSettings).getAsJsonObject(),
                JsonUtils.toJson(provisioningTemplate).getAsJsonObject());
    }

    public static AutoScalingGroup group(String name, LaunchConfiguration launchConfig, int desiredCapacity,
            Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withAutoScalingGroupName(name)
                .withLaunchConfigurationName(launchConfig.getLaunchConfigurationName())
                .withDesiredCapacity(desiredCapacity).withInstances(toAsInstances(ec2Instances));
        return autoScalingGroup;
    }

    public static Collection<Instance> asInstances(Instance... instances) {
        return new ArrayList<>(Arrays.asList(instances));
    }

    public static List<com.amazonaws.services.ec2.model.Instance> ec2Instances(
            com.amazonaws.services.ec2.model.Instance... instances) {
        return new ArrayList<>(Arrays.asList(instances));
    }

    public static Collection<Machine> machines(Machine... machines) {
        return new ArrayList<>(Arrays.asList(machines));
    }

    public static Collection<Instance> toAsInstances(
            Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
        List<Instance> asInstances = new ArrayList<>();
        for (com.amazonaws.services.ec2.model.Instance ec2Instance : ec2Instances) {
            Instance asInstance = new Instance().withInstanceId(ec2Instance.getInstanceId())
                    .withLifecycleState(ec2StateToLifecycleState(ec2Instance.getState()));
            asInstances.add(asInstance);
        }
        return asInstances;
    }

    private static LifecycleState ec2StateToLifecycleState(InstanceState state) {
        switch (state.getName()) {
        case "pending":
            return LifecycleState.Pending;
        case "running":
            return LifecycleState.InService;
        case "shutting-down":
            return LifecycleState.Terminating;
        case "terminated":
            return LifecycleState.Terminated;
        case "stopping":
            return LifecycleState.Terminating;
        case "stopped":
            return LifecycleState.Terminated;
        default:
            throw new IllegalArgumentException(String.format("unrecognized instance state: '%s'", state));
        }
    }

    public static com.amazonaws.services.ec2.model.Instance ec2Instance(String id, String state) {
        return new com.amazonaws.services.ec2.model.Instance().withInstanceId(id)
                .withInstanceType(InstanceType.M1Medium).withState(new InstanceState().withName(state));
    }

    public static com.amazonaws.services.ec2.model.Instance spotInstance(String spotId, String instanceId,
            String state) {
        return new com.amazonaws.services.ec2.model.Instance().withInstanceId(instanceId)
                .withInstanceType(InstanceType.M1Medium).withState(new InstanceState().withName(state))
                .withSpotInstanceRequestId(spotId);
    }

}
