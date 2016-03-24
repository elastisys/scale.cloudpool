package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Authentication configuration for a {@link KubernetesCloudPoolConfig}.
 * Describes how the client will authenticate itself to the Kubernetes apiserver
 * (and optionally authenticate the server).
 * <p/>
 * There are a couple of ways to authenticate the client -- (1) via a JSON Web
 * Token or (2) via TLS certificate. Either approach can be used, but as
 * explained in the <a href=
 * "http://kubernetes.io/docs/user-guide/accessing-the-cluster/#accessing-the-api-from-a-pod">
 * Kubernetes documentation</a>, the preferred approach when running the
 * {@link KubernetesCloudPool} inside a Kubernetes pod is to make use of
 * approach (1), since a service account token will always be available on the
 * pod's file system.
 * <p/>
 * Tokens and certificates/keys can be passed either as a file system path
 * (referring to the file system on which the {@link KubernetesCloudPool} is
 * running) or be passed as (base 64-encoded) values. Configuration variables
 * ending in {@code Path} are used to pass file system references.
 *
 * @see KubernetesCloudPoolConfig
 */
public class AuthConfig {
	/**
	 * The JWT auth token at the specified file system path will be used to
	 * authenticate the client.
	 */
	private final String clientTokenPath;

	/**
	 * The (base64-encoded) JWT auth token that will be used to authenticate the
	 * client.
	 */
	private final String clientToken;

	/**
	 * The PEM-encoded certificate at the specified file system path will be
	 * used to authenticate the client. Requires a {@link #clientKey} or
	 * {@link #clientKeyPath}.
	 */
	private final String clientCertPath;
	/**
	 * The (base64-encoded) PEM-encoded certificate that will be used to
	 * authenticate the client. Requires a {@link #clientKey} or
	 * {@link #clientKeyPath}.
	 */
	private final String clientCert;

	/**
	 * The PEM-encoded private key at the specified file system path will be
	 * used to authenticate the client. Requires a {@link #clientCert} or
	 * {@link #clientCertPath}.
	 */
	private final String clientKeyPath;

	/**
	 * The (base64-encoded) PEM-encoded private key that will be used to
	 * authenticate the client. Requires a {@link #clientCert} or
	 * {@link #clientCertPath}.
	 */
	private final String clientKey;

	/**
	 * The PEM-encoded server/CA certificate at the specified file system path
	 * will be used to authenticate the server. Optional. If left out, no
	 * validation of the server will be performed, similar to using {@code curl}
	 * with the {@code --insecure} flag.
	 */
	private final String serverCertPath;

	/**
	 * The (base64-encoded) PEM-encoded server/CA certificate that will be used
	 * to authenticate the server. Optional. If left out, no validation of the
	 * server will be performed, similar to using {@code curl} with the
	 * {@code --insecure} flag.
	 */
	private final String serverCert;

