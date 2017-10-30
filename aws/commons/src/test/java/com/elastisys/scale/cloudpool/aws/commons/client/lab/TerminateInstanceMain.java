package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TerminateInstances;

public class TerminateInstanceMain extends AbstractClient {

    // TODO: set to region where machine to terminate is hosted
    private static final String region = "us-east-1";
    // TODO: set to instance id of machine to terminate
    private static final String instanceId = "i-6f1e490d";

    public static void main(String[] args) throws Exception {
        logger.info(format("Terminating instance %s in region %s", instanceId, region));
        TerminateInstancesResult result = new TerminateInstances(AWS_CREDENTIALS, region, new ClientConfiguration(),
                instanceId).call();
        logger.info("Terminating instances: {}", result);
    }

}
