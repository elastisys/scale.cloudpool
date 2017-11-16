package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * A {@link Callable} task that, when executed, terminates a collection of EC2
 * instances and waits for the instances to appear terminating/terminated in
 * {@code DescribeInstances}, which may not be immediate due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API.
 */
public class TerminateInstances extends AmazonEc2Request<TerminateInstancesResult> {

    /** Initial exponential back-off delay in ms. */
    private static final int INITIAL_BACKOFF_DELAY = 1000;
    /** Maximum number of retries of operations. */
    private static final int MAX_RETRIES = 8;

    /** The identifiers of the instances to be terminated. */
    private final List<String> instanceIds;

    public TerminateInstances(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            String... instanceIds) {
        this(awsCredentials, region, clientConfig, ImmutableList.copyOf(instanceIds));
    }

    public TerminateInstances(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            Collection<String> instanceIds) {
        super(awsCredentials, region, clientConfig);
        this.instanceIds = ImmutableList.copyOf(instanceIds);
    }

    @Override
    public TerminateInstancesResult call() {
        TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(this.instanceIds);
        TerminateInstancesResult result = getClient().getApi().terminateInstances(request);

        awaitTermination(this.instanceIds);

        return result;
    }

    private void awaitTermination(List<String> instanceIds) {
        String name = String.format("await-terminal-state{%s}", instanceIds);

        Callable<List<Instance>> stateRequester = new GetInstances(getAwsCredentials(), getRegion(), getClientConfig(),
                instanceIds);
        Retryable<List<Instance>> retryer = Retryers.exponentialBackoffRetryer(name, stateRequester,
                INITIAL_BACKOFF_DELAY, TimeUnit.MILLISECONDS, MAX_RETRIES,
                allInAnyOfStates("shutting-down", "terminated"));

        try {
            retryer.call();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("gave up waiting for instances to terminate %s: %s", instanceIds, e.getMessage()), e);
        }
    }

    private Predicate<List<Instance>> allInAnyOfStates(String... acceptableStates) {
        return instances -> InstancePredicates.allInAnyOfStates(acceptableStates).test(instances);
    }

}
