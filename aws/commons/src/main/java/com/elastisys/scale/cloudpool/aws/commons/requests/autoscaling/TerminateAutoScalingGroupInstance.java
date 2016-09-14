package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.inAnyOfStates;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudpool.aws.commons.client.Ec2ApiClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstance;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;

/**
 * A {@link Callable} task that, when executed, requests a particular Auto
 * Scaling Group member instance to be terminated. As a side-effect, the desired
 * capacity of the Auto Scaling Group is decremented. The task also waits for
 * the instance to appear terminating/terminated in {@code DescribeInstances}.
 * <p/>
 * Due to the eventual consistency semantics of the Amazon API, operations may
 * need time to propagate through the system and results may not be immediate.
 */
public class TerminateAutoScalingGroupInstance extends AmazonAutoScalingRequest<Void> {

    /** The machine instance identifier to be terminated. */
    private final String instanceId;

    public TerminateAutoScalingGroupInstance(AWSCredentials awsCredentials, String region,
            ClientConfiguration clientConfig, String instanceId) {
        super(awsCredentials, region, clientConfig);
        this.instanceId = instanceId;
    }

    @Override
    public Void call() {
        TerminateInstanceInAutoScalingGroupRequest request = new TerminateInstanceInAutoScalingGroupRequest()
                .withInstanceId(this.instanceId).withShouldDecrementDesiredCapacity(true);
        getClient().getApi().terminateInstanceInAutoScalingGroup(request);

        awaitTermination(this.instanceId);
        return null;
    }

    private void awaitTermination(String instanceId) {
        String name = String.format("await-terminal-state{%s}", instanceId);
        try (Ec2ApiClient ec2Client = new Ec2ApiClient(getAwsCredentials(), getRegion(), getClientConfig())) {
            Callable<Instance> stateRequester = new GetInstance(getAwsCredentials(), getRegion(), getClientConfig(),
                    instanceId);

            int initialDelay = 1;
            int maxRetries = 8;
            Retryable<Instance> retryer = Retryers.exponentialBackoffRetryer(name, stateRequester, initialDelay,
                    TimeUnit.SECONDS, maxRetries, inAnyOfStates("shutting-down", "terminated"));
            try {
                retryer.call();
            } catch (Exception e) {
                throw new RuntimeException(String.format("gave up waiting for instance to terminate: '%s': %s",
                        instanceId, e.getMessage()), e);
            }
        }
    }
}
