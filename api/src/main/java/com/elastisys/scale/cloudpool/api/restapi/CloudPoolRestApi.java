package com.elastisys.scale.cloudpool.api.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.google.gson.JsonObject;

/**
 * The {@link CloudPool} REST API. For additional details, refer to the
 * <a href="http://cloudpoolrestapi.readthedocs.io/en/latest/">official cloud
 * pool API documentation</a>.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CloudPoolRestApi {

    /**
     * Retrieves the configuration currently set for the {@link CloudPool}.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @GET
    @Path("/config")
    Response getConfig();

    /**
     * Sets the configuration for the {@link CloudPool}.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param configuration
     *            The (JSON) configuration document to set.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/config")
    Response setConfig(JsonObject configuration);

    /**
     * Starts the cloud pool.
     * <p/>
     * This will set the cloud pool in an activated state where it will start to
     * accept requests to query or modify the machine pool.
     * <p/>
     * If the cloud pool has not been configured the method should fail. If the
     * cloud pool is already started this is a no-op.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/start")
    Response start();

    /**
     * Stops the cloud pool.
     * <p/>
     * A stopped cloud pool is in a passivated state and will not accept any
     * requests to query or modify the machine pool.
     * <p/>
     * If the cloud pool is already in a stopped state this is a no-op.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/stop")
    Response stop();

    /**
     * Retrieves the execution status for the cloud pool.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @GET
    @Path("/status")
    Response getStatus();

    /**
     * Retrieves the current machine pool members.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @GET
    @Path("/pool")
    Response getPool();

    /**
     * Sets the desired number of machines in the machine pool. This method is
     * asynchronous in that the method returns immediately without having
     * carried out any required changes to the machine pool.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param request
     *            A {@link SetDesiredSizeRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/size")
    Response setDesiredSize(SetDesiredSizeRequest request);

    /**
     * Returns the current size of the machine pool -- both in terms of the
     * desired size and the actual size (as these may differ at any time).
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @GET
    @Path("/pool/size")
    Response getPoolSize();

    /**
     * Terminates a particular machine pool member. The caller can control if a
     * replacement machine is to be provisioned.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param machineId
     *            The identifier of the machine to terminate.
     * @param request
     *            A {@link TerminateMachineRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/terminate")
    Response terminateMachine(@PathParam("machine") String machineId, TerminateMachineRequest request);

    /**
     * Removes a member from the pool without terminating it. The machine keeps
     * running but is no longer considered a pool member and, therefore, needs
     * to be managed independently. The caller can control if a replacement
     * machine is to be provisioned.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param machineId
     *            The identifier of the machine to detach from the pool.
     * @param request
     *            A {@link DetachMachineRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/detach")
    Response detachMachine(@PathParam("machine") String machineId, DetachMachineRequest request);

    /**
     * Attaches an already running machine instance to the pool, growing the
     * pool with a new member. This operation implies that the desired size of
     * the pool is incremented by one.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param machineId
     *            The identifier of the machine to attach to the pool.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/attach")
    Response attachMachine(@PathParam("machine") String machineId);

    /**
     * Sets the service state of a given machine pool member. Setting the
     * service state does not have any functional implications on the pool
     * member, but should be seen as way to supply operational information about
     * the service running on the machine to third-party services (such as load
     * balancers).
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param machineId
     *            The machine whose service state is to be set.
     * @param request
     *            A {@link SetServiceStateRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/serviceState")
    Response setServiceState(@PathParam("machine") String machineId, SetServiceStateRequest request);

    /**
     * Sets the membership status of a given pool member. The membership status
     * for a machine can be set to protect the machine from being terminated (by
     * setting its evictability status) and/or to mark a machine as being in
     * need of replacement by flagging it as an inactive pool member.
     * <p/>
     * More details can be found in the <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html">official API
     * documentation</a>.
     *
     * @param machineId
     *            The machine whose service state is to be set.
     * @param request
     *            A {@link SetMembershipStatusRequest}.
     * @return A response message as per the
     *         <a href="http://cloudpoolrestapi.readthedocs.org/">cloud pool
     *         REST API</a>.
     */
    @POST
    @Path("/pool/{machine}/membershipStatus")
    Response setMembershipStatus(@PathParam("machine") String machineId, SetMembershipStatusRequest request);
}