	/**
	 * Creates an {@link AuthConfig}.
	 *
	 * @param clientTokenPath
	 *            The JWT auth token at the specified file system path will be
	 *            used to authenticate the client.
	 * @param clientToken
	 *            The (base64-encoded) JWT auth token that will be used to
	 *            authenticate the client.
	 * @param clientCertPath
	 *            The PEM-encoded certificate at the specified file system path
	 *            will be used to authenticate the client. Requires a
	 *            {@link #clientKey} or {@link #clientKeyPath}.
	 * @param clientCert
	 *            The (base64-encoded) PEM-encoded certificate that will be used
	 *            to authenticate the client. Requires a {@link #clientKey} or
	 *            {@link #clientKeyPath}.
	 * @param clientKeyPath
	 *            The PEM-encoded private key at the specified file system path
	 *            will be used to authenticate the client. Requires a
	 *            {@link #clientCert} or {@link #clientCertPath}.
	 * @param clientKey
	 *            The (base64-encoded) PEM-encoded private key that will be used
	 *            to authenticate the client. Requires a {@link #clientCert} or
	 *            {@link #clientCertPath}.
	 * @param serverCertPath
	 *            The PEM-encoded server/CA certificate at the specified file
	 *            system path will be used to authenticate the server. If left
	 *            out, no validation of the server will be performed, similar to
	 *            using {@code curl} with the {@code --insecure} flag.
	 * @param serverCert
	 *            The (base64-encoded) PEM-encoded server/CA certificate that
	 *            will be used to authenticate the server. If left out, no
	 *            validation of the server will be performed, similar to using
	 *            {@code curl} with the {@code --insecure} flag.
	 */
	public AuthConfig(String clientTokenPath, String clientToken,
			String clientCertPath, String clientCert, String clientKeyPath,
			String clientKey, String serverCertPath, String serverCert,
			Boolean verifyServerCert) {
		this.clientTokenPath = clientTokenPath;
		this.clientToken = clientToken;
		this.clientCertPath = clientCertPath;
		this.clientCert = clientCert;
		this.clientKeyPath = clientKeyPath;
		this.clientKey = clientKey;
		this.serverCertPath = serverCertPath;
		this.serverCert = serverCert;
		validate();
	}

	/**
	 * Returns the base64-encoded client auth token to be used, or
	 * <code>null</code> if none was given.
	 *
	 * @return
	 * @throws IOException
	 */
	public String getClientToken() throws IOException {
		if (!hasClientToken()) {
			return null;
		}

		// client token is *either* given as value or as path
		String token = this.clientToken;
		if (this.clientTokenPath != null) {
			token = Files.toString(new File(this.clientTokenPath),
					Charsets.UTF_8);
		}
		return token;
	}

	/**
	 * Returns the client certificate to use for authentication, or
	 * <code>null</code> if none was given.
	 *
	 * @return
	 * @throws IOException
	 * @throws CertificateException
	 */
	public Certificate getClientCert()
			throws IOException, CertificateException {
		if (!hasClientCert()) {
			return null;
		}

		// client cert is *either* given as value or as path
		String pemEncodedCert = null;
		if (this.clientCertPath != null) {
			pemEncodedCert = Files.toString(new File(this.clientCertPath),
					Charsets.UTF_8);
		} else {
			// read base64-encoded cert
			pemEncodedCert = Base64Utils.fromBase64(this.clientCert,
					Charsets.UTF_8);
		}

		return PemUtils.parseX509Cert(new StringReader(pemEncodedCert));
	}

	/**
	 * Returns the client key to use for authentication, or <code>null</code> if
	 * none was given.
	 *
	 * @return
	 * @throws IOException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 */
	public PrivateKey getClientKey()
			throws IOException, NoSuchAlgorithmException,
			NoSuchProviderException, InvalidKeySpecException {
		if (!hasClientKey()) {
			return null;
		}

		// client key is *either* given as value or as path
		String pemEncodedKey = null;
		if (this.clientKeyPath != null) {
			pemEncodedKey = Files.toString(new File(this.clientKeyPath),
					Charsets.UTF_8);
		} else {
			// read base64-encoded key
			pemEncodedKey = Base64Utils.fromBase64(this.clientKey,
					Charsets.UTF_8);
		}

		return PemUtils.parseRsaPrivateKey(new StringReader(pemEncodedKey));
	}

