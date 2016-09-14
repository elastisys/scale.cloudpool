package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstances;

public class ListInstancesMain extends AbstractClient {

    /** TODO: set to the AWS region to connect to. */
    private static final String region = "eu-west-1";

    public static void main(String[] args) throws Exception {
        try {
            logger.info(format("Listing all instances in region %s", region));
            List<Instance> instances = new GetInstances(AWS_CREDENTIALS, region, new ClientConfiguration()).call();
            for (Instance instance : instances) {
                logger.debug("  instance " + instance);
            }
        } catch (AmazonServiceException e) {
            logger.debug("status code: " + e.getStatusCode());
            logger.debug("error code: " + e.getErrorCode());
            logger.error(e.getMessage(), e);
        }
    }
}
