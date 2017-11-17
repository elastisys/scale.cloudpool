package com.elastisys.scale.cloudpool.api.client;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.Optional;

import javax.ws.rs.core.Response.Status;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.restapi.types.AttachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} REST API client.
 */
public class CloudPoolClient implements CloudPool {

    /**
     * The {@link AuthenticatedHttpClient} used to communicate over the REST
     * API.
     */
    private final AuthenticatedHttpClient httpClient;
    /** Host/IP address of the {@link CloudPool}. */
    private final String cloudPoolHost;
    /** The port on which the {@link CloudPool} server is listening. */
    private final int cloudPoolPort;

    /**
     * Constructs a {@link CloudPoolClient} for a given {@link CloudPool}
     * endpoint, using a given {@link AuthenticatedHttpClient} for
     * communication.
     *
     * @param httpClient
     *            The {@link AuthenticatedHttpClient} used to communicate over
     *            the REST API.
     * @param cloudPoolHost
     *            Host/IP address of the {@link CloudPool}.
     * @param cloudPoolPort
     *            The port on which the {@link CloudPool} server is listening.
     */
    public CloudPoolClient(AuthenticatedHttpClient httpClient, String cloudPoolHost, int cloudPoolPort) {
        this.httpClient = httpClient;
        this.cloudPoolHost = cloudPoolHost;
        this.cloudPoolPort = cloudPoolPort;
    }

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        checkArgument(configuration != null, "null configuration now allowed");