	/**
	 * Returns the server/CA certificate to use for authenticating the server,
	 * or <code>null</code> if none was given.
	 *
	 * @return
	 * @throws IOException
	 * @throws CertificateException
	 */
	public Certificate getServerCert()
			throws IOException, CertificateException {
		if (!hasServerCert()) {
			return null;
		}

		// server cert is *either* given as value or as path
		String pemEncodedCert = null;
		if (this.serverCertPath != null) {
			pemEncodedCert = Files.toString(new File(this.serverCertPath),
					Charsets.UTF_8);
		} else {
			// read base64-encoded cert
			pemEncodedCert = Base64Utils.fromBase64(this.serverCert,
					Charsets.UTF_8);
		}

		return PemUtils.parseX509Cert(new StringReader(pemEncodedCert));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.clientToken, this.clientTokenPath,
				this.clientCert, this.clientCertPath, this.clientKey,
				this.clientKeyPath, this.serverCert, this.serverCertPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AuthConfig) {
			AuthConfig that = (AuthConfig) obj;
			return Objects.equals(this.clientToken, that.clientToken)
					&& Objects.equals(this.clientTokenPath,
							that.clientTokenPath)
					&& Objects.equals(this.clientCert, that.clientCert)
					&& Objects.equals(this.clientCertPath, that.clientCertPath)
					&& Objects.equals(this.clientKey, that.clientKey)
					&& Objects.equals(this.clientKeyPath, that.clientKeyPath)
					&& Objects.equals(this.serverCert, that.serverCert)
					&& Objects.equals(this.serverCertPath, that.serverCertPath);

		}
		return super.equals(obj);
	}

	public void validate() throws IllegalArgumentException {
		validateClientToken();
		validateClientCert();
		validateClientKey();
		// client must perform some kind of authentication
		checkArgument(hasClientToken() || (hasClientCert() && hasClientKey()),
				"auth: neither token- nor cert-based authentication specified");

		validateServerCert();
	}

	private void validateServerCert() throws IllegalArgumentException {
		if (hasServerCert()) {
			checkArgument(this.serverCertPath != null ^ this.serverCert != null,
					"auth: specify either serverCert or serverCertPath, not both");
			// makes sure that the server cert can be loaded/parsed.
			try {
				getServerCert();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"failed to parse server cert: %s", e.getMessage()), e);
			}
		}
	}

	private void validateClientKey() throws IllegalArgumentException {
		if (hasClientKey()) {
			checkArgument(this.clientKeyPath != null ^ this.clientKey != null,
					"auth: specify either clientKey or clientKeyPath, not both");
			checkArgument(hasClientCert(),
					"auth: client auth key specified without a cert");
			// makes sure that the client key can be loaded/parsed.
			try {
				getClientKey();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"failed to parse client key: %s", e.getMessage()), e);
			}
		}
	}

	private void validateClientCert() throws IllegalArgumentException {
		if (hasClientCert()) {
			checkArgument(this.clientCertPath != null ^ this.clientCert != null,
					"auth: specify either clientCertPath or clientCert, not both");
			checkArgument(hasClientKey(),
					"auth: client certificate auth specified without a key");
			// makes sure that the client cert can be loaded/parsed.
			try {
				getClientCert();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"failed to parse client key: %s", e.getMessage()), e);
			}
		}
	}

	private void validateClientToken() throws IllegalArgumentException {
		if (hasClientToken()) {
			checkArgument(
					this.clientTokenPath != null ^ this.clientToken != null,
					"auth: specify either clientToken or clientTokenPath, not both");
			// makes sure that the client token can be loaded/parsed.
			try {
				getClientToken();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"failed to parse client token: %s", e.getMessage()), e);
			}
		}
	}

	/**
	 * <code>true</code> if a client token was given (either by path or value).
	 *
	 * @return
	 */
	public boolean hasClientToken() {
		return this.clientToken != null || this.clientTokenPath != null;
	}

	/**
	 * <code>true</code> if a client cert was given (either by path or value).
	 *
	 * @return
	 */
	public boolean hasClientCert() {
		return this.clientCert != null || this.clientCertPath != null;
	}

	/**
	 * <code>true</code> if a client key was given (either by path or value).
	 *
	 * @return
	 */
	public boolean hasClientKey() {
		return this.clientKey != null || this.clientKeyPath != null;
	}

	/**
	 * <code>true</code> if a server cert was given (either by path or value).
	 *
	 * @return
	 */
	public boolean hasServerCert() {
		return this.serverCert != null || this.serverCertPath != null;
	}
}
