package com.elastisys.scale.cloudpool.api.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A REST response handler that handles requests to get ({@code GET}) the
 * {@link CloudPool} (JSON) configuration schema (if one is supplied).
 * <p/>
 * <i>Note: this is an optional extension of the {@link CloudPool} REST API, and
 * is provided as way for a {@link CloudPool} to publish its configuration
 * parameters, for the cases when remote configuration is enabled (see
 * {@link ConfigHandler}).</i>
 */
@Path("/config/schema")
public class ConfigSchemaHandler {
	static Logger log = LoggerFactory.getLogger(ConfigSchemaHandler.class);

	/** The {@link CloudPool} implementation to which all work is delegated. */
	private final CloudPool cloudPool;

	public ConfigSchemaHandler(CloudPool cloudPool) {
		log.info(getClass().getSimpleName() + " created");
		this.cloudPool = cloudPool;
	}

	/**
	 * Returns the JSON Schema for the {@link CloudPool}, if one is supplied.
	 * <p/>
	 * <i>Note: this is an optional extension of the cloud pool REST API
	 * provided to facilitate remote re-configuration of a {@link CloudPool}. A
	 * cloud pool is not required to respond to this type of requests.</i>
	 *
	 *
	 * @return A {@code 200} {@link Response} with the JSON Schema as content,
	 *         if one is supplied by the {@link CloudPool}. Otherwise a
	 *         {@code 404} {@link Response} is returned.
	 */
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationSchema() {
		log.info("GET /config/schema");
		try {
			Optional<JsonObject> schema = this.cloudPool
					.getConfigurationSchema();

			if (!schema.isPresent()) {
				ErrorType entity = new ErrorType(
						"cloud pool does not publish a configuration schema");
				return Response.status(Status.NOT_FOUND).entity(entity).build();

			}
			return Response.ok(schema.get()).build();
		} catch (Exception e) {
			String message = "failure to process config schema get request: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}

	}

}
