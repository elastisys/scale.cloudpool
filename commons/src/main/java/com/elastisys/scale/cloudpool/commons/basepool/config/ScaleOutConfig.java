package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to provision
 * additional servers (on scale-out).
 *
 * @see BaseCloudPoolConfig
 */
public class ScaleOutConfig {
	/** The name of the server type to launch. For example, m1.medium. */
	private final String size;
	/** The name of the machine image used to boot new servers. */
	private final String image;
	/** The name of the key pair to use for new machine instances. */
	private final String keyPair;
	/** The security group(s) to use for new machine instances. */
	private final List<String> securityGroups;
	/** The script to run after first boot of a new instance. */
	private final List<String> bootScript;

	public ScaleOutConfig(String size, String image, String keyPair,
			List<String> securityGroups, List<String> bootScript) {
		this.size = size;
		this.image = image;
		this.keyPair = keyPair;
		this.securityGroups = securityGroups;
		this.bootScript = bootScript;
	}

	public String getSize() {
		return this.size;
	}

	public String getImage() {
		return this.image;
	}

	public String getKeyPair() {
		return this.keyPair;
	}

	public List<String> getSecurityGroups() {
		return this.securityGroups;
	}

	public List<String> getBootScript() {
		return this.bootScript;
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		try {
			checkNotNull(this.size, "missing size");
			checkNotNull(this.image, "missing image");
			checkNotNull(this.keyPair, "missing keyPair");
			checkNotNull(this.securityGroups, "missing securityGroups");
			checkNotNull(this.bootScript, "missing bootScript");
		} catch (Exception e) {
			throw new CloudPoolException(format(
					"failed to validate scaleUpConfig: %s", e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.size, this.image, this.keyPair,
				this.securityGroups, this.bootScript);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScaleOutConfig) {
			ScaleOutConfig that = (ScaleOutConfig) obj;
			return equal(this.size, that.size) && equal(this.image, that.image)
					&& equal(this.keyPair, that.keyPair)
					&& equal(this.securityGroups, that.securityGroups)
					&& equal(this.bootScript, that.bootScript);
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
	}
}