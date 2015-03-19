package com.elastisys.scale.cloudpool.aws.commons.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Function;

/**
 * Exercises the {@link AwsEc2Functions}.
 *
 *
 */
public class TestAwsEc2Functions {

	@Test
	public void testToInstanceId() {
		assertThat(AwsEc2Functions.toInstanceId()
				.apply(ec2Instance("i-123456")), is("i-123456"));
	}

	@Test
	public void testToSpotRequestId() {
		assertThat(
				AwsEc2Functions.toSpotRequestId().apply(
						spotInstanceRequest("sir-1234567")), is("sir-1234567"));
	}

	@Test
	public void testRemainingBillingHourTime() {
		// "current time"
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:30:00Z"));

		Function<Instance, Long> remainingTimeFunction = AwsEc2Functions
				.remainingBillingHourTime();

		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T10:35:00Z"))), is(5 * 60L));
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T11:35:00Z"))), is(5 * 60L));
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T11:50:00Z"))), is(20 * 60L));
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T12:00:00Z"))), is(30 * 60L));
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T12:15:00Z"))), is(45 * 60L));

		// edge case
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T12:30:00Z"))), is(3600L));
		assertThat(remainingTimeFunction.apply(ec2Instance(DateTime
				.parse("2013-06-01T11:30:00Z"))), is(3600L));
	}

	private SpotInstanceRequest spotInstanceRequest(String withId) {
		return new SpotInstanceRequest().withSpotInstanceRequestId(withId);
	}

	private Instance ec2Instance(String withInstanceId) {
		return new Instance().withInstanceId(withInstanceId);
	}

	private Instance ec2Instance(DateTime withLaunchTime) {
		return new Instance().withInstanceId("i-123").withLaunchTime(
				withLaunchTime.toDate());
	}

}
