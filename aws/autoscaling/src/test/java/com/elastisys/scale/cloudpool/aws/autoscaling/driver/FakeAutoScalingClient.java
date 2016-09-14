package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.elastisys.scale.cloudpool.aws.autoscaling.driver.TestUtils.group;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.google.common.collect.Lists;

/**
 * Fake {@link AutoScalingClient} that manages an Auto Scaling Group for a phony
 * AWS account.
 */
public class FakeAutoScalingClient implements AutoScalingClient {
    private static final Logger LOG = LoggerFactory.getLogger(FakeAutoScalingClient.class);

    protected int idSequencer = 0;

    protected String autoScalingGroupName;
    protected LaunchConfiguration launchConfig;
    protected int desiredCapacity;
    /** The current Auto Scaling Group members. */
    protected List<Instance> memberInstances;
    /** The list of all {@link Machine}s available in the fake cloud account. */
    protected List<Instance> allInstances;

    /**
     * Creates a new {@link FakeAutoScalingClient} where the collection of Auto
     * Scaling Group members are all instances in the cloud account.
     *
     * @param autoScalingGroupName
     * @param launchConfig
     * @param desiredCapacity
     * @param groupMembers
     */
    public FakeAutoScalingClient(String autoScalingGroupName, LaunchConfiguration launchConfig, int desiredCapacity,
            List<Instance> groupMembers) {
        this(autoScalingGroupName, launchConfig, desiredCapacity, groupMembers, new ArrayList<Instance>());
    }

    /**
     * Creates a new {@link FakeAutoScalingClient} where the collection of Auto
     * Scaling Group members may differ from the total set of instances in the
     * cloud account.
     *
     * @param autoScalingGroupName
     * @param launchConfig
     * @param desiredCapacity
     * @param groupMmembers
     * @param nonGroupMembers
     */
    public FakeAutoScalingClient(String autoScalingGroupName, LaunchConfiguration launchConfig, int desiredCapacity,
            List<Instance> groupMmembers, List<Instance> nonGroupMembers) {
        this.autoScalingGroupName = autoScalingGroupName;
        this.launchConfig = launchConfig;
        this.desiredCapacity = desiredCapacity;
        this.memberInstances = Lists.newArrayList(groupMmembers);

        this.allInstances = Lists.newArrayList(nonGroupMembers);
        this.allInstances.addAll(groupMmembers);

        this.idSequencer = this.allInstances.size();
    }

    @Override
    public void configure(AwsAsPoolDriverConfig configuration) {
    }

    @Override
    public AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) {
        return group(autoScalingGroupName, this.launchConfig, this.desiredCapacity, this.memberInstances);
    }

    @Override
    public LaunchConfiguration getLaunchConfiguration(String launchConfigurationName) {
        checkArgument(launchConfigurationName.equals(this.launchConfig.getLaunchConfigurationName()),
                "unknown launch configuration '%s'", launchConfigurationName);
        return this.launchConfig;
    }

    @Override
    public List<Instance> getAutoScalingGroupMembers(String autoScalingGroupName) {
        return Lists.newArrayList(this.memberInstances);
    }

    /**
     * Sets the desired capacity of the auto scaling group. Note that to have
     * the group reach the desired size, the {@link #updateToDesiredSize()} can
     * be called.
     */
    @Override
    public void setDesiredSize(String autoScalingGroupName, int desiredSize) {
        this.desiredCapacity = desiredSize;
        LOG.debug(
                "desired size now {}, actual Auto Scaling Group size {}"
                        + ", changes will take effect on a call to updateToDesiredSize()",
                desiredSize, this.memberInstances.size());
    }

    /**
     * Updates the group by adding/removing instances to reach the set desired
     * capacity for the group ({@link #setDesiredSize(String, int)}.
     */
    public void updateToDesiredSize() {
        int delta = this.memberInstances.size() - this.desiredCapacity;
        if (delta > 0) {
            // add instance(s) to group
            for (int i = 0; i < delta; i++) {
                int idNum = ++this.idSequencer;
                Instance newInstance = new Instance().withInstanceId("i-" + idNum)
                        .withState(new InstanceState().withName("pending")).withPublicIpAddress("1.2.3." + idNum);
                this.allInstances.add(newInstance);
                this.memberInstances.add(newInstance);
            }
        } else if (delta < 0) {
            // remove instance(s) from group
            int toTerminate = -delta;
            for (int i = 0; i < toTerminate; i++) {
                terminateInstance(this.autoScalingGroupName, this.memberInstances.get(0).getInstanceId());
            }
        }
    }

    @Override
    public void terminateInstance(String autoScalingGroupName, String instanceId) throws NotFoundException {
        Instance instance = getInstanceOrFail(instanceId);
        this.allInstances.remove(instance);
        if (this.memberInstances.remove(instance)) {
            this.desiredCapacity--;
        }
    }

    @Override
    public void attachInstance(String autoScalingGroupName, String instanceId) throws NotFoundException {
        // verify existance
        Instance instance = getInstanceOrFail(instanceId);

        this.memberInstances.add(instance);
        this.desiredCapacity++;
    }

    @Override
    public void detachInstance(String autoScalingGroupName, String instanceId) throws NotFoundException {
        // verify existance
        Instance instance = getInstanceOrFail(instanceId);

        if (this.memberInstances.remove(instance)) {
            this.desiredCapacity--;
        }
    }

    @Override
    public void tagInstance(String instanceId, List<Tag> tags) throws NotFoundException {
        // verify group member
        Instance instance = getInstanceOrFail(instanceId);
        instance.getTags().addAll(tags);
    }

    private Instance getInstanceOrFail(String instanceId) throws NotFoundException {
        LOG.debug("all instances: {}", this.allInstances);
        for (Instance instance : this.allInstances) {
            if (instanceId.equals(instance.getInstanceId())) {
                return instance;
            }
        }
        throw new NotFoundException(String.format("no instance with id %s in Auto Scaling Group", instanceId));
    }

}
