package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.elastisys.scale.cloudpool.aws.commons.client.AmazonApiUtils;

public class PlaceMultiInstanceRequest extends BaseClient {
    private static final Logger LOG = LoggerFactory.getLogger(PlaceMultiInstanceRequest.class);

    public static void main(String[] args) {
        String region = "us-east-1";

        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);

        AmazonEC2 api = AmazonEC2ClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

        // no particular availability zone
        String availabilityZone = null;
        String instanceType = "t1.micro";
        String imageId = "ami-3cf8b154";
        List<String> securityGroups = Arrays.asList("webserver");
        String keyPair = "instancekey";

        String bootScript = String.join("\n",
                Arrays.asList("#!/bin/bash", "sudo apt-get update -qy", "sudo apt-get install -qy apache2"));
        int instanceCount = 50;
        String bidPrice = "0.001";

        SpotPlacement placement = new SpotPlacement().withAvailabilityZone(availabilityZone);
        LaunchSpecification launchSpec = new LaunchSpecification().withInstanceType(instanceType).withImageId(imageId)
                .withPlacement(placement).withSecurityGroups(securityGroups).withKeyName(keyPair)
                .withUserData(AmazonApiUtils.base64Encode(bootScript));
        RequestSpotInstancesRequest request = new RequestSpotInstancesRequest().withInstanceCount(instanceCount)
                .withType(SpotInstanceType.Persistent).withSpotPrice(bidPrice).withLaunchSpecification(launchSpec);
        RequestSpotInstancesResult result = api.requestSpotInstances(request);

        for (SpotInstanceRequest spotRequest : result.getSpotInstanceRequests()) {
            LOG.info("placed request: {}", spotRequest.getSpotInstanceRequestId());
        }
    }
}
