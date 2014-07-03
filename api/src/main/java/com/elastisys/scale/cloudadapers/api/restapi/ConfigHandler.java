package com.elastisys.scale.cloudadapers.api.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A REST response handler that handles requests to get ({@code GET}) and set (
 * {@code POST}) the {@link CloudAdapter} configuration.
 * <p/>
 * <i>Note: this is an optional extension of the {@link CloudAdapter} REST API,
 * and is merely provided to facilitate remote (re-)configuration of a
 * {@link CloudAdapter}.</i>
 * 
 * 
 * 
 */
@Path("/config")
public class ConfigHandler {
	static Logger log = LoggerFactory.getLogger(ConfigHandler.class);

	/** The {@link CloudAdapter} implementation to which all work is delegated. */
	private final CloudAdapter cloudAdapter;

	public ConfigHandler(CloudAdapter cloudAdapter) {
		log.info(getClass().getSimpleName() + " created");
		this.cloudAdapter = cloudAdapter;
	}

	/**
	 * Retrieves the configuration currently set for the {@link CloudAdapter}.
	 * <p/>
	 * <i>Note: this is an optional extension of the cloud adapter REST API
	 * provided to facilitate remote re-configuration of a {@link CloudAdapter}.
	 * A cloud adapter is not required to respond to this type of requests.</i>
	 * 
	 * @return <ul>
	 *         <li>On success: HTTP response code 200 with a JSON-formatted
	 *         configuration.</li> <li>On error: HTTP response code 500 with an
	 *         {@link ErrorType} message.</li>
	 *         </ul>
	 */
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfig() {
		log.info("GET /config");
		try {
			Optional<JsonObject> configuration = this.cloudAdapter
					.getConfiguration();
			if (!configuration.isPresent()) {
				ErrorType entity = new ErrorType(
						"no cloud adapter configuration has been set");
				return Response.status(Status.NOT_FOUND).entity(entity).build();
			}
			return Response.ok(configuration.get()).build();
		} catch (Exception e) {
			String message = "failure to process config get request: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Sets the configuration for the {@link CloudAdapter}.
	 * <p/>
	 * <i>Note: this is an optional extension of the cloud adapter REST API
	 * provided to facilitate remote re-configuration of a {@link CloudAdapter}.
	 * A cloud adapter is not required to respond to this type of requests.</i>
	 * 
	 * @param configuration
	 *            The (JSON) configuration document to set.
	 * @return <ul>
	 *         <li>On success: HTTP response code 200 without content.</li> <li>
	 *         On error:
	 *         <ul>
	 *         <li>on illegal input: HTTP response code 400 with an
	 *         {@link ErrorType} message</li> <li>otherwise: HTTP response code
	 *         500 with an {@link ErrorType} message</li>
	 *         </ul>
	 *         </ul>
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setConfig(JsonObject configuration) {
		log.info("POST /config");
		try {
			this.cloudAdapter.configure(configuration);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			String message = "illegal input: " + e.getMessage();
			log.error(message, e);
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = "failure to process config set request: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

}
