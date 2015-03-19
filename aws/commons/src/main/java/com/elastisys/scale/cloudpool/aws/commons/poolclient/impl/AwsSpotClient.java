package com.elastisys.scale.cloudpool.aws.commons.poolclient.impl;

import java.util.Collection;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CancelSpotInstanceRequest;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetSpotInstanceRequest;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetSpotInstanceRequests;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.PlaceSpotInstanceRequest;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.google.common.base.Joiner;

public class AwsSpotClient extends AwsEc2Client implements SpotClient {

	public AwsSpotClient() {
		super();
	}

	@Override
	public SpotInstanceRequest getSpotInstanceRequest(
			String spotInstanceRequestId) throws AmazonClientException {
		return new GetSpotInstanceRequest(awsCredentials(), region(),
				spotInstanceRequestId).call();
	}

	@Override
	public List<SpotInstanceRequest> getSpotInstanceRequests(
			Collection<Filter> filters) throws AmazonClientException {
		return new GetSpotInstanceRequests(awsCredentials(), region(), filters)
				.call();
	}

	@Override
	public SpotInstanceRequest placeSpotRequest(double bidPrice,
			ScaleOutConfig scaleOutConfig) {
		String bootscript = Joiner.on("\n")
				.join(scaleOutConfig.getBootScript());
		// no particular availability zone
		String availabilityZone = null;
		PlaceSpotInstanceRequest request = new PlaceSpotInstanceRequest(
				awsCredentials(), region(), bidPrice, availabilityZone,
				scaleOutConfig.getSecurityGroups(),
				scaleOutConfig.getKeyPair(), scaleOutConfig.getSize(),
				scaleOutConfig.getImage(), bootscript);
		return request.call();
	}

	@Override
	public void cancelSpotRequest(String spotInstanceRequestId) {
		new CancelSpotInstanceRequest(awsCredentials(), region(),
				spotInstanceRequestId).call();
	}

}
