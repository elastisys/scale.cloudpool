package com.elastisys.scale.cloudpool.splitter.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

/**
 * Splitter configuration for a single back-end cloud pool.
 *
 * @see SplitterConfig
 */
public class PrioritizedCloudPool implements Comparable<PrioritizedCloudPool> {

	/** The priority of this cloud pool. */
	private final int priority;
	/** The host name/IP address of the remote cloud pool REST endpoint. */
	private final String cloudPoolHost;
	/** The HTTPS port of the remote cloud pool REST endpoint. */
	private final int cloudPoolPort;
	/** Username/password credentials for basic authentication. */
	private final BasicCredentials basicCredentials;
	/** Certificate credentials for certificate-based client authentication. */
	private final CertificateCredentials certificateCredentials;

	/**
	 * Creates a new instance with the values set to the given values.
	 *
	 * @param priority
	 *            The priority of this cloud pool.
	 * @param cloudPoolHost
	 *            The hostname of the REST endpoint.
	 * @param cloudPoolPort
	 *            The port number of the REST endpoint.
	 * @param basicCredentials
	 *            Credentials, if any, required for HTTP Basic Authentication.
	 *            If none are needed, provide null as the value.
	 * @param certificateCredentials
	 *            Credentials, if any, for SSL/TLS certificate-based
	 *            authentication. If none are needed, provide null as the value.
	 */
	public PrioritizedCloudPool(int priority, String cloudPoolHost,
			int cloudPoolPort, BasicCredentials basicCredentials,
			CertificateCredentials certificateCredentials) {
		this.priority = priority;
		this.cloudPoolHost = cloudPoolHost;
		this.cloudPoolPort = cloudPoolPort;
		this.basicCredentials = basicCredentials;
		this.certificateCredentials = certificateCredentials;
	}

	/**
	 * Returns the priority of this cloud pool.
	 *
	 * @return
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Returns the hostname of the REST endpoint.
	 *
	 * @return
	 */
	public String getCloudPoolHost() {
		return this.cloudPoolHost;
	}

	/**
	 * Returns the port number of the REST endpoint.
	 *
	 * @return
	 */
	public int getCloudPoolPort() {
		return this.cloudPoolPort;
	}

	/**
	 * Returns credentials, if any, required for HTTP Basic Authentication.
	 *
	 * @return
	 */
	public Optional<BasicCredentials> getBasicCredentials() {
		return Optional.fromNullable(this.basicCredentials);
	}

	/**
	 * Returns credentials, if any, for SSL/TLS certificate-based
	 * authentication.
	 *
	 * @return
	 */
	public Optional<CertificateCredentials> getCertificateCredentials() {
		return Optional.fromNullable(this.certificateCredentials);
	}

	/**
	 * Makes a basic sanity check verifying that all values are non-
	 * <code>null</code>. If a value is missing for any field a
	 * {@link ConfigurationException} is thrown.
	 *
	 * @throws IllegalArgumentException
	 *             If any configuration field is missing.
	 */
	public void validate() throws IllegalArgumentException {
		try {
			checkArgument(Range.closed(0, 100).contains(this.priority),
					"cloudPool: priority must be a value between 0 and 100");
			checkArgument(this.cloudPoolHost != null,
					"cloudPool: missing cloudPoolHost");
			checkArgument(this.cloudPoolPort > 0,
					"cloudPool: cloudPoolPort must be > 0");

			// validate client credentials
			if (getBasicCredentials().isPresent()) {
				getBasicCredentials().get().validate();
			}
			if (getCertificateCredentials().isPresent()) {
				getCertificateCredentials().get().validate();
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format(
					"invalid splitter configuration: %s", e.getMessage()), e);
		}
	}

	/**
	 * Two {@link PrioritizedCloudPool}s are considered equal if they share the
	 * same host and port.
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.cloudPoolHost, this.cloudPoolPort);
	}

	/**
	 * Two {@link PrioritizedCloudPool}s are considered equal if they share the
	 * same host and port.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PrioritizedCloudPool) {
			PrioritizedCloudPool that = (PrioritizedCloudPool) obj;
			return Objects.equals(this.cloudPoolHost, that.cloudPoolHost)
					&& Objects.equals(this.cloudPoolPort, that.cloudPoolPort);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("cloudPoolHost", this.cloudPoolHost)
				.add("cloudPoolPort", this.cloudPoolPort)
				.add("priority", this.priority).toString();
	}

	/**
	 * Compares the priorities of the prioritized cloud pools so that higher
	 * priorities are sorted first, i.e., descending order is the most natural
	 * for these pools.
	 */
	@Override
	public int compareTo(PrioritizedCloudPool o) {
		return -1 * Integer.compare(getPriority(), o.getPriority());
	}
}