package com.elastisys.scale.cloudpool.api.restapi;

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
import java.nio.file.Paths;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link ConfigHandler} endpoint correctly dispatches
 * incoming calls to the backing {@link CloudPool} implementation.
 */
public class TestConfigHandlerDispatch {
	/** The object under test. */
	private ConfigHandler restEndpoint;
	/**
	 * Mock backing {@link CloudPool} that endpoint will dispatch incoming calls
	 * to.
	 */
	private CloudPool cloudPoolMock = mock(CloudPool.class);

	/** Storage dir for configurations. */
	private static final String storageDir = Paths
			.get("target", "cloudpool", "storage").toString();

	@Before
	public void onSetup() {
		this.restEndpoint = new ConfigHandler(this.cloudPoolMock, storageDir);
	}

	/**
	 * Verify proper delegation of {@code getConfiguration} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testGetConfigurationDispatch() throws IOException {
		// set up mock response
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}")
				.getAsJsonObject();
		when(this.cloudPoolMock.getConfiguration())
				.thenReturn(Optional.of(config));

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getConfig();
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), config);
	}

	/**
	 * Verify proper handling of {@code getConfiguration} calls when an error is
	 * thrown from the backing {@link CloudPool}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetConfigurationDispatchOnCloudPoolError()
			throws Exception {
		// set up mock response
		when(this.cloudPoolMock.getConfiguration())
				.thenThrow(CloudPoolException.class);

		// call rest endpoint and verify proper dispatching to mock
		Response response = this.restEndpoint.getConfig();
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper delegation of {@code postConfig} to backing
	 * {@link CloudPool}.
	 */
	@Test
	public void testPostConfigDispatch() throws Exception {
		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}")
				.getAsJsonObject();
		Response response = this.restEndpoint.setAndStoreConfig(config);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		assertEquals(response.getEntity(), null);

		verify(this.cloudPoolMock).configure(config);
		verifyNoMoreInteractions(this.cloudPoolMock);
	}

	/**
	 * Verify proper handling of {@code psotConfig} calls when a cloud provider
	 * error is thrown from the backing {@link CloudPool}. Should render a
	 * {@code 502} response.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPostConfigOnCloudError() throws Exception {
		// set up mock response: should throw error
		doThrow(new CloudPoolException("api error")).when(this.cloudPoolMock)
				.configure(any(JsonObject.class));

		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}")
				.getAsJsonObject();
		Response response = this.restEndpoint.setAndStoreConfig(config);
		assertEquals(response.getStatus(), Status.BAD_GATEWAY.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code psotConfig} calls when an internal error
	 * is thrown from the backing {@link CloudPool}. Should render a {@code 500}
	 * response.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPostConfigOnInternalError() throws Exception {
		// set up mock response: should throw error
		doThrow(new RuntimeException("buggy code")).when(this.cloudPoolMock)
				.configure(any(JsonObject.class));

		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}")
				.getAsJsonObject();
		Response response = this.restEndpoint.setAndStoreConfig(config);
		assertEquals(response.getStatus(),
				Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

	/**
	 * Verify proper handling of {@code postConfig} calls when an error is
	 * thrown from the backing {@link CloudPool}.Should render a {@code 400}
	 * response.
	 */
	@Test
	public void testPostConfigOnIllegalInputError() throws Exception {
		// set up mock response: should throw error
		doThrow(IllegalArgumentException.class).when(this.cloudPoolMock)
				.configure(any(JsonObject.class));

		// call rest endpoint and verify proper dispatching to mock
		JsonObject config = JsonUtils
				.parseJsonString("{\"key\": \"illegal-value\"}")
				.getAsJsonObject();
		Response response = this.restEndpoint.setAndStoreConfig(config);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity(), instanceOf(ErrorType.class));
	}

}
