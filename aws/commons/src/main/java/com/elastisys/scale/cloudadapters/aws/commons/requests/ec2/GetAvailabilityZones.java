package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;

/**
 * A {@link Callable} task that, when executed, requests a listing of AWS
 * availability zones for a given region.
 * 
 * 
 * 
 */
public class GetAvailabilityZones extends
		AmazonEc2Request<List<AvailabilityZone>> {

	public GetAvailabilityZones(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
	}

	@Override
	public List<AvailabilityZone> call() {
		DescribeAvailabilityZonesResult result = getClient().getApi()
				.describeAvailabilityZones();
		return result.getAvailabilityZones();
	}

}
