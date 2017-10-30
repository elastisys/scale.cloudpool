package com.elastisys.scale.cloudpool.openstack.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.openstack.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.openstack.requests.AssignFloatingIpRequest;
import com.elastisys.scale.cloudpool.openstack.requests.CreateServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerMetadataRequest;
import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.GetServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.ListServersWithTagRequest;
import com.elastisys.scale.cloudpool.openstack.requests.UpdateServerMetadataRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * Standard implementation of the {@link OpenstackClient} interface.
 */
public class StandardOpenstackClient implements OpenstackClient {

    /**
     * {@link OSClientFactory} configured to use the latest set
     * {@link CloudApiSettings}.
     */
    private OSClientFactory clientFactory;

    /** The currently set configuration. */
    private CloudApiSettings config;

    public StandardOpenstackClient() {
        this.clientFactory = null;
    }

    @Override
    public void configure(CloudApiSettings configuration) {
        checkArgument(configuration != null, "null configuration");

        this.clientFactory = new OSClientFactory(configuration);

        this.config = configuration;
    }

    @Override
    public List<Server> getServers(String tag, String tagValue) throws ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new ListServersWithTagRequest(clientFactory(), tag, tagValue).call();
    }

    @Override
    public Server getServer(String serverId) throws NotFoundException, ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new GetServerRequest(clientFactory(), serverId).call();
    }

    @Override
    public Server launchServer(String serverName, ProvisioningTemplate provisioningDetails, Map<String, String> tags)
            throws ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        CreateServerRequest request = new CreateServerRequest(clientFactory(), serverName,
                provisioningDetails.getSize(), provisioningDetails.getImage(), provisioningDetails.getKeyPair(),
                provisioningDetails.getSecurityGroups(), provisioningDetails.getNetworks(),
                provisioningDetails.getEncodedUserData(), tags);
        return request.call();
    }

    @Override
    public String assignFloatingIp(String serverId) throws NotFoundException, ResponseException {
        Server server = getServer(serverId);
        return new AssignFloatingIpRequest(clientFactory(), server).call();
    }

    @Override
    public void terminateServer(String serverId) throws NotFoundException, ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");
        new DeleteServerRequest(clientFactory(), serverId).call();
    }

    @Override
    public void tagServer(String serverId, Map<String, String> tags) throws ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        new UpdateServerMetadataRequest(clientFactory(), serverId, tags).call();
    }

    @Override
    public void untagServer(String serverId, List<String> tagKeys) throws ResponseException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        new DeleteServerMetadataRequest(clientFactory(), serverId, tagKeys).call();
    }

    private boolean isConfigured() {
        return config() != null;
    }

    public OSClientFactory clientFactory() {
        return this.clientFactory;
    }

    private CloudApiSettings config() {
        return this.config;
    }

}
