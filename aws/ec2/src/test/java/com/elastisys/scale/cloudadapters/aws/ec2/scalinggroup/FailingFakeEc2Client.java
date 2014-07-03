package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;

/**
 * A {@link FakeEc2Client} that only allows a limited number of
 * {@link #launchInstance} calls before an error is raised.
 *
 * 
 *
 */
public class FailingFakeEc2Client extends FakeEc2Client {
	private final int numLaunchesBeforeFailure;
	private int numLaunches;

	public FailingFakeEc2Client(List<Instance> instances,
			int numLaunchesBeforeFailure) {
		super(instances);
		this.numLaunchesBeforeFailure = numLaunchesBeforeFailure;
		this.numLaunches = 0;
	}

	@Override
	public Instance launchInstance(ScaleUpConfig provisioningDetails) {
		this.numLaunches++;
		if (this.numLaunches > this.numLaunchesBeforeFailure) {
			throw new AmazonServiceException("failed to launch instance");
		}
		return super.launchInstance(provisioningDetails);
	}
}
