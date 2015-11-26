package com.elastisys.scale.cloudpool.api.restapi;

import static com.google.common.base.Preconditions.checkArgument;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.commons.json.types.ErrorType;

/**
 * A REST response handler that handles requests to start {@code POST /start},
 * stop {@code POST /stop}, and get the execution status of {@code GET /status}
 * a {@link CloudPool}.
 */
@Path("/")
public class StartStopHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(StartStopHandler.class);

	/** The back-end {@link CloudPool} being managed. */
	private final CloudPool cloudPool;

	/**
	 * Creates a {@link StartStopHandler} for a given {@link CloudPool}.
	 *
	 * @param cloudPool
	 *            The back-end {@link CloudPool} to manage.
	 */
	public StartStopHandler(CloudPool cloudPool) {
		LOG.info(getClass().getSimpleName() + " created");
		checkArgument(cloudPool != null, "cloudPool cannot be null");
		this.cloudPool = cloudPool;
	}

	/**
	 * Starts the cloud pool.
	 * <p/>
	 * This will set the cloud pool in an activated state where it will start to
	 * accept requests to query or modify the machine pool.
	 * <p/>
	 * If the cloud pool has not been configured the method should fail. If the
	 * cloud pool is already started this is a no-op.
	 *
	 * @return
	 * 		<ul>
	 *         <li>On success: HTTP response code 200.</li>
	 *         <li>On error: HTTP response 400 (Bad Request) with an
	 *         {@link ErrorType} message on an attempt to start an unconfigured
	 *         {@link CloudPool}. On other errors: HTTP response code 500 with
	 *         an {@link ErrorType} message.</li>
	 *         </ul>
	 */
	@POST
	@Path("/start")
	public Response start() {
		try {
			this.cloudPool.start();
			return Response.ok().build();
		} catch (NotConfiguredException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorType(e.getMessage(), e)).build();
		} catch (Exception e) {
			String message = "failure to start cloud pool: " + e.getMessage();
			LOG.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Stops the cloud pool.
	 * <p/>
	 * A stopped cloud pool is in a passivated state and will not accept any
	 * requests to query or modify the machine pool.
	 * <p/>
	 * If the cloud pool is already in a stopped state this is a no-op.
	 *
	 * @return
	 * 		<ul>
	 *         <li>On success: HTTP response code 200.</li>
	 *         <li>On error: HTTP response code 500 with an {@link ErrorType}
	 *         message.</li>
	 *         </ul>
	 */
	@POST
	@Path("/stop")
	public Response stop() {
		try {
			this.cloudPool.stop();
			return Response.ok().build();
		} catch (Exception e) {
			String message = "failure to stop cloud pool: " + e.getMessage();
			LOG.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Retrieves the execution status for the cloud pool.
	 *
	 * @return
	 * 		<ul>
	 *         <li>On success: HTTP response code 200 with a
	 *         {@link CloudPoolStatus} message.</li>
	 *         <li>On error: HTTP response code 500 with an {@link ErrorType}
	 *         message.</li>
	 *         </ul>
	 */
	@GET
	@Path("/status")
	public Response getStatus() {
		try {
			CloudPoolStatus status = this.cloudPool.getStatus();
			return Response.ok().entity(status).build();
		} catch (Exception e) {
			String message = "failure to get cloud pool execution status: "
					+ e.getMessage();
			LOG.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}
}
