package com.elastisys.scale.cloudadapers.api.restapi;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import com.elastisys.scale.cloudadapers.api.restapi.types.PoolResizeRequest;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.commons.rest.types.ErrorType;

/**
 * Implements the cloud adapter REST API, which is fully covered in the <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud adapter REST API documentation</a>.
 * 
 * 
 * 
 */
@Path("/pool")
public class PoolHandler {
	static Logger log = LoggerFactory.getLogger(PoolHandler.class);

	/** The {@link CloudAdapter} implementation to which all work is delegated. */
	private final CloudAdapter cloudAdapter;

	/** Lock to prevent concurrent access to critical sections. */
	private final Lock lock = new ReentrantLock();

	public PoolHandler(CloudAdapter cloudAdapter) {
		log.info(getClass().getSimpleName() + " created");
		this.cloudAdapter = cloudAdapter;
	}

	/**
	 * Retrieves the current machine pool members.
	 * 
	 * @return <ul>
	 *         <li>On success: HTTP response code 200 with a <i>machine pool
	 *         response message</i> (see {@link CloudAdapterRestApi}).</li>
	 *         <li>On error: HTTP response code 500 with an {@link ErrorType}
	 *         message.</li>
	 *         </ul>
	 */
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPool() {
		log.info("GET /pool");
		try {
			MachinePool machinePool = this.cloudAdapter.getMachinePool();
			return Response.ok(machinePool.toJson()).build();
		} catch (Exception e) {
			String message = "failure to process pool get request: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Requests a resize of the machine pool.
	 * 
	 * @param resizeRequest
	 *            The desired number of machine instances in the pool as a
	 *            <i>machine pool resize request message</i> (see
	 *            {@link CloudAdapterRestApi}).
	 * @return <ul>
	 *         <li>On success: HTTP response code 200 without message content.</li>
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response resizePool(PoolResizeRequest resizeRequest) {
		log.info("POST /pool");
		try {
			// make sure requests are processed one-by-one
			this.lock.lock();
			log.debug("Handling POST /pool");
			this.cloudAdapter.resizeMachinePool(resizeRequest.getDesiredCapacity());
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			String message = "illegal input: " + e.getMessage();
			log.error(message, e);
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = "failure to process pool resize request: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		} finally {
			log.debug("Finished handling POST /pool");
			this.lock.unlock();
		}

	}

}
