package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;

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
	/** The name of the key pair to use for the machine instance. */
	private final String keyPair;
	/** The security group(s) to use for the new machine instance. */
	private final List<String> securityGroups;

	/**
	 * The (optional) user data (boot up script) used to contextualize the
	 * started instance.
	 */
	private final Optional<String> userData;
	/** Any meta data tags to assign to the new server. */
	private final Map<String, String> metadata;

	/**
	 * Creates a new {@link CreateServerRequest}.
	 *
	 * @param accessConfig
	 *            Connection details for a particular OpenStack account.
	 * @param serverName
	 *            The name to assign to the new server.
	 * @param flavorName
	 *            The name of the machine type (flavor) to launch.
	 * @param imageName
	 *            The identifier of the machine image used to boot the machine.
	 * @param keyPair
	 *            The name of the key pair to use for the machine instance.
	 * @param securityGroups
	 *            The security group(s) to use for the new machine instance.
	 * @param userData
	 *            The (optional) user data (boot up script) used to
	 *            contextualize the started instance.
	 * @param metadata
	 *            Any meta data tags to assign to the new server.
	 */
	public CreateServerRequest(OpenStackPoolDriverConfig accessConfig,
			String serverName, String flavorName, String imageName,
			String keyPair, List<String> securityGroups,
			Optional<String> userData, Map<String, String> metadata) {
		super(accessConfig);

		checkNotNull(serverName, "server name cannot be null");
		checkNotNull(flavorName, "flavor name cannot be null");
		checkNotNull(imageName, "image name cannot be null");
		checkNotNull(keyPair, "keyPair cannot be null");
		checkNotNull(securityGroups, "securityGroups cannot be null");
		checkNotNull(metadata, "metadata map cannot be null");

		this.serverName = serverName;
		this.flavorName = flavorName;
		this.imageName = imageName;
		this.keyPair = keyPair;
		this.securityGroups = securityGroups;

		this.userData = userData;
		this.metadata = metadata;
	}

	@Override
	public Server doRequest(OSClient api) {
		ServerService serverService = api.compute().servers();

		ServerCreateBuilder serverCreateBuilder = serverService.serverBuilder()
				.name(this.serverName).flavor(getFlavorId())
				.image(getImageId()).keypairName(this.keyPair)
				.addMetadata(this.metadata);

		for (String securityGroup : this.securityGroups) {
			serverCreateBuilder.addSecurityGroup(securityGroup);
		}

		if (this.userData.isPresent()) {
			String base64UserData = BaseEncoding.base64().encode(
					this.userData.get().getBytes());
			serverCreateBuilder.userData(base64UserData);
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
	private String getFlavorId() throws IllegalArgumentException,
			CloudPoolDriverException {
		List<Flavor> flavors;
		try {
			flavors = new ListSizesRequest(getAccessConfig()).call();
		} catch (Exception e) {
			throw new CloudPoolDriverException(String.format(
					"failed to fetch the list of available flavors: %s",
					e.getMessage()), e);
		}
		for (Flavor flavor : flavors) {
			if (flavor.getName().equals(this.flavorName)) {
				return flavor.getId();
			}
		}
		throw new IllegalArgumentException(String.format(
				"failed to create server: no flavor with "
						+ "name \"%s\" exists in region \"%s\"",
				this.flavorName, getAccessConfig().getRegion()));
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
	private String getImageId() throws IllegalArgumentException,
			CloudPoolDriverException {

		List<Image> images;
		try {
			images = new ListImagesRequest(getAccessConfig()).call();
		} catch (Exception e) {
			throw new CloudPoolDriverException(String.format(
					"failed to fetch the list of available images: %s",
					e.getMessage()), e);
		}
		for (Image image : images) {
			if (image.getName().equals(this.imageName)) {
				return image.getId();
			}
		}
		throw new IllegalArgumentException(String.format(
				"failed to create server: no image with "
						+ "name \"%s\" exists in region \"%s\"",
				this.imageName, getAccessConfig().getRegion()));
	}

}
