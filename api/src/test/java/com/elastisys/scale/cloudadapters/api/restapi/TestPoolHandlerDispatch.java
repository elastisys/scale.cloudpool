package com.elastisys.scale.cloudadapters.api.restapi;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.cloudadapers.api.restapi.types.PoolResizeRequest;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.api.types.TestUtils;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link PoolHandler} endpoint correctly dispatches incoming
 * calls to the backing {@link CloudAdapter} implementation.
 *
 *
 *
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
		MachinePool pool = TestUtils.pool(DateTime
				.parse("2014-01-13T12:00:00.000Z"), TestUtils.machineNoIp("m1",
						MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z"), new JsonObject()));
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
	public void testGetPoolDispatchOnCloudAdapterError() throws Exception {
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
	 * Verify proper delegation of {@code resizePool} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testResizePoolDispatch() throws Exception {
		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.resizePool(new PoolResizeRequest(
				2));
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudAdapterMock).resizeMachinePool(2);
		verifyNoMoreInteractions(this.cloudAdapterMock);
	}

	/**
	 * Verify proper handling of {@code resizePool} calls when a cloud provider
	 * error is thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testResizePoolDispatchOnCloudAdapterError()
			throws CloudAdapterException {
		// set up mock response: should throw error
		doThrow(CloudAdapterException.class).when(this.cloudAdapterMock)
				.resizeMachinePool(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.resizePool(new PoolResizeRequest(
				2));
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code resizePool} calls when an error is
	 * thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testResizePoolDispatchOnIllegalInputError()
			throws CloudAdapterException {
		// set up mock response: should throw error
		doThrow(IllegalArgumentException.class).when(this.cloudAdapterMock)
				.resizeMachinePool(anyInt());

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.resizePool(new PoolResizeRequest(
				-2));
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}
}
