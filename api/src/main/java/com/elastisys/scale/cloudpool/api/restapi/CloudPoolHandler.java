package com.elastisys.scale.cloudpool.api.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.google.gson.JsonObject;

/**
 * Implements the cloud pool REST API, which is fully covered in the
 * <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud pool REST API documentation</a>.
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CloudPoolHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CloudPoolHandler.class);

    /** The {@link CloudPool} implementation to which all work is delegated. */
    private final CloudPool cloudPool;

    public CloudPoolHandler(CloudPool cloudPool) {
        LOG.info(getClass().getSimpleName() + " created");
        this.cloudPool = cloudPool;
    }

    /**
     * Retrieves the current machine pool members.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @GET
    @Path("/pool")
    public Response getPool() {
        requireStartedCloudPool();

        try {
            MachinePool machinePool = this.cloudPool.getMachinePool();
            return Response.ok(toJson(machinePool)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse("failure to process GET /pool", e);
        } catch (Exception e) {
            return internalErrorResponse("internal error on GET /pool", e);
        }
    }

    /**
     * Sets the desired number of machines in the machine pool. This method is
     * asynchronous in that the method returns immediately without having
     * carried out any required changes to the machine pool.
     *
     * @param request
     *            The desired number of machine in the pool.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @POST
    @Path("/pool/size")
    public Response setDesiredSize(SetDesiredSizeRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.setDesiredSize(request.getDesiredSize());
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            String message = "illegal input: " + e.getMessage();
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse("failure to process POST /pool/size", e);
        } catch (Exception e) {
            return internalErrorResponse("internal error on POST /pool/size", e);
        }
    }

    /**
     * Returns the current size of the machine pool -- both in terms of the
     * desired size and the actual size (as these may differ at any time).
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @GET
    @Path("/pool/size")
    public Response getPoolSize() {
        requireStartedCloudPool();

        try {
            PoolSizeSummary poolSize = this.cloudPool.getPoolSize();
            return Response.ok(toJson(poolSize)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse("failure to process GET /pool/size", e);
        } catch (Exception e) {
            return internalErrorResponse("internal error on GET /pool/size", e);
        }
    }

    /**
     * Returns metadata about the cloud pool and the cloud infrastructure it
     * manages.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @GET
    @Path("/pool/metadata")
    public Response getMetadata() {
        requireStartedCloudPool();

        try {
            CloudPoolMetadata metadata = this.cloudPool.getMetadata();
            return Response.ok(toJson(metadata)).build();
        } catch (Exception e) {
            return internalErrorResponse("internal error on GET /pool/metadata", e);
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
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/terminate")
    public Response terminateMachine(@PathParam("machine") String machineId, TerminateMachineRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.terminateMachine(machineId, request.isDecrementDesiredSize());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/%s/terminate", machineId), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/%s/terminate", machineId), e);
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
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/detach")
    public Response detachMachine(@PathParam("machine") String machineId, DetachMachineRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.detachMachine(machineId, request.isDecrementDesiredSize());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/%s/detach", machineId), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/%s/detach", machineId), e);
        }
    }

    /**
     * Attaches an already running machine instance to the pool, growing the
     * pool with a new member. This operation implies that the desired size of
     * the pool is incremented by one.
     *
     * @param machineId
     *            The identifier of the machine to attach to the pool.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/attach")
    public Response attachMachine(@PathParam("machine") String machineId) {
        requireStartedCloudPool();

        try {
            this.cloudPool.attachMachine(machineId);
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/%s/attach", machineId), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/%s/attach", machineId), e);
        }
    }

    /**
     * Sets the service state of a given machine pool member. Setting the
     * service state does not have any functional implications on the pool
     * member, but should be seen as way to supply operational information about
     * the service running on the machine to third-party services (such as load
     * balancers).
     *
     * @param machineId
     *            The machine whose service state is to be set.
     * @param request
     *            A {@link SetServiceStateRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" >
     *         cloud pool REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/serviceState")
    public Response setServiceState(@PathParam("machine") String machineId, SetServiceStateRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.setServiceState(machineId, request.getServiceState());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/%s/serviceState", machineId), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/%s/serviceState", machineId), e);
        }
    }

    @POST
    @Path("/pool/{machine}/membershipStatus")
    public Response setMembershipStatus(@PathParam("machine") String machineId, SetMembershipStatusRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.setMembershipStatus(machineId, request.getMembershipStatus());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/%s/setMembershipStatus", machineId),
                    e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/%s/membershipStatus", machineId),
                    e);
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

    /**
     * Requires the {@link CloudPool} to be in a started (and configured) state,
     * or else throws an {@link InternalServerErrorException} which breaks the
     * request processing and responds with a
     * {@code 500 (Internal Server Error)} response.
     */
    private void requireStartedCloudPool() {
        CloudPoolStatus cloudPoolStatus = this.cloudPool.getStatus();
        if (!cloudPoolStatus.isStarted()) {
            String errorMessage = "attempt to invoke cloudpool before being started";
            LOG.warn(errorMessage);
            ErrorType error = new ErrorType(errorMessage);
            throw new InternalServerErrorException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    /**
     * Produces a {@code 502} response for {@link CloudPoolException} raised by
     * the {@link CloudPool}.
     *
     * @param message
     * @param error
     * @return
     */
    private Response cloudErrorResponse(String message, CloudPoolException error) {
        String errorMsg = String.format("%s: %s", message, error.getMessage());
        LOG.error(errorMsg, error);
        return Response.status(Status.BAD_GATEWAY).entity(new ErrorType(errorMsg, error)).build();
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
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(errorMsg, error)).build();
    }

}
