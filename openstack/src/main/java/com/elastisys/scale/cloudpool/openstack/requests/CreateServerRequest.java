package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriverConfig;
import com.google.common.base.Optional;

/**
 * An Openstack Nova (compute) request that, when executed, creates a new server
 * instance.
 */
public class CreateServerRequest extends AbstractNovaRequest<Server> {

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
	 * @param account
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
	public CreateServerRequest(OpenStackPoolDriverConfig account,
			String serverName, String flavorName, String imageName,
			String keyPair, List<String> securityGroups,
			Optional<String> userData, Map<String, String> metadata) {
		super(account);

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
	public Server doRequest(NovaApi api) {
		CreateServerOptions serverOptions = new CreateServerOptions();
		serverOptions.keyPairName(this.keyPair);
		serverOptions.securityGroupNames(this.securityGroups);
		serverOptions.metadata(this.metadata);
		if (this.userData.isPresent()) {
			serverOptions.userData(this.userData.get().getBytes());
		}

		// create server
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		ServerCreated serverCreated = serverApi.create(this.serverName,
				getImageId(), getFlavorId(), serverOptions);
		return serverApi.get(serverCreated.getId());
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
			flavors = new ListSizesRequest(getAccount()).call();
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
				this.flavorName, getAccount().getRegion()));
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
			images = new ListImagesRequest(getAccount()).call();
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
				this.imageName, getAccount().getRegion()));
	}

}
