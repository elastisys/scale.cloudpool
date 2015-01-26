package com.elastisys.scale.cloudadapers.api.server;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.io.Resources;

/**
 * Captures (command-line) options accepted by a {@link CloudAdapterServer}.
 *
 *
 *
 */
public class CloudAdapterOptions {

	/**
	 * The default SSL key store embedded in the server's JAR file. This key
	 * store is only used when no key store is specified on construction.
	 */
	public final static String DEFAULT_KEYSTORE_PATH = Resources.getResource(
			"etc/security/server_keystore.p12").toString();
	/** Password to the default SSL key store. */
	public final static String DEFAULT_KEYSTORE_PASSWORD = "pkcs12password";

	public final static String DEFAULT_TRUSTSTORE_PATH = Resources.getResource(
			"etc/security/server_truststore.jks").toString();
	public final static String DEFAULT_TRUSTSTORE_PASSWORD = "truststorepassword";

	@Option(name = "--https-port", metaVar = "PORT", usage = "The HTTPS port to listen on. Default: 8443.")
	public int httpsPort = 8443; // default

	@Option(name = "--cert-required", usage = "Set if SSL clients must "
			+ "authenticate with a certificate. "
			+ "Note that this requires trusted clients to "
			+ "have their certificates added to the server's trust store. "
			+ "Default: false.")
	public boolean requireClientCert = false;

	@Option(name = "--ssl-keystore", metaVar = "PATH", usage = "The location of the server's SSL key store (PKCS12 format). "
			+ "Default: an embedded key store with a self-signed certificate.")
	public String sslKeyStore = DEFAULT_KEYSTORE_PATH;
	@Option(name = "--ssl-keystore-password", metaVar = "PASSWORD", usage = "The password that protects the SSL key store. "
			+ "Default: '" + DEFAULT_KEYSTORE_PASSWORD + "'")
	public String sslKeyStorePassword = DEFAULT_KEYSTORE_PASSWORD;

	@Option(name = "--ssl-truststore", metaVar = "PATH", usage = "The location of "
			+ "a SSL trust store (JKS format), containing trusted client "
			+ "certificates. Default: an embedded key store that trusts a "
			+ "dummy client.")
	public String sslTrustStore = DEFAULT_TRUSTSTORE_PATH;
	@Option(name = "--ssl-truststore-password", metaVar = "PASSWORD", usage = "The password that protects the SSL trust store. "
			+ "Default: '" + DEFAULT_TRUSTSTORE_PASSWORD + "'")
	public String sslTrustStorePassword = DEFAULT_TRUSTSTORE_PASSWORD;

	@Option(name = "--config", metaVar = "FILE", usage = "Initial cloud adapter configuration file (JSON-formatted).")
	public String config = null;

	@Option(name = "--config-handler", usage = "Publish a /config handler that allows configuration to be queried and updated. In addition, /config/schema will listen for GET requests to serve the schema for valid configurations. Default: False.")
	public boolean enableConfigHandler = false;

	@Option(name = "--exit-handler", usage = "Publish an /exit handler that shuts down the server on 'GET /exit'. Default: False.")
	public boolean enableExitHandler = false;

	@Option(name = "--help", usage = "Displays this help text.")
	public boolean help = false; // default

	@Option(name = "--version", usage = "Displays the version of the cloud adapter.")
	public boolean version = false; // default

	// receives command line arguments other than options
	@Argument
	public List<String> arguments = new ArrayList<String>();
}
