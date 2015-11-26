package com.elastisys.scale.cloudpool.api.restapi;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.commons.json.types.ErrorType;

/**
 * Verifies that the {@link StartStopHandler} endpoint correctly dispatches
 * incoming calls to the backing {@link CloudPool} implementation.
 */
public class TestStartStopHandlerDispatch {
	/** The object under test. */
	private StartStopHandler restEndpoint;
	/**
	 * Backing {@link CloudPool} that endpoint will dispatch incoming calls to.
	 */
	private CloudPool cloudPoolMock = mock(CloudPool.class);

	@Before
	public void onSetup() {
		this.restEndpoint = new StartStopHandler(this.cloudPoolMock);
	}

	/**
	 * Verify proper delegation of {@code getStatus} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testGetStatusDispatch() throws IOException {
		// set up mock response
		when(this.cloudPoolMock.getStatus()).thenReturn(
				new CloudPoolStatus(true, true));

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getStatus();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), new CloudPoolStatus(true, true));
	}

	/**
	 * Verify proper handling of {@code getStatus} calls when an error is thrown
	 * from the backing {@link CloudPool}.
	 */
	@Test
	public void testGetStatusDispatchOnCloudPoolError() throws Exception {
		// set up mock response
		when(this.cloudPoolMock.getStatus()).thenThrow(
				new CloudPoolException("error!"));

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getStatus();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code start} to backing {@link CloudPool}.
	 */
	@Test
	public void testStartDispatch() throws Exception {
		Response response = this.restEndpoint.start();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudPoolMock).start();
		verifyNoMoreInteractions(this.cloudPoolMock);
	}

	/**
	 * Verify proper handling of {@code start} calls when a cloud provider error
	 * is thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testStartDispatchOnNonConfiguredCloudPool() throws Exception {
		// set up mock response: should throw error
		doThrow(new NotConfiguredException("attempt to start without config"))
				.when(this.cloudPoolMock).start();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.start();
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code start} calls when an unexpected error is
	 * thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testStartDispatchOnUnexpectedCloudPoolError() throws Exception {
		// set up mock response: should throw error
		doThrow(new IllegalStateException("start failed!")).when(
				this.cloudPoolMock).start();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.start();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code stop} to backing {@link CloudPool}.
	 */
	@Test
	public void testStopDispatch() throws Exception {
		Response response = this.restEndpoint.stop();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudPoolMock).stop();
		verifyNoMoreInteractions(this.cloudPoolMock);
	}

	/**
	 * Verify proper handling of {@code stop} calls when an unexpected error is
	 * thrown from the backing {@link CloudPool}.
	 */
	@Test
	public void testStopDispatchOnUnexpectedCloudPoolError() throws Exception {
		// set up mock response: should throw error
		doThrow(new IllegalStateException("stop failed!")).when(
				this.cloudPoolMock).stop();

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.stop();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

}
