package com.elastisys.scale.cloudpool.api.restapi;

import static com.elastisys.scale.commons.json.JsonUtils.parseJsonString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifier;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.api.types.TestUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link CloudPoolHandler} endpoint correctly dispatches
 * incoming calls to the backing {@link CloudPool} implementation.
 */
public class TestCloudPoolHandlerDispatch {

	/** The object under test. */
	private CloudPoolHandler restEndpoint;
	/**
	 * Mock backing {@link CloudPool} that endpoint will dispatch incoming calls
	 * to.
	 */
	private CloudPool cloudPoolMock = mock(CloudPool.class);

	@Before
	public void onSetup() {
		this.restEndpoint = new CloudPoolHandler(this.cloudPoolMock);
	}

	/**
	 * Verify proper delegation of {@code getPool} to backing {@link CloudPool}.
	 */
	@Test
	public void testGetPoolDispatch() throws Exception {
		// set up mock response
		List<String> publicIps = asList("1.2.3.4");
		List<String> privateIps = asList("1.2.3.5");
		JsonObject metadata = parseJsonString("{\"id\": \"i-1\"}");
		Machine machine = new Machine("i-1", MachineState.PENDING,
				MembershipStatus.defaultStatus(), ServiceState.BOOTING, null,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), publicIps,
				privateIps, metadata);
		MachinePool pool = TestUtils.pool(
				DateTime.parse("2014-01-13T12:00:00.000Z"), machine);
		when(this.cloudPoolMock.getMachinePool()).thenReturn(pool);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPool();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), pool.toJson());
	}

	/**
	 * Verify proper handling of {@code getPool} calls when an error is thrown
	 * from the backing {@link CloudPool}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetPoolDispatchOnInternalError() throws Exception {
		// set up mock response
		when(this.cloudPoolMock.getMachinePool()).thenThrow(
				CloudPoolException.class);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPool();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code setDesiredSize} to the backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testSetDesiredSizeDispatch() throws Exception {
		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(2));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudPoolMock).setDesiredSize(2);
		verifyNoMoreInteractions(this.cloudPoolMock);
	}

	/**
	 * Verify proper handling of {@code setDesiredSize} calls when an unexpected
	 * error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testSetDesiredSizeDispatchOnInternalError()
			throws CloudPoolException {
		// set up mock response: should throw error
		doThrow(new CloudPoolException("null pointer"))
				.when(this.cloudPoolMock).setDesiredSize(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(2));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code setDesiredSize} calls when an illegal
	 * input error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testSetDesiredSizeDispatchOnIllegalInputError()
			throws CloudPoolException {
		// set up mock response: should throw error
		doThrow(IllegalArgumentException.class).when(this.cloudPoolMock)
				.setDesiredSize(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(-2));
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code getPoolSize} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testGetPoolSizeDispatch() throws Exception {
		// set up mock response
		PoolSizeSummary poolSize = new PoolSizeSummary(3, 2, 1);
		when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPoolSize();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), JsonUtils.toJson(poolSize));
	}

	/**
	 * Verify proper handling of {@code getPoolsize} calls when an error is
	 * thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testGetPoolSizeDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).getPoolSize();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPoolSize();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code getMetadata} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testGetMetadataDispatch() throws Exception {
		// set up mock response
		CloudPoolMetadata metadata = new CloudPoolMetadata(
				PoolIdentifier.AWS_EC2, Lists.<String> newArrayList("3.1"));

		when(this.cloudPoolMock.getMetadata()).thenReturn(metadata);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getMetadata();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), JsonUtils.toJson(metadata));
	}

	/**
	 * Verify proper handling of {@code getMetadata} calls when an error is
	 * thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testGetMetadataDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).getMetadata();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getMetadata();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code terminateMachine} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testTerminateMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudPoolMock).terminateMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-1",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code terminateMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testTerminateMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudPoolMock)
				.terminateMachine("i-X", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-X",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code terminateMachine} calls when an internal
	 * error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testTerminateMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).terminateMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-1",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code detachMachine} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testDetachMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudPoolMock).detachMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-1",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code detachMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testDetachMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudPoolMock)
				.detachMachine("i-X", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-X",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code detachMachine} calls when an internal
	 * error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testDetachMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).detachMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-1",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code attachMachine} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testAttachMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudPoolMock).attachMachine("i-1");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-1");
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code attachMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testAttachMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudPoolMock)
				.attachMachine("i-X");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-X");
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code attachMachine} calls when an internal
	 * error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testAttachMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).attachMachine("i-1");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-1");
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code setServiceState} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testSetServiceStateDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudPoolMock).setServiceState("i-1",
				ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-1",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code setServiceState} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testSetServiceStateOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudPoolMock)
				.setServiceState("i-X", ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-X",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code setServiceState} calls when an internal
	 * error is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testSetServiceStateDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudPoolException("cloud api outage")).when(
				this.cloudPoolMock).setServiceState("i-1",
				ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-1",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code setMembershipStatus} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testSetMembershipStatusDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudPoolMock).setMembershipStatus("i-1",
				MembershipStatus.awaitingService());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setMembershipStatus(
				"i-1",
				new SetMembershipStatusRequest(MembershipStatus
						.awaitingService()));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code setMembershipStatus} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testSetMembershipStatusDispatchOnNotFoundError()
			throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudPoolMock)
				.setMembershipStatus("i-X", MembershipStatus.awaitingService());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setMembershipStatus(
				"i-X",
				new SetMembershipStatusRequest(MembershipStatus
						.awaitingService()));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code setMembershipStatus} calls when an
	 * internal error is thrown from the backing {@link CloudPool} .
	 */
	@Test
	public void testSetMembershipStatusDispatchOnInternalError()
			throws Exception {
		// set up mock response
		doThrow(new RuntimeException("cloud api outage")).when(
				this.cloudPoolMock).setMembershipStatus("i-X",
				MembershipStatus.awaitingService());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setMembershipStatus(
				"i-X",
				new SetMembershipStatusRequest(MembershipStatus
						.awaitingService()));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}
}
