package com.elastisys.scale.cloudpool.multipool.restapi.restapi.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.restapi.restapi.MultiCloudPoolRestApi;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

@Path("/")
public class MultiCloudPoolRestApiImpl implements MultiCloudPoolRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MultiCloudPoolRestApiImpl.class);

    /**
     * Request information that gets injected by the JAX-RS runtime for each
     * method invocation. As such, its use is thread-safe.
     */
    @Context
    private UriInfo requestUri;

    private final MultiCloudPool multiCloudPool;

    public MultiCloudPoolRestApiImpl(MultiCloudPool multiCloudPool) {
        checkArgument(multiCloudPool != null, "multiCloudPool cannot be null");
        this.multiCloudPool = multiCloudPool;
    }

    @Override
    public Response listCloudPools() {
        return handleRequest(() -> {
            List<String> poolUrls = new ArrayList<>();
            for (String instanceName : getMultiCloudPool().list()) {
                poolUrls.add(requestUri().getAbsolutePath() + "/" + instanceName);
            }
            return Response.ok(poolUrls).build();
        });
    }

    @Override
    public Response createCloudPool(final String cloudPoolName) {
        return handleRequest(() -> {
            getMultiCloudPool().create(cloudPoolName);
            return Response.created(requestUri().getRequestUri()).build();
        });
    }

    @Override
    public Response deleteCloudPool(final String cloudPoolName) {
        return handleRequest(() -> {
            getMultiCloudPool().delete(cloudPoolName);
            return Response.ok().build();
        });
    }

    @Override
    public Response getConfig(final String cloudPoolName) {
        return handleRequest(() -> {
            Optional<JsonObject> config = getMultiCloudPool().get(cloudPoolName).getConfiguration();
            if (!config.isPresent()) {
                throw new NotFoundException("no cloud pool configuration has been set");
            }
            return Response.ok(config.get()).build();
        });
    }

    @Override
    public Response setConfig(final String cloudPoolName, final JsonObject configuration) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            instance.configure(configuration);
            return Response.ok().build();
        });
    }

    @Override
    public Response start(final String cloudPoolName) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            instance.start();
            return Response.ok().build();
        });
    }

    @Override
    public Response stop(final String cloudPoolName) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            instance.stop();
            return Response.ok().build();
        });
    }

    @Override
    public Response getStatus(final String cloudPoolName) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            return Response.ok().entity(instance.getStatus()).build();
        });
    }

    @Override
    public Response getPool(final String cloudPoolName) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            return Response.ok().entity(instance.getMachinePool()).build();
        });
    }

    @Override
    public Response setDesiredSize(final String cloudPoolName, final SetDesiredSizeRequest request) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            instance.setDesiredSize(request.getDesiredSize());
            return Response.ok().build();
        });
    }

    @Override
    public Response getPoolSize(final String cloudPoolName) {
        return handleRequest(() -> {
            CloudPoolInstance instance = getMultiCloudPool().get(cloudPoolName);
            return Response.ok().entity(instance.getPoolSize()).build();
        });
    }

    @Override
    public Response terminateMachine(final String cloudPoolName, final String machineId,
            final TerminateMachineRequest request) {
        return handleRequest(() -> {
            getMultiCloudPool().get(cloudPoolName).terminateMachine(machineId, request.isDecrementDesiredSize());
            return Response.ok().build();
        });
    }

    @Override
    public Response detachMachine(final String cloudPoolName, final String machineId,
            final DetachMachineRequest request) {
        return handleRequest(() -> {
            getMultiCloudPool().get(cloudPoolName).detachMachine(machineId, request.isDecrementDesiredSize());
            return Response.ok().build();
        });
    }

    @Override
    public Response attachMachine(final String cloudPoolName, final String machineId) {
        return handleRequest(() -> {
            getMultiCloudPool().get(cloudPoolName).attachMachine(machineId);
            return Response.ok().build();
        });
    }

    @Override
    public Response setServiceState(final String cloudPoolName, final String machineId,
            final SetServiceStateRequest request) {
        return handleRequest(() -> {
            getMultiCloudPool().get(cloudPoolName).setServiceState(machineId, request.getServiceState());
            return Response.ok().build();
        });
    }

    @Override
    public Response setMembershipStatus(final String cloudPoolName, final String machineId,
            final SetMembershipStatusRequest request) {
        return handleRequest(() -> {
            getMultiCloudPool().get(cloudPoolName).setMembershipStatus(machineId, request.getMembershipStatus());
            return Response.ok().build();
        });
    }

    private MultiCloudPool getMultiCloudPool() {
        return this.multiCloudPool;
    }

    /**
     * Request information that gets injected by the JAX-RS runtime for each
     * method invocation. As such, its use is thread-safe.
     *
     * @return
     */
    private UriInfo requestUri() {
        return this.requestUri;
    }

    /**
     * Executes the given request handler code and returns whatever
     * {@link Response} it produces. In case the request handler throws an
     * exception, that exception is turned into an error {@link Response}.
     *
     * @param requestHandler
     * @return The {@link Response} produced by the request handler or, in case
     *         an exception was thrown, a {@link Response} with an error code
     *         set to incdicate the type of error.
     */
    private Response handleRequest(Callable<Response> requestHandler) {
        try {
            return requestHandler.call();
        } catch (IllegalArgumentException | NotStartedException | NotConfiguredException e) {
            return badRequestErrorResponse(e);
        } catch (NotFoundException e) {
            return notFoundErrorResponse(e);
        } catch (CloudPoolException e) {
            return cloudErrorResponse(e);
        } catch (Exception e) {
            return internalErrorResponse(e);
        }
    }

    /**
     * Produces a {@code 400} (Bad Request) error for a given exception.
     *
     * @param error
     * @return
     */
    private Response badRequestErrorResponse(Exception error) {
        LOG.error(error.getMessage());
        return Response.status(Status.BAD_REQUEST).entity(new ErrorType(error.getMessage())).build();
    }

    /**
     * Produces a {@code 404} (Not Found) error for a given
     * {@link NotFoundException}.
     *
     * @param error
     * @return
     */
    private Response notFoundErrorResponse(NotFoundException error) {
        LOG.error(error.getMessage());
        return Response.status(Status.NOT_FOUND).entity(new ErrorType(error.getMessage())).build();
    }

    /**
     * Produces a {@code 502} response for a {@link CloudPoolException} raised
     * by a {@link CloudPool}.
     *
     * @param error
     * @return
     */
    private Response cloudErrorResponse(CloudPoolException error) {
        LOG.error(error.getMessage());
        return Response.status(Status.BAD_GATEWAY).entity(new ErrorType(error.getMessage())).build();
    }

    /**
     * Produces a {@code 500} (Internal Server Error) response for a given
     * {@link Exception}.
     *
     * @param error
     * @return
     */
    private Response internalErrorResponse(Exception error) {
        LOG.error(error.getMessage(), error);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(error.getMessage())).build();
    }

}