        String url = fullUrl("/config");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(JsonUtils.toPrettyString(configuration), ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.BAD_REQUEST.getStatusCode()) {
                throw new IllegalArgumentException(
                        format("failed to set cloud pool config for %s: " + "bad request: %s", url, e.getMessage()), e);
            }
            throw new CloudPoolException(format("failed to set cloud pool config: %s: %s", url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to set cloud pool config: %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public Optional<JsonObject> getConfiguration() {
        String url = fullUrl("/config");
        try {
            HttpGet request = new HttpGet(url);
            JsonObject config = responseToJson(this.httpClient.execute(request)).getAsJsonObject();
            return Optional.of(config);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.empty();
            }
            throw new CloudPoolException(format("failed to get cloud pool config: %s: %s", url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to get cloud pool config: %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public void start() throws NotConfiguredException {
        String url = fullUrl("/start");
        try {
            HttpPost request = new HttpPost(url);
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            String message = format("failed to start cloud pool %s: %s", url, e.getMessage());
            if (e.getStatusCode() == Status.BAD_REQUEST.getStatusCode()) {
                throw new NotConfiguredException(message, e);
            }
            throw new CloudPoolException(message, e);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to start cloud pool %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public void stop() {
        String url = fullUrl("/stop");
        try {
            HttpPost request = new HttpPost(url);
            this.httpClient.execute(request);
        } catch (Exception e) {
            String message = format("failed to stop cloud pool %s: %s", url, e.getMessage());
            throw new CloudPoolException(message, e);
        }
    }

    @Override
    public CloudPoolStatus getStatus() {
        String url = fullUrl("/status");
        try {
            HttpGet request = new HttpGet(url);
            HttpRequestResponse response = this.httpClient.execute(request);
            return responseToObject(response, CloudPoolStatus.class);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to get cloud pool status: %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
        String url = fullUrl("/pool");
        try {
            HttpGet request = new HttpGet(url);
            HttpRequestResponse response = this.httpClient.execute(request);
            return responseToObject(response, MachinePool.class);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to get cloud pool metadata: %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {

        String url = fullUrl("/pool/size");
        try {
            HttpRequestResponse response = this.httpClient.execute(new HttpGet(url));
            return responseToObject(response, PoolSizeSummary.class);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to retrieve pool size from " + "cloud pool at %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public void setDesiredSize(int desiredSize)
            throws IllegalArgumentException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/size");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(
                    new StringEntity(JsonUtils.toPrettyString(JsonUtils.toJson(new SetDesiredSizeRequest(desiredSize))),
                            ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.BAD_REQUEST.getStatusCode()) {
                throw new IllegalArgumentException(format(
                        "failed to set desired size for cloud pool %s: " + "bad request: %s", url, e.getMessage()), e);
            }
            throw new CloudPoolException(
                    format("failed to set desired size for cloud pool %s: %s", url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to set desired size for cloud pool %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/terminate");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(
                    JsonUtils.toPrettyString(
                            JsonUtils.toJson(new TerminateMachineRequest(machineId, decrementDesiredSize))),
                    ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(format("failed to terminate machine %s in cloud pool %s: bad request: %s",
                        machineId, url, e.getMessage()), e);
            }
            throw new CloudPoolException(
                    format("failed to terminate machine %s in cloud pool %s: %s", machineId, url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to terminate machine %s in cloud pool %s: %s", machineId, url, e.getMessage()), e);
        }
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/serviceState");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(
                    JsonUtils.toPrettyString(JsonUtils.toJson(new SetServiceStateRequest(machineId, serviceState))),
                    ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(
                        format("failed to set service state for machine %s in cloud pool %s: " + "bad request: %s",
                                machineId, url, e.getMessage()),
                        e);
            }
            throw new CloudPoolException(format("failed to set service state for " + "machine %s in cloud pool %s: %s",
                    machineId, url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(format("failed to set service state for " + "machine %s in cloud pool %s: %s",
                    machineId, url, e.getMessage()), e);

        }
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/membershipStatus");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(
                    JsonUtils.toPrettyString(
                            JsonUtils.toJson(new SetMembershipStatusRequest(machineId, membershipStatus))),
                    ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(
                        format("failed to set membership status for machine %s in cloud pool %s: " + "bad request: %s",
                                machineId, url, e.getMessage()),
                        e);
            }
            throw new CloudPoolException(
                    format("failed to set membership status for " + "machine %s in cloud pool %s: %s", machineId, url,
                            e.getMessage()),
                    e);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to set membership status for " + "machine %s in cloud pool %s: %s", machineId, url,
                            e.getMessage()),
                    e);
        }
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/attach");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(
                    new StringEntity(JsonUtils.toPrettyString(JsonUtils.toJson(new AttachMachineRequest(machineId))),
                            ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(format("failed to attach machine %s to cloud pool %s: " + "bad request: %s",
                        machineId, url, e.getMessage()), e);
            }
            throw new CloudPoolException(
                    format("failed to attach " + "machine %s in cloud pool %s: %s", machineId, url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to attach " + "machine %s in cloud pool %s: %s", machineId, url, e.getMessage()), e);
        }
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        String url = fullUrl("/pool/detach");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(
                    JsonUtils.toPrettyString(
                            JsonUtils.toJson(new DetachMachineRequest(machineId, decrementDesiredSize))),
                    ContentType.APPLICATION_JSON));
            this.httpClient.execute(request);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(
                        format("failed to detach machine %s from cloud pool %s: " + "bad request: %s", machineId, url,
                                e.getMessage()),
                        e);
            }
            throw new CloudPoolException(
                    format("failed to detach machine %s from cloud pool %s: %s", machineId, url, e.getMessage()), e);
        } catch (Exception e) {
            throw new CloudPoolException(
                    format("failed to detach machine %s from cloud pool %s: %s", machineId, url, e.getMessage()), e);
        }
    }

    /**
     * Returns the base HTTPS URL of the {@link CloudPool}. For instance,
     * {@code https://1.2.3.4:8443}.
     *
     * @return
     */
    private String baseUrl() {
        return String.format("https://%s:%d", this.cloudPoolHost, this.cloudPoolPort);
    }

    /**
     * Returns the full URL to a particular path on the {@link CloudPool}
     * server.
     *
     * @param path
     *            Path relative to the {@link #baseUrl()} of the
     *            {@link CloudPool}.
     * @return
     */
    private String fullUrl(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return String.format("%s%s", baseUrl(), path);
    }

    /**
     * Attempts to convert a {@link HttpRequestResponse}, to JSON.
     *
     * @param response
     *            The response message.
     * @return
     */
    private JsonElement responseToJson(HttpRequestResponse response) {
        return JsonUtils.parseJsonString(response.getResponseBody());
    }

    /**
     * Attempts to convert a {@link HttpRequestResponse}, containing JSON data,
     * to a Java object of a given type.
     *
     * @param response
     *            The response message.
     * @param type
     *            The type of Java object to convert to.
     * @return
     */
    private <T> T responseToObject(HttpRequestResponse response, Class<T> type) {
        return JsonUtils.toObject(JsonUtils.parseJsonString(response.getResponseBody()), type);
    }

}
