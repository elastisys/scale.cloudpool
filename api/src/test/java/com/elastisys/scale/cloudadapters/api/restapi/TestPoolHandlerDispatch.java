package com.elastisys.scale.cloudadapters.api.restapi;

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

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.cloudadapers.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.api.types.TestUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link PoolHandler} endpoint correctly dispatches incoming
 * calls to the backing {@link CloudAdapter} implementation.
 */
public class TestPoolHandlerDispatch {

	/** The object under test. */
	private PoolHandler restEndpoint;
	/**
	 * Mock backing {@link CloudAdapter} that endpoint will dispatch incoming
	 * calls to.
	 */
	private CloudAdapter cloudAdapterMock = mock(CloudAdapter.class);

	@Before
	public void onSetup() {
		this.restEndpoint = new PoolHandler(this.cloudAdapterMock);
	}

	/**
	 * Verify proper delegation of {@code getPool} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testGetPoolDispatch() throws Exception {
		// set up mock response
		List<String> publicIps = asList("1.2.3.4");
		List<String> privateIps = asList("1.2.3.5");
		JsonObject metadata = parseJsonString("{\"id\": \"i-1\"}");
		Machine machine = new Machine("i-1", MachineState.PENDING,
				ServiceState.BOOTING,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), publicIps,
				privateIps, metadata);
		MachinePool pool = TestUtils.pool(
				DateTime.parse("2014-01-13T12:00:00.000Z"), machine);
		when(this.cloudAdapterMock.getMachinePool()).thenReturn(pool);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPool();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), pool.toJson());
	}

	/**
	 * Verify proper handling of {@code getPool} calls when an error is thrown
	 * from the backing {@link CloudAdapter}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetPoolDispatchOnInternalError() throws Exception {
		// set up mock response
		when(this.cloudAdapterMock.getMachinePool()).thenThrow(
				CloudAdapterException.class);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPool();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code setDesiredSize} to the backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testSetDesiredSizeDispatch() throws Exception {
		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(2));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudAdapterMock).setDesiredSize(2);
		verifyNoMoreInteractions(this.cloudAdapterMock);
	}

	/**
	 * Verify proper handling of {@code setDesiredSize} calls when an unexpected
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testSetDesiredSizeDispatchOnInternalError()
			throws CloudAdapterException {
		// set up mock response: should throw error
		doThrow(new CloudAdapterException("null pointer")).when(
				this.cloudAdapterMock).setDesiredSize(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(2));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code setDesiredSize} calls when an illegal
	 * input error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testSetDesiredSizeDispatchOnIllegalInputError()
			throws CloudAdapterException {
		// set up mock response: should throw error
		doThrow(IllegalArgumentException.class).when(this.cloudAdapterMock)
		.setDesiredSize(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint
				.setDesiredSize(new SetDesiredSizeRequest(-2));
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code getPoolSize} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testGetPoolSizeDispatch() throws Exception {
		// set up mock response
		PoolSizeSummary poolSize = new PoolSizeSummary(3, 2, 1);
		when(this.cloudAdapterMock.getPoolSize()).thenReturn(poolSize);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPoolSize();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), JsonUtils.toJson(poolSize));
	}

	/**
	 * Verify proper handling of {@code getPoolsize} calls when an error is
	 * thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testGetPoolSizeDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudAdapterException("cloud api outage")).when(
				this.cloudAdapterMock).getPoolSize();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getPoolSize();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code terminateMachine} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testTerminateMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudAdapterMock).terminateMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-1",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code terminateMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudAdapter}
	 * .
	 */
	@Test
	public void testTerminateMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudAdapterMock)
		.terminateMachine("i-X", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-X",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code terminateMachine} calls when an internal
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testTerminateMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudAdapterException("cloud api outage")).when(
				this.cloudAdapterMock).terminateMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.terminateMachine("i-1",
				new TerminateMachineRequest(false));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code detachMachine} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testDetachMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudAdapterMock).detachMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-1",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code detachMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudAdapter}
	 * .
	 */
	@Test
	public void testDetachMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudAdapterMock)
		.detachMachine("i-X", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-X",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code detachMachine} calls when an internal
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testDetachMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudAdapterException("cloud api outage")).when(
				this.cloudAdapterMock).detachMachine("i-1", false);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.detachMachine("i-1",
				new DetachMachineRequest(false));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code attachMachine} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testAttachMachineDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudAdapterMock).attachMachine("i-1");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-1");
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code attachMachine} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudAdapter}
	 * .
	 */
	@Test
	public void testAttachMachineOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudAdapterMock)
		.attachMachine("i-X");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-X");
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code attachMachine} calls when an internal
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testAttachMachineDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudAdapterException("cloud api outage")).when(
				this.cloudAdapterMock).attachMachine("i-1");

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.attachMachine("i-1");
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code setServiceState} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testSetServiceStateDispatch() throws Exception {
		// set up mock response
		doNothing().when(this.cloudAdapterMock).setServiceState("i-1",
				ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-1",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
	}

	/**
	 * Verify proper handling of {@code setServiceState} calls when a
	 * {@link NotFoundException} is thrown from the backing {@link CloudAdapter}
	 * .
	 */
	@Test
	public void testSetServiceStateOnNotFoundError() throws Exception {
		// set up mock response
		doThrow(NotFoundException.class).when(this.cloudAdapterMock)
		.setServiceState("i-X", ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-X",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code setServiceState} calls when an internal
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testSetServiceStateDispatchOnInternalError() throws Exception {
		// set up mock response
		doThrow(new CloudAdapterException("cloud api outage")).when(
				this.cloudAdapterMock).setServiceState("i-1",
						ServiceState.IN_SERVICE);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.setServiceState("i-1",
				new SetServiceStateRequest(ServiceState.IN_SERVICE));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));

	}

}
