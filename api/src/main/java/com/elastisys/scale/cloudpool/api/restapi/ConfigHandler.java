package com.elastisys.scale.cloudpool.api.restapi;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

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

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

/**
 * A REST response handler that handles requests to get ({@code GET}) and set (
 * {@code POST}) the {@link CloudPool} configuration.
 */
@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(ConfigHandler.class);

	/**
	 * Default file name (within the storage directory) in which
	 * {@link CloudPool} configuration is stored.
	 */
	public static final String DEFAULT_CONFIG_FILE_NAME = "config.json";

	/** The back-end {@link CloudPool} being managed. */
	private final CloudPool cloudPool;

	/**
	 * The directory where runtime state for the {@link CloudPool} is stored.
	 * The {@link ConfigHandler} will use this directory to store every set
	 * configuration so that it can be restored on restart.
	 */
	private final String storageDir;

	/**
	 * The file name, within the {@link #storageDir}, in which {@link CloudPool}
	 * configuration is stored.
	 */
	private final String configFileName;

	/**
	 * Creates a {@link ConfigHandler} that will store set {@link CloudPool}
	 * configurations under a given storage directory with the
	 * {@link #DEFAULT_CONFIG_FILE_NAME}.
	 *
	 * @param cloudPool
	 *            The back-end {@link CloudPool} that is being managed.
	 * @param storageDir
	 *            The directory path where runtime state for the
	 *            {@link CloudPool} is stored. The {@link ConfigHandler} will
	 *            use this directory to store every set configuration so that it
	 *            can be restored on restart. The directory will be created if
	 *            it does not exist.
	 */
	public ConfigHandler(CloudPool cloudPool, String storageDir) {
		this(cloudPool, storageDir, DEFAULT_CONFIG_FILE_NAME);
	}

	/**
	 * Creates a {@link ConfigHandler} that will store set {@link CloudPool}
	 * configurations under a given storage directory with a given file name.
	 *
	 * @param cloudPool
	 *            The back-end {@link CloudPool} that is being managed.
	 * @param storageDir
	 *            The directory path where runtime state for the
	 *            {@link CloudPool} is stored. The {@link ConfigHandler} will
	 *            use this directory to store every set configuration so that it
	 *            can be restored on restart. The directory will be created if
	 *            it does not exist.
	 * @param configFileName
	 *            The file name, within the {@link #storageDir}, in which
	 *            {@link CloudPool} configuration is stored.
	 */
	public ConfigHandler(CloudPool cloudPool, String storageDir,
			String configFileName) {
		LOG.info(getClass().getSimpleName() + " created");
		checkArgument(cloudPool != null, "cloudPool cannot be null");
		File storageDirectory = new File(storageDir);
		if (!storageDirectory.exists()) {
			prepareStorageDir(storageDirectory);
		} else {
			checkArgument(storageDirectory.isDirectory(),
					"cloud pool storageDir %s is not a directory", storageDir);
		}

		checkArgument(configFileName != null, "configFileName cannot be null");
		this.cloudPool = cloudPool;
		this.storageDir = storageDir;
		this.configFileName = configFileName;
	}

	/**
	 * Attempts to creates a given storage directory.
	 *
	 * @param storageDirectory
	 * @throws IllegalArgumentException
	 */
	private void prepareStorageDir(File storageDirectory)
			throws IllegalArgumentException {
		LOG.info("creating storage directory {}",
				storageDirectory.getAbsolutePath());
		if (!storageDirectory.mkdirs()) {
			throw new IllegalArgumentException(String.format(
					"cloud pool: failed to create specified "
							+ "storage directory %s",
					storageDirectory.getAbsolutePath()));
		}
	}

	/**
	 * Retrieves the configuration currently set for the {@link CloudPool}.
	 *
	 * @return
	 * 		<ul>
	 *         <li>On success: HTTP response code 200 with a JSON-formatted
	 *         configuration.</li>
	 *         <li>On error: HTTP response 404 (Not Found) if no configuration
	 *         has been set. On other errors: HTTP response code 500 with an
	 *         {@link ErrorType} message.</li>
	 *         </ul>
	 */
	@GET
	public Response getConfig() {
		try {
			Optional<JsonObject> configuration = this.cloudPool
					.getConfiguration();
			if (!configuration.isPresent()) {
				ErrorType entity = new ErrorType(
						"no cloud pool configuration has been set");
				return Response.status(Status.NOT_FOUND).entity(entity).build();
			}
			return Response.ok(configuration.get()).build();
		} catch (Exception e) {
			return internalErrorResponse("internal error on GET /config", e);
		}
	}

	/**
	 * Sets the configuration for the {@link CloudPool}.
	 *
	 * @param configuration
	 *            The (JSON) configuration document to set.
	 * @return
	 * 		<ul>
	 *         <li>On success: HTTP response code 200 without content.</li>
	 *         <li>On error:
	 *         <ul>
	 *         <li>on illegal input: HTTP response code 400 with an
	 *         {@link ErrorType} message</li>
	 *         <li>otherwise: HTTP response code 500 with an {@link ErrorType}
	 *         message</li>
	 *         </ul>
	 *         </ul>
	 */
	@POST
	public Response setAndStoreConfig(JsonObject configuration) {
		try {
			this.cloudPool.configure(configuration);
			storeConfig(configuration);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			String message = "illegal input: " + e.getMessage();
			LOG.error(message, e);
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorType(message, e)).build();
		} catch (CloudPoolException e) {
			return cloudErrorResponse("failure to process POST /config", e);
		} catch (Exception e) {
			return internalErrorResponse("internal error on POST /config", e);
		}
	}

	/**
	 * Stores a configuration in the {@link #storageDir}, to allow it to be
	 * restored when the {@link CloudPool} is restarted.
	 *
	 * @param configuration
	 * @throws IOException
	 *             If the configuration could not be stored.
	 */
	public void storeConfig(JsonObject configuration) throws IOException {
		Files.write(JsonUtils.toPrettyString(configuration),
				getCloudPoolConfigPath().toFile(), Charsets.UTF_8);
	}

	/**
	 * Returns the file system path where the {@link ConfigHandler} stores
	 * received {@link CloudPool} configurations.
	 *
	 * @param storageDir
	 * @return
	 */
	public java.nio.file.Path getCloudPoolConfigPath() {
		return Paths.get(this.storageDir, this.configFileName);
	}

	/**
	 * Produces a {@code 502} response for {@link CloudPoolException} raised by
	 * the {@link CloudPool}.
	 *
	 * @param message
	 * @param error
	 * @return
	 */
	private Response cloudErrorResponse(String message,
			CloudPoolException error) {
		String errorMsg = String.format("%s: %s", message, error.getMessage());
		LOG.error(errorMsg, error);
		return Response.status(Status.BAD_GATEWAY)
				.entity(new ErrorType(errorMsg, error)).build();
	}

	/**
	 * Produces a {@code 500} response for internal error {@link Exception}s
	 * raised by the {@link CloudPool}.
	 *
	 * @param message
	 * @param error
	 * @return
	 */
	private Response internalErrorResponse(String message, Exception error) {
		String errorMsg = String.format("%s: %s", message, error.getMessage());
		LOG.error(errorMsg, error);
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(new ErrorType(errorMsg, error)).build();
	}
}
