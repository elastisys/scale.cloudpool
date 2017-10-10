package com.elastisys.scale.cloudpool.multipool.restapi.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.cloudpool.api.restapi.types.AttachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.google.gson.JsonObject;

/**
 * A {@link MultiCloudPoolRestApi} is a REST API endpoint that offers management
 * (create/delete) and provides access to a collection of {@link CloudPool}
 * instances.
 * <p/>
 * Whereas a {@link CloudPoolRestApi} is a singleton API that publishes
 * <i>one</i> {@link CloudPool}, a {@link MultiCloudPoolRestApi} allows a server
 * to host multiple {@link CloudPool} instances.
 * <p/>
 * The {@link CloudPool}s created by the {@link MultiCloudPoolRestApi} are
 * published under the {@code /cloudpools} resource, and the
 * <a href="http://cloudpoolrestapi.readthedocs.io/en/latest/">API</a> resources
 * for a given {@link CloudPool} instance are available under
 * {@code /cloudpools/<cloudPoolName>/} .
 *
 * @see MultiCloudPool
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MultiCloudPoolRestApi {

    //
    // Factory operations
    //

    /**
     * Retrieves the URLs of all {@link CloudPool} instances in the collection.
     *
     * @return On success: a {@code 200} response message with a JSON array of
     *         URLs. On error: a {@code non-2XX} response code with an error
     *         response message as described in the <a href=
     *         "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message">cloudpool
     *         REST API</a>.
     */
    @GET
    @Path("/cloudpools")
    Response listCloudPools();

    /**
     * Creates a new {@link CloudPool} instance and adds it to the collection.
     * <p/>
     * The created instance will be in an unconfigured and unstarted state and
     * the instance's <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">API</a> will
     * be available under `/cloudpool/<name>/`.
     *
     * @param cloudPoolName
     *            The name of the {@link CloudPool} instance.
     * @return On success: a {@code 201} response message with a
     *         {@code Location} header specifying the URL of the created
     *         instance. On error: a {@code non-2XX} response code with an error
     *         response message as described in the <a href=
     *         "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message">cloudpool
     *         REST API</a>.
     */
    @POST
    @Path("/cloudpools/{cloudPoolName}")
    Response createCloudPool(@PathParam("cloudPoolName") String cloudPoolName);

    /**
     * Deletes the {@link CloudPool} instance with the given name from the
     * collection.
     *
     * @param cloudPoolName
     *            The name of the {@link CloudPool} instance to delete.
     * @return On success: a {@code 200} response message. On error: a
     *         {@code non-2XX} response code with an error response message as
     *         described in the <a href=
     *         "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message">cloudpool
     *         REST API</a>.
     */
    @DELETE
    @Path("/cloudpools/{cloudPoolName}")
    Response deleteCloudPool(@PathParam("cloudPoolName") String cloudPoolName);

    //
    // Instance operations
    //

    @GET
    @Path("/cloudpools/{cloudPoolName}/config")
    Response getConfig(@PathParam("cloudPoolName") String cloudPoolName);

    @POST
    @Path("/cloudpools/{cloudPoolName}/config")
    Response setConfig(@PathParam("cloudPoolName") String cloudPoolName, JsonObject configuration);

    @POST
    @Path("/cloudpools/{cloudPoolName}/start")
    Response start(@PathParam("cloudPoolName") String cloudPoolName);

    @POST
    @Path("/cloudpools/{cloudPoolName}/stop")
    Response stop(@PathParam("cloudPoolName") String cloudPoolName);

    @GET
    @Path("/cloudpools/{cloudPoolName}/status")
    Response getStatus(@PathParam("cloudPoolName") String cloudPoolName);

    @GET
    @Path("/cloudpools/{cloudPoolName}/pool")
    Response getPool(@PathParam("cloudPoolName") String cloudPoolName);

    @POST
    @Path("/cloudpools/{cloudPoolName}/pool/size")
    Response setDesiredSize(@PathParam("cloudPoolName") String cloudPoolName, SetDesiredSizeRequest request);

    @GET
    @Path("/cloudpools/{cloudPoolName}/pool/size")
    Response getPoolSize(@PathParam("cloudPoolName") String cloudPoolName);

    @POST
    @Path("/cloudpools/{cloudPoolName}/terminate")
    Response terminateMachine(@PathParam("cloudPoolName") String cloudPoolName, TerminateMachineRequest request);

    @POST
    @Path("/cloudpools/{cloudPoolName}/detach")
    Response detachMachine(@PathParam("cloudPoolName") String cloudPoolName, DetachMachineRequest request);

    @POST
    @Path("/cloudpools/{cloudPoolName}/attach")
    Response attachMachine(@PathParam("cloudPoolName") String cloudPoolName, AttachMachineRequest request);

    @POST
    @Path("/cloudpools/{cloudPoolName}/serviceState")
    Response setServiceState(@PathParam("cloudPoolName") String cloudPoolName, SetServiceStateRequest request);

    @POST
    @Path("/cloudpools/{cloudPoolName}/membershipStatus")
    Response setMembershipStatus(@PathParam("cloudPoolName") String cloudPoolName, SetMembershipStatusRequest request);
}
