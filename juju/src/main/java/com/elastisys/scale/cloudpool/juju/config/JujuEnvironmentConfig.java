package com.elastisys.scale.cloudpool.juju.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Base64;

/**
 * Holds all configuration needed for the command line application.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 *
 */
public class JujuEnvironmentConfig {

	/**
	 * The name of the environment.
	 */
	private final String name;
	/**
	 * Base64-encoded environment configuration from environments.yaml
	 */
	private final String encodedConfig;

	/**
	 * Base64-encoded environment.jenv file for the given environment.
	 */
	private final String encodedJenv;

	/**
	 * Base64-encoded public key file for the environment.
	 */
	private final String encodedPubkey;

	/**
	 * Base64-encoded public key file for the environment.
	 */
	private final String encodedPrivkey;

	/**
	 * Creates a new instance.
	 *
	 * @param name
	 *            The environment's name.
	 * @param encodedConfig
	 *            Base64-encoded environment configuration from
	 *            environments.yaml
	 * @param encodedJenv
	 *            Base64-encoded environment.jenv file for the given
	 *            environment.
	 * @param encodedPubkey
	 *            Base64-encoded public key file for the environment.
	 * @param encodedPrivkey
	 *            Base64-encoded public key file for the environment.
	 */
	public JujuEnvironmentConfig(String name, String encodedConfig, String encodedJenv, String encodedPubkey,
			String encodedPrivkey) {
		super();
		this.name = name;
		this.encodedConfig = encodedConfig;
		this.encodedJenv = encodedJenv;
		this.encodedPubkey = encodedPubkey;
		this.encodedPrivkey = encodedPrivkey;
	}

	public String getName() {
		return this.name;
	}

	public String getEncodedConfig() {
		return this.encodedConfig;
	}

	public String getEncodedJenv() {
		return this.encodedJenv;
	}

	public String getEncodedPubkey() {
		return this.encodedPubkey;
	}

	public String getEncodedPrivkey() {
		return this.encodedPrivkey;
	}

	/**
	 * @return The stored configuration, decoded.
	 */
	public String getConfig() {
		return new String(Base64.getDecoder().decode(this.encodedConfig));
	}

	/**
	 * @return The stored .jenv contents, decoded.
	 */
	public String getJenv() {
		return new String(Base64.getDecoder().decode(this.encodedJenv));
	}

	/**
	 * @return The stored public key, decoded.
	 */
	public String getPubkey() {
		return new String(Base64.getDecoder().decode(this.encodedPubkey));
	}

	/**
	 * @return The stored private key, decoded.
	 */
	public String getPrivkey() {
		return new String(Base64.getDecoder().decode(this.encodedPrivkey));
	}

	@Override
	public String toString() {
		return "EnvironmentConfig [name=" + this.name + ", encodedConfig=" + this.encodedConfig + ", encodedJenv="
				+ this.encodedJenv + ", encodedPubkey=" + this.encodedPubkey + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.encodedConfig == null ? 0 : this.encodedConfig.hashCode());
		result = prime * result + (this.encodedJenv == null ? 0 : this.encodedJenv.hashCode());
		result = prime * result + (this.encodedPrivkey == null ? 0 : this.encodedPrivkey.hashCode());
		result = prime * result + (this.encodedPubkey == null ? 0 : this.encodedPubkey.hashCode());
		result = prime * result + (this.name == null ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JujuEnvironmentConfig other = (JujuEnvironmentConfig) obj;
		if (this.encodedConfig == null) {
			if (other.encodedConfig != null) {
				return false;
			}
		} else if (!this.encodedConfig.equals(other.encodedConfig)) {
			return false;
		}
		if (this.encodedJenv == null) {
			if (other.encodedJenv != null) {
				return false;
			}
		} else if (!this.encodedJenv.equals(other.encodedJenv)) {
			return false;
		}
		if (this.encodedPrivkey == null) {
			if (other.encodedPrivkey != null) {
				return false;
			}
		} else if (!this.encodedPrivkey.equals(other.encodedPrivkey)) {
			return false;
		}
		if (this.encodedPubkey == null) {
			if (other.encodedPubkey != null) {
				return false;
			}
		} else if (!this.encodedPubkey.equals(other.encodedPubkey)) {
			return false;
		}
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public void validate() {
		checkArgument(this.name != null, "config: no environment name given");
		checkArgument(this.encodedConfig != null, "config: no environment config (encoded) given");
		checkArgument(this.encodedJenv != null, "config: no environment jenv (encoded) given");
		checkArgument(this.encodedPrivkey != null, "config: no environment private key (encoded) given");
		checkArgument(this.encodedPubkey != null, "config: no environment public key (encoded) given");

		// TODO Verify that private/public keys match
		// TODO Ensure that the name exists in the configuration and jenv
	}

}
