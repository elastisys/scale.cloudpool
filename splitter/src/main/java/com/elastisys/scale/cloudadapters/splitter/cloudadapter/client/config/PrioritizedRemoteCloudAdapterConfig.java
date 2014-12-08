package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Objects;

import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.google.common.base.Optional;

/**
 * Abstract configuration of a cloud adapter accessible via its <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/index.html">REST
 * interface</a>.
 *
 * <p>
 * This class is thread-safe by virtue of being immutable.
 * </p>
 */
public class PrioritizedRemoteCloudAdapterConfig {

	/** The host name/IP address of the remote cloud adapter REST endpoint. */
	protected final String cloudAdapterHost;
	/** The HTTPS port of the remote cloud adapter REST endpoint. */
	protected final int cloudAdapterPort;
	/** The priority of this cloud adapter. */
	protected final int priority;
	/** Username/password credentials for basic authentication. */
	protected final BasicCredentials basicCredentials;
	/** Certificate credentials for certificate-based client authentication. */
	protected final CertificateCredentials certificateCredentials;

	/**
	 * Creates a new instance with the values set to the given values.
	 *
	 * @param cloudAdapterHost
	 *            The hostname of the REST endpoint.
	 * @param cloudAdapterPort
	 *            The port number of the REST endpoint.
	 * @param priority
	 *            The priority of this cloud adapter.
	 * @param basicCredentials
	 *            Credentials, if any, required for HTTP Basic Authentication.
	 *            If none are needed, provide null as the value.
	 * @param certificateCredentials
	 *            Credentials, if any, for SSL/TLS certificate-based
	 *            authentication. If none are needed, provide null as the value.
	 */
	public PrioritizedRemoteCloudAdapterConfig(String cloudAdapterHost,
			int cloudAdapterPort, int priority,
			BasicCredentials basicCredentials,
			CertificateCredentials certificateCredentials) {
		this.cloudAdapterHost = cloudAdapterHost;
		this.cloudAdapterPort = cloudAdapterPort;
		this.priority = priority;
		this.basicCredentials = basicCredentials;
		this.certificateCredentials = certificateCredentials;
	}

	public String getCloudAdapterHost() {
		return this.cloudAdapterHost;
	}

	public int getCloudAdapterPort() {
		return this.cloudAdapterPort;
	}

	/**
	 * @return The priority of this cloud adapter.
	 */
	public int getPriority() {
		return this.priority;
	}

	public Optional<BasicCredentials> getBasicCredentials() {
		return Optional.fromNullable(this.basicCredentials);
	}

	public Optional<CertificateCredentials> getCertificateCredentials() {
		return Optional.fromNullable(this.certificateCredentials);
	}

	/**
	 * Makes a basic sanity check verifying that all values are non-
	 * <code>null</code>. If a value is missing for any field a
	 * {@link ConfigurationException} is thrown.
	 *
	 * @throws ConfigurationException
	 *             If any configuration field is missing.
	 */
	public void validate() throws ConfigurationException {
		try {
			// validate connection details
			checkNotNull(this.cloudAdapterHost, "missing cloudAdapterHost");
			checkArgument(this.cloudAdapterPort > 0,
					"cloudAdapterPort must be > 0");

			// validate client credentials
			checkArgument(getBasicCredentials().isPresent()
					|| getCertificateCredentials().isPresent(),
					"neither basic nor certificate credentials given");
			if (getBasicCredentials().isPresent()) {
				validateBasicCredentials(getBasicCredentials().get());
			}
			if (getCertificateCredentials().isPresent()) {
				validateCertificateCredentials(getCertificateCredentials()
						.get());
			}
		} catch (Exception e) {
			throw new ConfigurationException("Invalid configuration: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Performs a basic sanity check to verify that the combination of
	 * parameters is valid. If validation fails a {@link ConfigurationException}
	 * is thrown.
	 *
	 * @throws ConfigurationException
	 *             If any configuration field is missing.
	 */
	private static void validateBasicCredentials(BasicCredentials credentials)
			throws ConfigurationException {
		checkNotNull(credentials.getUsername(),
				"basic credentials missing username");
		checkNotNull(credentials.getPassword(),
				"basic credentials missing password");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.cloudAdapterHost, this.cloudAdapterPort,
				this.priority, this.basicCredentials,
				this.certificateCredentials);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		PrioritizedRemoteCloudAdapterConfig that = (PrioritizedRemoteCloudAdapterConfig) obj;
		return Objects.equals(this.cloudAdapterHost, that.cloudAdapterHost)
				&& Objects.equals(this.cloudAdapterPort, that.cloudAdapterPort)
				&& Objects.equals(this.priority, that.priority)
				&& Objects.equals(this.basicCredentials, that.basicCredentials)
				&& Objects.equals(this.certificateCredentials,
						that.certificateCredentials);
	}

	/**
	 * Performs a basic sanity check to verify that the combination of
	 * parameters is valid. If validation fails a {@link ConfigurationException}
	 * is thrown.
	 *
	 * @throws ConfigurationException
	 *             If any configuration field is missing.
	 */
	private static void validateCertificateCredentials(
			CertificateCredentials credentials) throws ConfigurationException {
		checkNotNull(credentials.getKeystorePath(),
				"certificate credentials missing keystore path");
		checkArgument(new File(credentials.getKeystorePath()).isFile(),
				"certificate credentials keystore path '%s' is not a file",
				credentials.getKeystorePath());
		checkNotNull(credentials.getKeystorePassword(),
				"certificate credentials missing keystore password");
	}

}