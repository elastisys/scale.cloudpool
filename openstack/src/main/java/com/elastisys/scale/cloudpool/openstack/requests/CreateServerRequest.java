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
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.Network;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.base.Charsets;

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
			Map<String, String> metadata) {
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
	public Server doRequest(OSClient api) {
		ServerService serverService = api.compute().servers();

		ServerCreateBuilder serverCreateBuilder = serverService.serverBuilder().name(this.serverName)
				.flavor(getFlavorId()).image(getImageId()).keypairName(this.keyPair).addMetadata(this.metadata);

		logDebugInfo();

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
	 * @throws IllegalArgumentException
	 *             If the specified flavor name doesn't exist.
	 * @throws CloudPoolDriverException
	 *             If the available flavors could not be listed.
	 */
	private String getFlavorId() throws IllegalArgumentException, CloudPoolDriverException {
		List<Flavor> flavors;
		try {
			flavors = new ListSizesRequest(getClientFactory()).call();
		} catch (Exception e) {
			throw new CloudPoolDriverException(
					String.format("failed to fetch the list of available flavors: %s", e.getMessage()), e);
		}
		for (Flavor flavor : flavors) {
			if (flavor.getName().equals(this.flavorName)) {
				return flavor.getId();
			}
		}
		throw new IllegalArgumentException(
				String.format("failed to create server: no flavor with " + "name \"%s\" exists in region \"%s\"",
						this.flavorName, getApiAccessConfig().getRegion()));
	}

	/**
	 * Returns the image identifier that corresponds to the specified image
	 * name.
	 *
	 * @return
	 * @throws CloudPoolException
	 *             If the available images could not be listed.
	 * @throws IllegalArgumentException
	 *             If the specified image name doesn't exist.
	 */
	private String getImageId() throws IllegalArgumentException, CloudPoolDriverException {

		List<Image> images;
		try {
			images = new ListImagesRequest(getClientFactory()).call();
		} catch (Exception e) {
			throw new CloudPoolDriverException(
					String.format("failed to fetch the list of available images: %s", e.getMessage()), e);
		}
		for (Image image : images) {
			if (image.getName().equals(this.imageName)) {
				return image.getId();
			}
		}
		throw new IllegalArgumentException(
				String.format("failed to create server: no image with " + "name \"%s\" exists in region \"%s\"",
						this.imageName, getApiAccessConfig().getRegion()));
	}

	/**
	 * Returns the network identifiers that correspond to the specified set of
	 * network names.
	 *
	 * @return
	 * @throws IllegalArgumentException
	 * @throws CloudPoolDriverException
	 */
	private List<String> getNetworkIds() throws IllegalArgumentException, CloudPoolDriverException {
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
				throw new IllegalArgumentException(String.format(
						"failed to create server: no network with " + "name \"%s\" exists in region \"%s\"",
						networkName, getApiAccessConfig().getRegion()));
			}
			networkIds.add(matchingNetwork.get().getId());
		}

		return networkIds;
	}

	/**
	 * Logs debug information about the server that is to be launched.
	 */
	private void logDebugInfo() {
		LOG.debug("Starting server: {}", this.serverName);
		LOG.debug("Flavor: {}", this.flavorName);
		LOG.debug("Image: {}", this.imageName);
		LOG.debug("Key pair: {}", this.keyPair);
		LOG.debug("Metadata: {}", this.metadata);
		LOG.debug("Security groups: {}", this.securityGroups);
		LOG.debug("Encoded user data: {}", this.encodedUserData);
		LOG.debug("networks: {}", this.networks);
		if (this.encodedUserData != null) {
			LOG.debug("User data: {}", Base64Utils.fromBase64(this.encodedUserData, Charsets.UTF_8));
		}
	}
}
