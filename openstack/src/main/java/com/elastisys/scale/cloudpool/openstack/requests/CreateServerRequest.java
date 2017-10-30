package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.Network;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * An request that, when executed, creates a new server instance.
 */
public class CreateServerRequest extends AbstractOpenstackRequest<Server> {

    /** The name to assign to the new server. */
    private final String serverName;
    /** The name of the machine type (flavor) to launch. */
    private final String flavorName;
    /** The identifier of the machine image used to boot the machine. */
    private final String imageName;
    /**
     * The name of the key pair to use for the machine instance. May be
     * <code>null</code>.
     */
    private final String keyPair;
    /**
     * The security group(s) to use for the new machine instance. May be
     * <code>null</code>.
     */
    private final List<String> securityGroups;

    /**
     * The names of the networks to attach the launched server to (for example,
     * {@code private}). Each network creates a separate network interface
     * controller (NIC) on the created server. Typically, this option can be
     * left out, but in rare cases, when an account has more than one network to
     * choose from, the OpenStack API forces us to be explicit about the
     * network(s) we want to use.
     * <p/>
     * If set to <code>null</code> or left empty, the default behavior is to use
     * which ever network is configured by the cloud provider for the
     * user/project. However, if there are multiple choices, this will cause
     * server boot requests to fail.
     */
    private final List<String> networks;
    /**
     * The base64-encoded user data used to contextualize the started instance.
     * May be <code>null</code>.
     */
    private final String encodedUserData;
    /** Any meta data tags to assign to the new server. */
    private final Map<String, String> metadata;

    /**
     * Creates a new {@link CreateServerRequest}.
     *
     * @param clientFactory
     *            OpenStack API client factory.
     * @param serverName
     *            The name to assign to the new server.
     * @param flavorName
     *            The name of the machine type (flavor) to launch.
     * @param imageName
     *            The identifier of the machine image used to boot the machine.
     * @param keyPair
     *            The name of the key pair to use for the machine instance. May
     *            be <code>null</code>.
     * @param securityGroups
     *            The security group(s) to use for the new machine instance. May
     *            be <code>null</code>.
     * @param networks
     *            The names of the networks to attach the launched server to
     *            (for example, {@code private}). Each network creates a
     *            separate network interface controller (NIC) on the created
     *            server. Typically, this option can be left out, but in rare
     *            cases, when an account has more than one network to choose
     *            from, the OpenStack API forces us to be explicit about the
     *            network(s) we want to use.
     *            <p/>
     *            If set to <code>null</code> or left empty, the default
     *            behavior is to use which ever network is configured by the
     *            cloud provider for the user/project. However, if there are
     *            multiple choices, this will cause server boot requests to
     *            fail.
     * @param encodedUserData
     *            The base64-encoded user data used to contextualize the started
     *            instance. May be <code>null</code>.
     * @param metadata
     *            Any meta data tags to assign to the new server. May be
     *            <code>null</code>.
     */
    public CreateServerRequest(OSClientFactory clientFactory, String serverName, String flavorName, String imageName,
            String keyPair, List<String> securityGroups, List<String> networks, String encodedUserData,
            Map<String, String> metadata) throws ResponseException {
        super(clientFactory);

        checkNotNull(serverName, "server name cannot be null");
        checkNotNull(flavorName, "flavor name cannot be null");
        checkNotNull(imageName, "image name cannot be null");

        this.serverName = serverName;
        this.flavorName = flavorName;
        this.imageName = imageName;
        this.keyPair = keyPair;
        this.securityGroups = securityGroups == null ? new ArrayList<>() : securityGroups;
        this.networks = networks;
        this.encodedUserData = encodedUserData;
        this.metadata = metadata == null ? new HashMap<>() : metadata;
    }

    @Override
    public Server doRequest(OSClient api) throws NotFoundException, ResponseException {
        ServerService serverService = api.compute().servers();

        ServerCreateBuilder serverCreateBuilder = serverService.serverBuilder().name(this.serverName)
                .flavor(getFlavorId()).image(getImageId()).keypairName(this.keyPair).addMetadata(this.metadata);

        for (String securityGroup : this.securityGroups) {
            serverCreateBuilder.addSecurityGroup(securityGroup);
        }
        if (this.networks != null && !this.networks.isEmpty()) {
            serverCreateBuilder.networks(getNetworkIds());
        }
        if (this.encodedUserData != null) {
            serverCreateBuilder.userData(this.encodedUserData);
        }

        ServerCreate serverCreate = serverCreateBuilder.build();
        Server server = serverService.boot(serverCreate);
        // first call to boot only seem to return a stripped Server object
        // (missing fields such as status). re-fetch it before returning.
        return serverService.get(server.getId());
    }

    /**
     * Returns the flavor identifier that corresponds to the specified flavor
     * name.
     *
     * @return
     * @throws NotFoundException
     *             If the specified flavor name doesn't exist.
     * @throws ResponseException
     *             On communication errors.
     */
    private String getFlavorId() throws NotFoundException, ResponseException {
        List<Flavor> flavors = new ListSizesRequest(getClientFactory()).call();
        for (Flavor flavor : flavors) {
            if (flavor.getName().equals(this.flavorName)) {
                return flavor.getId();
            }
        }
        throw new NotFoundException(String.format("no flavor with name \"%s\" exists in region \"%s\"", this.flavorName,
                getApiAccessConfig().getRegion()));
    }

    /**
     * Returns the image identifier that corresponds to the specified image
     * name.
     *
     * @return
     * @throws NotFoundException
     *             If the specified image name doesn't exist.
     * @throws ResponseException
     *             On communication errors.
     */
    private String getImageId() throws NotFoundException, ResponseException {

        List<Image> images = new ListImagesRequest(getClientFactory()).call();
        for (Image image : images) {
            if (image.getName().equals(this.imageName)) {
                return image.getId();
            }
        }
        throw new NotFoundException(String.format("no image with name \"%s\" exists in region \"%s\"", this.imageName,
                getApiAccessConfig().getRegion()));
    }

    /**
     * Returns the network identifiers that correspond to the specified set of
     * network names.
     *
     * @return
     * @throws NotFoundException
     *             If any of the specified network names does not exist.
     * @throws ResponseException
     *             On communication errors.
     */
    private List<String> getNetworkIds() throws NotFoundException, ResponseException {
        List<String> networkIds = new LinkedList<>();

        if (this.networks == null) {
            return networkIds;
        }

        // retrieve all available networks
        List<Network> availableNetworks = new ListNetworksRequest(getClientFactory()).call();

        // translate the requested network names to their corresponding ids
        for (String networkName : this.networks) {
            Optional<Network> matchingNetwork = availableNetworks.stream()
                    .filter(network -> network.getName().equals(networkName)).findFirst();
            if (!matchingNetwork.isPresent()) {
                throw new NotFoundException(String.format("no network with name \"%s\" exists in region \"%s\"",
                        networkName, getApiAccessConfig().getRegion()));
            }
            networkIds.add(matchingNetwork.get().getId());
        }

        return networkIds;
    }
}
