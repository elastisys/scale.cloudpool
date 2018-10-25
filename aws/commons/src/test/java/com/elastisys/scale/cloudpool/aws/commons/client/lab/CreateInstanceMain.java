package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CreateInstances;
import com.elastisys.scale.commons.util.collection.Maps;

public class CreateInstanceMain extends AbstractClient {

    // TODO: set to region where you want machine to be hosted
    private static final String region = "us-east-1";

    // TODO: set to an ec2 instance type
    private static final String instanceType = "t2.micro";
    // TODO: set to an AMI (amazon machine image) id
    private static final String amiId = "ami-da05a4a0";
    // TODO: set to one of your EC2 key pairs
    private static final String keyPair = "itestkeys";
    // TODO: set to IAM instance profile ARN
    private static final String iamInstanceProfileArn = null;

    // TODO: set to subnet ids (assumed to belong to the same VPC)
    private static final List<String> subnetIds = Arrays.asList("subnet-8621b58a", "subnet-9e1c4eb5");
    // TODO: indicate if public IP is to be assigned
    private static final boolean assignPublicIp = true;

    // TODO: set to ids of security groups belonging to the VPC of the subnets
    private static final List<String> securityGroupIds = Arrays.asList("sg-cc5957ab");

    // TODO: set to a user data boot script
    private static final String encodedUserData = "IyEvYmluL2Jhc2gKCmFwdCB1cGRhdGUgLXF5ICYmIGFwdCBpbnN0YWxsIGFwYWNoZTIgLXF5CgpmdW5jdGlvbiBkZWZhdWx0X2lwIHsKICAgIGRlZmF1bHRfbmV0d29ya19pbnRlcmZhY2U9JChyb3V0ZSB8IGdyZXAgZGVmYXVsdCB8IGF3ayAne3ByaW50ICQ4fScpCiAgICBpcD0kKGlmY29uZmlnICR7ZGVmYXVsdF9uZXR3b3JrX2ludGVyZmFjZX0gfCBncmVwICdpbmV0IGFkZHInIHwgYXdrICd7cHJpbnQgJDJ9JyB8IGF3ayAtRiA6ICd7cHJpbnQgJDJ9JykKICAgIGVjaG8gIiR7aXB9Igp9CgpvdXRwdXRfcGF0aD0vdmFyL3d3dy9odG1sL2luZGV4Lmh0bWwKCmNhdCA+ICR7b3V0cHV0X3BhdGh9IDw8RU9GCjxodG1sPgo8Ym9keT4KSGVsbG8hIEkgYW0gJChkZWZhdWx0X2lwKSEKPC9ib2R5Pgo8L2h0bWw+CkVPRgo=";

    // TODO: indicate if instance is to be EBS-optimized (not applicable to all
    // instance types)
    private static final boolean ebsOptimized = false;

    // TODO: set instance tags
    private static final Map<String, String> tags = Maps.of("cluster", "mycluster");

    public static void main(String[] args) throws Exception {
        logger.info(format("Starting instance in region %s ...", region));

        Ec2ProvisioningTemplate instanceTemplate = new Ec2ProvisioningTemplate(instanceType, amiId, subnetIds,
                assignPublicIp, keyPair, iamInstanceProfileArn, securityGroupIds, encodedUserData, ebsOptimized, tags);

        CreateInstances request = new CreateInstances(AWS_CREDENTIALS, region, new ClientConfiguration(),
                instanceTemplate, 1);
        List<Instance> instances = request.call();
        Instance instance = instances.get(0);
        logger.info("Launched instance : " + instance.getInstanceId() + ": " + instance.getState());
    }
}
