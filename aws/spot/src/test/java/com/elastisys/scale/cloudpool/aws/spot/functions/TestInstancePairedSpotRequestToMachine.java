package com.elastisys.scale.cloudpool.aws.spot.functions;

import static com.elastisys.scale.cloudpool.aws.spot.util.SpotTestUtil.list;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifiers;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.spot.metadata.InstancePairedSpotRequest;
import com.elastisys.scale.cloudpool.aws.spot.util.SpotTestUtil;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Tests the translation of an {@link InstancePairedSpotRequest} to a
 * {@link Machine} representation.
 */
public class TestInstancePairedSpotRequestToMachine {

	private static final DateTime NOW = UtcTime
			.parse("2015-01-01T12:00:00.000Z");

	@Before
	public void onSetup() {
		FrozenTime.setFixed(NOW);
	}

	/** Conversion of open (not yet fulfilled) request. */
	@Test
	public void testConversionOfOpenRequest() {
		InstancePairedSpotRequest openRequest = new InstancePairedSpotRequest(
				SpotTestUtil.spotRequest("sir-1", "open", null), null);

		Machine machine = InstancePairedSpotRequestToMachine
				.convert(openRequest);
		assertThat(machine.getId(), is("sir-1"));
		assertThat(machine.getMachineState(), is(MachineState.REQUESTED));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getLaunchTime(), is(nullValue()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps(), is(list()));
		assertThat(machine.getPrivateIps(), is(list()));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(openRequest)));
	}

	/** Conversion of closed request. */
	@Test
	public void testConversionOfClosedRequest() {
		InstancePairedSpotRequest closedRequest = new InstancePairedSpotRequest(
				SpotTestUtil.spotRequest("sir-1", "closed", null), null);

		Machine machine = InstancePairedSpotRequestToMachine
				.convert(closedRequest);
		assertThat(machine.getId(), is("sir-1"));
		assertThat(machine.getMachineState(), is(MachineState.TERMINATED));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getLaunchTime(), is(nullValue()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps(), is(list()));
		assertThat(machine.getPrivateIps(), is(list()));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(closedRequest)));

	}

	/** Conversion of failed request. */
	@Test
	public void testConversionOfFailedRequest() {
		InstancePairedSpotRequest failedRequest = new InstancePairedSpotRequest(
				SpotTestUtil.spotRequest("sir-1", "failed", null), null);

		Machine machine = InstancePairedSpotRequestToMachine
				.convert(failedRequest);
		assertThat(machine.getId(), is("sir-1"));
		assertThat(machine.getMachineState(), is(MachineState.REJECTED));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getLaunchTime(), is(nullValue()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps(), is(list()));
		assertThat(machine.getPrivateIps(), is(list()));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(failedRequest)));
	}

	/** Conversion of cancelled request. */
	@Test
	public void testConversionOfCancelledRequest() {
		InstancePairedSpotRequest cancelledRequest = new InstancePairedSpotRequest(
				SpotTestUtil.spotRequest("sir-1", "cancelled", null), null);

		Machine machine = InstancePairedSpotRequestToMachine
				.convert(cancelledRequest);
		assertThat(machine.getId(), is("sir-1"));
		assertThat(machine.getMachineState(), is(MachineState.TERMINATED));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getLaunchTime(), is(nullValue()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps(), is(list()));
		assertThat(machine.getPrivateIps(), is(list()));
		assertThat(machine.getMetadata(),
				is(JsonUtils.toJson(cancelledRequest)));
	}

	/**
	 * Conversion of request that has been cancelled but with an instance that
	 * is still running.
	 */
	@Test
	public void testConversionOfCancelledRequestWithRunningInstance() {
		InstancePairedSpotRequest cancelledRequest = new InstancePairedSpotRequest(
				SpotTestUtil.spotRequest("sir-1", "cancelled", "i-1"),
				SpotTestUtil.instance("i-1", InstanceStateName.Running,
						"sir-1"));

		Machine machine = InstancePairedSpotRequestToMachine
				.convert(cancelledRequest);
		assertThat(machine.getId(), is("sir-1"));
		assertThat(machine.getMachineState(), is(MachineState.TERMINATED));
		assertThat(machine.getCloudProvider(), is(PoolIdentifiers.AWS_SPOT));
		assertThat(machine.getMachineSize(), is("m1.medium"));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getLaunchTime(), is(FrozenTime.now()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getPublicIps(), is(list()));
		assertThat(machine.getPrivateIps(), is(list()));
		assertThat(machine.getMetadata(),
				is(JsonUtils.toJson(cancelledRequest)));
	}

	@Test
	public void testConversionWithServiceStateTag() {
		SpotInstanceRequest spotRequest = SpotTestUtil.spotRequest("sir-1",
				"open", null);
		spotRequest.withTags(new Tag().withKey(ScalingTags.SERVICE_STATE_TAG)
				.withValue(ServiceState.IN_SERVICE.name()));
		InstancePairedSpotRequest request = new InstancePairedSpotRequest(
				spotRequest, null);

		Machine machine = InstancePairedSpotRequestToMachine.convert(request);
		assertThat(machine.getServiceState(), is(ServiceState.IN_SERVICE));
	}

	@Test
	public void testConversionWithMembershipStatusTag() {
		SpotInstanceRequest spotRequest = SpotTestUtil.spotRequest("sir-1",
				"open", null);
		spotRequest
				.withTags(new Tag().withKey(ScalingTags.MEMBERSHIP_STATUS_TAG)
						.withValue(MembershipStatus.blessed().toString()));
		InstancePairedSpotRequest request = new InstancePairedSpotRequest(
				spotRequest, null);

		Machine machine = InstancePairedSpotRequestToMachine.convert(request);
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.blessed()));
	}

}
