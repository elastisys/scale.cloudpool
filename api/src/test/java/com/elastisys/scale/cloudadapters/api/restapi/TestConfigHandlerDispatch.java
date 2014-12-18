package com.elastisys.scale.cloudadapters.api.restapi;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.restapi.ConfigHandler;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link ConfigHandler} endpoint correctly dispatches
 * incoming calls to the backing {@link CloudAdapter} implementation.
 *
 *
 *
 */
public class TestConfigHandlerDispatch {
	/** The object under test. */
	private ConfigHandler restEndpoint;
	/**
	 * Mock backing {@link CloudAdapter} that endpoint will dispatch incoming
	 * calls to.
	 */
	private CloudAdapter cloudAdapterMock = mock(CloudAdapter.class);

	@Before
	public void onSetup() {
		this.restEndpoint = new ConfigHandler(this.cloudAdapterMock);
	}

	/**
	 * Verify proper delegation of {@code getConfiguration} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testGetConfigurationDispatch() throws IOException {
		// set up mock response
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}");
		when(this.cloudAdapterMock.getConfiguration()).thenReturn(
				Optional.of(config));

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getConfig();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), config);
	}

	/**
	 * Verify proper handling of {@code getConfiguration} calls when an error is
	 * thrown from the backing {@link CloudAdapter}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetConfigurationDispatchOnCloudAdapterError()
			throws Exception {
		// set up mock response
		when(this.cloudAdapterMock.getConfiguration()).thenThrow(
				CloudAdapterException.class);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getConfig();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code postConfig} to backing
	 * {@link CloudAdapter}.
	 */
	@Test
	public void testPostConfigDispatch() throws Exception {
		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}");
		Response response = this.restEndpoint.setConfig(config);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudAdapterMock).configure(config);
		verifyNoMoreInteractions(this.cloudAdapterMock);
	}

	/**
	 * Verify proper handling of {@code psotConfig} calls when a cloud provider
	 * error is thrown from the backing {@link CloudAdapter}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPostConfigOnCloudAdapterError() throws Exception {
		// set up mock response: should throw error
		doThrow(CloudAdapterException.class).when(this.cloudAdapterMock)
		.configure(any(JsonObject.class));

		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}");
		Response response = this.restEndpoint.setConfig(config);
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code postConfig} calls when an error is
	 * thrown from the backing {@link CloudAdapter}.
	 */
	@Test
	public void testPostConfigOnIllegalInputError() throws Exception {
		// set up mock response: should throw error
		doThrow(IllegalArgumentException.class).when(this.cloudAdapterMock)
		.configure(any(JsonObject.class));

		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils
				.parseJsonString("{\"key\": \"illegal-value\"}");
		Response response = this.restEndpoint.setConfig(config);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

}
