package com.elastisys.scale.cloudpool.api.restapi.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.cloudpool.api.restapi.types.AttachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

/**
 * /** Implements the cloud pool REST API, which is fully covered in the
 * <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud pool REST API documentation</a>.
 */
@Path("/")
public class CloudPoolRestApiImpl implements CloudPoolRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(CloudPoolRestApiImpl.class);
    /**
     * Default file name (within the storage directory) in which
     * {@link CloudPool} configuration is stored.
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.json";

    /** The {@link CloudPool} back-end to which all work is delegated. */
    private final CloudPool cloudPool;
    /**
     * The directory where runtime state for the {@link CloudPool} is stored.
     * The {@link CloudPoolRestApiImpl} will use this directory to store every
     * set configuration so that it can be restored on restart.
     */
    private final String storageDir;

    /**
     * The file name, within the {@link #storageDir}, in which {@link CloudPool}
     * configuration is stored.
     */
    private final String configFileName;

    /**
     * Creates a {@link CloudPoolRestApiImpl} that will store set
     * {@link CloudPool} configurations under a given storage directory with the
     * {@link #DEFAULT_CONFIG_FILE_NAME}.
     *
     * @param cloudPool
     *            The back-end {@link CloudPool} that is being managed.
     * @param storageDir
     *            The directory path where runtime state for the
     *            {@link CloudPool} is stored. The {@link CloudPoolRestApiImpl}
     *            will use this directory to store every set configuration so
     *            that it can be restored on restart. The directory will be
     *            created if it does not exist.
     */
    public CloudPoolRestApiImpl(CloudPool cloudPool, String storageDir) {
        this(cloudPool, storageDir, DEFAULT_CONFIG_FILE_NAME);
    }

    /**
     * Creates a {@link CloudPoolRestApiImpl} that will store set
     * {@link CloudPool} configurations under a given storage directory with a
     * given file name.
     *
     * @param cloudPool
     *            The back-end {@link CloudPool} that is being managed.
     * @param storageDir
     *            The directory path where runtime state for the
     *            {@link CloudPool} is stored. The {@link CloudPoolRestApiImpl}
     *            will use this directory to store every set configuration so
     *            that it can be restored on restart. The directory will be
     *            created if it does not exist.
     * @param configFileName
     *            The file name, within the {@link #storageDir}, in which
     *            {@link CloudPool} configuration is stored.
     */
    public CloudPoolRestApiImpl(CloudPool cloudPool, String storageDir, String configFileName) {
        LOG.info(getClass().getSimpleName() + " created");
        checkArgument(cloudPool != null, "cloudPool cannot be null");
        File storageDirectory = new File(storageDir);
        if (!storageDirectory.exists()) {
            prepareStorageDir(storageDirectory);
        } else {
            checkArgument(storageDirectory.isDirectory(), "cloud pool storageDir %s is not a directory", storageDir);
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
    private void prepareStorageDir(File storageDirectory) throws IllegalArgumentException {
        LOG.info("creating storage directory {}", storageDirectory.getAbsolutePath());
        if (!storageDirectory.mkdirs()) {
            throw new IllegalArgumentException(
                    String.format("cloud pool: failed to create specified " + "storage directory %s",
                            storageDirectory.getAbsolutePath()));
        }
    }

    @Override
    public Response getConfig() {
        try {
            Optional<JsonObject> configuration = this.cloudPool.getConfiguration();
            if (!configuration.isPresent()) {
                ErrorType entity = new ErrorType("no cloud pool configuration has been set");
                return Response.status(Status.NOT_FOUND).entity(entity).build();
            }
            return Response.ok(configuration.get()).build();
        } catch (Exception e) {
            return internalErrorResponse("internal error on GET /config", e);
        }
    }

    @Override
    public Response setConfig(JsonObject configuration) {
        try {
            this.cloudPool.configure(configuration);
            storeConfig(configuration);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            String message = "illegal input: " + e.getMessage();
            LOG.error(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse("failure to process POST /config", e);
        } catch (Exception e) {
            return internalErrorResponse("internal error on POST /config", e);
        }
    }

    @Override
    public Response start() {
        try {
            this.cloudPool.start();
            return Response.ok().build();
        } catch (NotConfiguredException e) {
            LOG.error(e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(e.getMessage(), e)).build();
        } catch (Exception e) {
            String message = "failure to start cloud pool: " + e.getMessage();
            LOG.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    @Override
    public Response stop() {
        try {
            this.cloudPool.stop();
            return Response.ok().build();
        } catch (Exception e) {
            String message = "failure to stop cloud pool: " + e.getMessage();
            LOG.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    @Override
    public Response getStatus() {
        try {
            CloudPoolStatus status = this.cloudPool.getStatus();
            return Response.ok().entity(status).build();
        } catch (Exception e) {
            String message = "failure to get cloud pool execution status: " + e.getMessage();
            LOG.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
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
        Files.write(JsonUtils.toPrettyString(configuration), getCloudPoolConfigPath().toFile(), Charsets.UTF_8);
    }

    /**
     * Returns the file system path where the {@link CloudPoolRestApiImpl}
     * stores received {@link CloudPool} configurations.
     *
     * @param storageDir
     * @return
     */
    public java.nio.file.Path getCloudPoolConfigPath() {
        return Paths.get(this.storageDir, this.configFileName);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Response terminateMachine(TerminateMachineRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.terminateMachine(request.getMachineId(), request.isDecrementDesiredSize());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/terminate"), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/terminate"), e);
        }
    }

    @Override
    public Response detachMachine(DetachMachineRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.detachMachine(request.getMachineId(), request.isDecrementDesiredSize());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/detach"), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/detach"), e);
        }
    }

    @Override
    public Response attachMachine(AttachMachineRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.attachMachine(request.getMachineId());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/attach"), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/attach"), e);
        }
    }

    @Override
    public Response setServiceState(SetServiceStateRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.setServiceState(request.getMachineId(), request.getServiceState());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/serviceState"), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/serviceState"), e);
        }
    }

    @Override
    public Response setMembershipStatus(SetMembershipStatusRequest request) {
        requireStartedCloudPool();

        try {
            this.cloudPool.setMembershipStatus(request.getMachineId(), request.getMembershipStatus());
            return Response.ok().build();
        } catch (NotFoundException e) {
            String message = "unrecognized machine: " + e.getMessage();
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (CloudPoolException e) {
            return cloudErrorResponse(String.format("failure to process POST /pool/membershipStatus"), e);
        } catch (Exception e) {
            return internalErrorResponse(String.format("internal error on POST /pool/membershipStatus"), e);
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
