package com.elastisys.scale.cloudadapers.api.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudadapers.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.types.ErrorType;
import com.google.gson.JsonObject;

/**
 * Implements the cloud adapter REST API, which is fully covered in the <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud adapter REST API documentation</a>.
 */
@Path("/")
public class PoolHandler {
	static Logger log = LoggerFactory.getLogger(PoolHandler.class);

	/** The {@link CloudAdapter} implementation to which all work is delegated. */
	private final CloudAdapter cloudAdapter;

	public PoolHandler(CloudAdapter cloudAdapter) {
		log.info(getClass().getSimpleName() + " created");
		this.cloudAdapter = cloudAdapter;
	}

	/**
	 * Retrieves the current machine pool members.
	 *
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@GET
	@Path("/pool")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPool() {
		log.info("GET /pool");
		try {
			MachinePool machinePool = this.cloudAdapter.getMachinePool();
			return Response.ok(toJson(machinePool)).build();
		} catch (Exception e) {
			String message = "failure to process pool GET /pool: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Sets the desired number of machines in the machine pool. This method is
	 * asynchronous in that the method returns immediately without having
	 * carried out any required changes to the machine pool.
	 *
	 * @param request
	 *            The desired number of machine in the pool.
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@POST
	@Path("/pool/size")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setDesiredSize(SetDesiredSizeRequest request) {
		log.info("POST /pool/size");
		try {
			this.cloudAdapter.setDesiredSize(request.getDesiredSize());
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			String message = "illegal input: " + e.getMessage();
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = "failure to process POST /pool/size: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Returns the current size of the machine pool -- both in terms of the
	 * desired size and the actual size (as these may differ at any time).
	 *
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@GET
	@Path("/pool/size")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoolSize() {
		log.info("GET /pool/size");
		try {
			PoolSizeSummary poolSize = this.cloudAdapter.getPoolSize();
			return Response.ok(toJson(poolSize)).build();
		} catch (Exception e) {
			String message = "failure to process GET /pool/size: "
					+ e.getMessage();
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Terminates a particular machine pool member. The caller can control if a
	 * replacement machine is to be provisioned.
	 *
	 * @param machineId
	 *            The identifier of the machine to terminate.
	 * @param request
	 *            A {@link TerminateMachineRequest}.
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@POST
	@Path("/pool/{machine}/terminate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response terminateMachine(@PathParam("machine") String machineId,
			TerminateMachineRequest request) {
		log.info("POST /pool/{}/terminate", machineId);
		try {
			this.cloudAdapter.terminateMachine(machineId,
					request.isDecrementDesiredSize());
			return Response.ok().build();
		} catch (NotFoundException e) {
			String message = "unrecognized machine: " + e.getMessage();
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = String.format(
					"failure to process POST /pool/%s/terminate: %s",
					machineId, e.getMessage());
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Removes a member from the pool without terminating it. The machine keeps
	 * running but is no longer considered a pool member and, therefore, needs
	 * to be managed independently. The caller can control if a replacement
	 * machine is to be provisioned.
	 * 
	 * @param machineId
	 *            The identifier of the machine to detach from the pool.
	 * @param request
	 *            A {@link DetachMachineRequest}.
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@POST
	@Path("/pool/{machine}/detach")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response detachMachine(@PathParam("machine") String machineId,
			DetachMachineRequest request) {
		log.info("POST /pool/{}/detach", machineId);
		try {
			this.cloudAdapter.detachMachine(machineId,
					request.isDecrementDesiredSize());
			return Response.ok().build();
		} catch (NotFoundException e) {
			String message = "unrecognized machine: " + e.getMessage();
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = String.format(
					"failure to process POST /pool/%s/detach: %s", machineId,
					e.getMessage());
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Attaches an already running machine instance to the pool, growing the
	 * pool with a new member. This operation implies that the desired size of
	 * the group is incremented by one.
	 *
	 * @param machineId
	 *            The identifier of the machine to attach to the pool.
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@POST
	@Path("/pool/{machine}/attach")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response attachMachine(@PathParam("machine") String machineId) {
		log.info("POST /pool/{}/attach", machineId);
		try {
			this.cloudAdapter.attachMachine(machineId);
			return Response.ok().build();
		} catch (NotFoundException e) {
			String message = "unrecognized machine: " + e.getMessage();
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = String.format(
					"failure to process POST /pool/%s/attach: %s", machineId,
					e.getMessage());
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Sets the service state of a given machine pool member. Setting the
	 * service state has no side-effects, unless the service state is set to
	 * {@link ServiceState#OUT_OF_SERVICE}, in which case a replacement machine
	 * will be launched (since {@link ServiceState#OUT_OF_SERVICE} machines are
	 * not considered effective members of the pool). An out-of-service machine
	 * can later be taken back into service by another call to this method to
	 * re-set its service state.
	 *
	 * @param machineId
	 *            The machine whose service state is to be set.
	 * @param request
	 *            A {@link SetServiceStateRequest}.
	 * @return A response message as per the <a
	 *         href="http://cloudadapterapi.readthedocs.org/en/latest/" >cloud
	 *         adapter REST API</a>.
	 */
	@POST
	@Path("/pool/{machine}/serviceState")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setServiceState(@PathParam("machine") String machineId,
			SetServiceStateRequest request) {
		log.info("POST /pool/{}/serviceState", machineId);
		try {
			this.cloudAdapter.setServiceState(machineId,
					request.getServiceState());
			return Response.ok().build();
		} catch (NotFoundException e) {
			String message = "unrecognized machine: " + e.getMessage();
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorType(message, e)).build();
		} catch (Exception e) {
			String message = String.format(
					"failure to process POST /pool/%s/serviceState: %s",
					machineId, e.getMessage());
			log.error(message, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorType(message, e)).build();
		}
	}

	/**
	 * Turns an arbitrary {@link Object} to JSON.
	 *
	 * @param object
	 * @return
	 */
	private JsonObject toJson(Object object) {
		return JsonUtils.toJson(object).getAsJsonObject();
	}

}
