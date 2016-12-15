package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolFetchConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolUpdateConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

public class TestUtils {

    public static BaseCloudPoolConfig config(String scalingGroupName) {
        AwsAsPoolDriverConfig awsApiConfig = new AwsAsPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey",
                "eu-west-1");
        CloudPoolConfig scalingGroupConfig = new CloudPoolConfig(scalingGroupName,
                JsonUtils.toJson(awsApiConfig).getAsJsonObject());
        JsonObject scaleOutConfig = new JsonObject();
        ScaleInConfig scaleDownConfig = new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);

        SmtpAlerterConfig smtpAlerter = new SmtpAlerterConfig(Arrays.asList("receiver@destination.com"),
                "noreply@elastisys.com", "cloud pool alert!", "INFO|WARN|ERROR|FATAL",
                new SmtpClientConfig("smtp.host.com", 25, null, false));
        List<HttpAlerterConfig> httpAlerters = Arrays.asList();
        AlertersConfig alertSettings = new AlertersConfig(Arrays.asList(smtpAlerter), httpAlerters);

        TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
        TimeInterval reachabilityTimeout = new TimeInterval(5L, TimeUnit.MINUTES);
        PoolFetchConfig poolFetch = new PoolFetchConfig(new RetriesConfig(3, new TimeInterval(2L, TimeUnit.SECONDS)),
                refreshInterval, reachabilityTimeout);
        PoolUpdateConfig poolUpdate = new PoolUpdateConfig(new TimeInterval(60L, TimeUnit.SECONDS));
        return new BaseCloudPoolConfig(scalingGroupConfig, scaleOutConfig, scaleDownConfig, alertSettings, poolFetch,
                poolUpdate);
    }

    public static AutoScalingGroup group(String name, LaunchConfiguration launchConfig, int desiredCapacity,
            Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withAutoScalingGroupName(name)
                .withLaunchConfigurationName(launchConfig.getLaunchConfigurationName())
                .withDesiredCapacity(desiredCapacity).withInstances(toAsInstances(ec2Instances));
        return autoScalingGroup;
    }

    public static Collection<Instance> asInstances(Instance... instances) {
        return Lists.newArrayList(instances);
    }

    public static List<com.amazonaws.services.ec2.model.Instance> ec2Instances(
            com.amazonaws.services.ec2.model.Instance... instances) {
        return Lists.newArrayList(instances);
    }

    public static Collection<Machine> machines(Machine... machines) {
        return Lists.newArrayList(machines);
    }

    public static Collection<Instance> toAsInstances(
            Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
        List<Instance> asInstances = Lists.newArrayList();
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
