package com.elastisys.scale.cloudpool.aws.commons.poolclient.impl;

import java.util.Collection;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CancelSpotInstanceRequests;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetSpotInstanceRequest;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetSpotInstanceRequests;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.PlaceSpotInstanceRequests;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;

public class AwsSpotClient extends AwsEc2Client implements SpotClient {

    public AwsSpotClient() {
        super();
    }

    @Override
    public SpotInstanceRequest getSpotInstanceRequest(String spotInstanceRequestId) throws AmazonClientException {
        return new GetSpotInstanceRequest(awsCredentials(), region(), clientConfig(), spotInstanceRequestId).call();
    }

    @Override
    public List<SpotInstanceRequest> getSpotInstanceRequests(Collection<Filter> filters) throws AmazonClientException {
        return new GetSpotInstanceRequests(awsCredentials(), region(), clientConfig(), filters).call();
    }

    @Override
    public List<SpotInstanceRequest> placeSpotRequests(double bidPrice, ScaleOutConfig scaleOutConfig, int count,
            List<Tag> tags) {
        // no particular availability zone
        String availabilityZone = null;
        PlaceSpotInstanceRequests request = new PlaceSpotInstanceRequests(awsCredentials(), region(), clientConfig(),
                bidPrice, availabilityZone, scaleOutConfig.getSecurityGroups(), scaleOutConfig.getKeyPair(),
                scaleOutConfig.getSize(), scaleOutConfig.getImage(), scaleOutConfig.getEncodedUserData(), count, tags);
        return request.call();
    }

    @Override
    public void cancelSpotRequests(List<String> spotInstanceRequestIds) {
        new CancelSpotInstanceRequests(awsCredentials(), region(), clientConfig(), spotInstanceRequestIds).call();
    }

}
