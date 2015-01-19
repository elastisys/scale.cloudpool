package com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.ErrorResponseMessage;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.ResizeRequestMessage;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.google.common.base.Charsets;
import com.google.gson.JsonElement;

public class CloudAdapterServlet extends HttpServlet {
	private static final Logger LOG = LoggerFactory
			.getLogger(CloudAdapterServlet.class);

	public static final String SERVER_PATH = "src/test/resources/server/";
	public static final String REALM_FILE = SERVER_PATH
			+ "security-realm.properties";
	public static final String KEYSTORE_PATH = SERVER_PATH
			+ "server_keystore.p12";
	public static final String REALM_ROLE = "USER";
	public static final String KEYSTORE_PASSWORD = "pkcs12password";

	private static final long serialVersionUID = 0xC0DEBEEF;

	private int nextGetResponse = HttpServletResponse.SC_OK;
	private int nextPostResponse = HttpServletResponse.SC_OK;

	private boolean returnBrokenMachinePool = false;

	private long requestedSize;
	private final PrioritizedRemoteCloudAdapter cloudAdapter;

	/**
	 * @return the cloudAdapter
	 */
	public PrioritizedRemoteCloudAdapter getCloudAdapter() {
		return this.cloudAdapter;
	}

	public CloudAdapterServlet(PrioritizedRemoteCloudAdapter cloudAdapter) {
		this.cloudAdapter = cloudAdapter;
	}

	public void setNextPostResponse(int nextResponse) {
		this.nextPostResponse = ensureValidResponse(nextResponse);
	}

	private int ensureValidResponse(int nextResponse) {
		switch (nextResponse) {
		case HttpServletResponse.SC_BAD_REQUEST:
		case HttpServletResponse.SC_OK:
		case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
			return nextResponse;
		default:
			throw new IllegalArgumentException(
					"Unallowed value for nextResponse!");
		}
	}

	public void setNextGetResponse(int nextResponse) {
		this.nextGetResponse = ensureValidResponse(nextResponse);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		LOG.info("GET recevied and should result in " + this.nextGetResponse);

		switch (this.nextGetResponse) {
		case HttpServletResponse.SC_OK:
			final String json;
			if (!this.returnBrokenMachinePool) {
				json = JsonUtils.toPrettyString(JsonUtils
						.toJson(this.cloudAdapter.getMachinePool()));

			} else {
				json = "{ \"brokenPoolGoesHere\": \"true\" }";
			}
			resp.getWriter().write(json);
			break;
		case HttpServletResponse.SC_BAD_REQUEST:
			ErrorResponseMessage error = new ErrorResponseMessage(
					"the request was invalid", "");
			resp.getWriter().write(
					JsonUtils.toPrettyString(JsonUtils.toJson(error)));
			break;
		case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
			error = new ErrorResponseMessage("internal server error",
					"some error happened");
			resp.getWriter().write(
					JsonUtils.toPrettyString(JsonUtils.toJson(error)));
			break;
		default:
			LOG.error("This should not be possible");
			break;
		}

		resp.setContentType("application/json");
		resp.setStatus(this.nextGetResponse);
	}

	/**
	 * @param returnBrokenMachinePool
	 *            the returnBrokenMachinePool to set
	 */
	public void setReturnBrokenMachinePool(boolean returnBrokenMachinePool) {
		this.returnBrokenMachinePool = returnBrokenMachinePool;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		LOG.info("POST recevied and should result in " + this.nextPostResponse);

		switch (this.nextPostResponse) {
		case HttpServletResponse.SC_OK:
			JsonElement json = JsonUtils.parseJsonString(IO.toString(
					req.getInputStream(), Charsets.UTF_8.displayName()));
			ResizeRequestMessage resizeRequest = JsonUtils.toObject(json,
					ResizeRequestMessage.class);
			this.requestedSize = resizeRequest.getDesiredCapacity();
			this.cloudAdapter.resizeMachinePool(this.requestedSize);
			LOG.debug("Requested size via: " + JsonUtils.toString(json)
					+ " is=" + this.requestedSize);
			// resp.getWriter().write("");
			break;
		case HttpServletResponse.SC_BAD_REQUEST:
			ErrorResponseMessage error = new ErrorResponseMessage(
					"the request was invalid", "");
			resp.getWriter().write(
					JsonUtils.toPrettyString(JsonUtils.toJson(error)));
			break;
		case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
			error = new ErrorResponseMessage("internal server error",
					"some error happened");
			resp.getWriter().write(
					JsonUtils.toPrettyString(JsonUtils.toJson(error)));
			break;
		default:
			LOG.error("This should not be possible");
			break;
		}

		resp.setContentType("application/json");
		resp.setStatus(this.nextPostResponse);
	}

	/**
	 * @return the requestedSize
	 */
	public long getRequestedSize() {
		return this.requestedSize;
	}

	/**
	 * Creates a new {@link Server} with the given parameters.
	 *
	 * @param servlet
	 *            The servlet to run at the <code>"/pool/"</code> endpoint.
	 * @param serverPort
	 *            The port number to use.
	 * @param realmFile
	 *            The file with the HTTP BASIC credentials.
	 * @param realmRole
	 *            The role users must have.
	 * @param keystorePath
	 *            The path to the keystore for {@link CertificateCredentials}.
	 * @param keystorePassword
	 *            The password to the keystore.
	 * @return A new server.
	 */
	public static Server getServer(CloudAdapterServlet servlet, int serverPort,
			String realmFile, String realmRole, String keystorePath,
			String keystorePassword) {
		ServletDefinition servletDefinition = new ServletDefinition.Builder()
				.servlet(servlet).servletPath("/pool").requireBasicAuth(true)
				.realmFile(realmFile).requireRole(realmRole).build();
		return ServletServerBuilder.create().httpsPort(serverPort)
				.sslKeyStoreType(SslKeyStoreType.PKCS12)
				.sslKeyStorePath(keystorePath)
				.sslKeyStorePassword(keystorePassword)
				.sslRequireClientCert(false).addServlet(servletDefinition)
				.build();
	}

	/**
	 * Convenience method for creating a default Server configured with working
	 * settings.
	 *
	 * @param servlet
	 *            The servlet to run at the <code>"/pool/"</code> endpoint.
	 * @param serverPort
	 *            The port number to use.
	 * @return A new server.
	 */
	public static Server getServer(CloudAdapterServlet servlet, int serverPort) {
		return getServer(servlet, serverPort, REALM_FILE, REALM_ROLE,
				KEYSTORE_PATH, KEYSTORE_PASSWORD);
	}
}