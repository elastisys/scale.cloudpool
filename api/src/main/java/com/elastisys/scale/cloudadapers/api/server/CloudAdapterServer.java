package com.elastisys.scale.cloudadapers.api.server;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.restapi.ConfigHandler;
import com.elastisys.scale.cloudadapers.api.restapi.ConfigSchemaHandler;
import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.rest.responsehandlers.ExitHandler;
import com.elastisys.scale.commons.rest.server.JaxRsApplication;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.google.gson.JsonObject;

/**
 * Factory methods for creating a HTTPS server that publishes a REST API for a
 * {@link CloudAdapter} instance.
 * <p/>
 * The REST API is fully covered in the <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud adapter REST API documentation</a>.
 * 
 * 
 * 
 */
public class CloudAdapterServer {
	static Logger log = LoggerFactory.getLogger(CloudAdapterServer.class);

	/**
	 * Parse command-line arguments and start an HTTPS server that serves REST
	 * API requests for a given {@link CloudAdapter}.
	 * <p/>
	 * A failure to parse the command-line arguments will cause the program to
	 * print a usage message and exit with an error code. The function blocks
	 * until the started server is stopped.
	 * 
	 * @param cloudAdapter
	 *            The cloud adapter instance that the started server will
	 *            publish.
	 * @param args
	 *            The command-line arguments.
	 * @throws Exception
	 */
	public static void main(CloudAdapter cloudAdapter, String[] args)
			throws Exception {
		CloudAdapterOptions arguments = new CloudAdapterOptions();
		CmdLineParser parser = new CmdLineParser(arguments);
		parser.setUsageWidth(80);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			log.error("error: " + e.getMessage());
			parser.printUsage(System.err);
			System.exit(-1);
		}

		if (arguments.help) {
			parser.printUsage(System.err);
			System.exit(0);
		}

		Server server = createServer(cloudAdapter, arguments);

		// start server and wait
		log.info("Starting Jetty server with HTTPS port " + arguments.httpsPort);
		try {
			server.start();
		} catch (Exception e) {
			log.error("failed to start server: " + e.getMessage(), e);
			System.exit(-1);
		}
		log.info("Started Jetty server with HTTPS port " + arguments.httpsPort);
		server.join();
	}

	/**
	 * Creates a HTTPS server that serves REST API requests for a given
	 * {@link CloudAdapter}.
	 * <p/>
	 * The created server is returned with the {@link CloudAdapter} REST API
	 * deployed, but in an <i>unstarted</i> state, so the client is responsible
	 * for starting the server.
	 * <p/>
	 * The behavior of the HTTPS server is controlled via a set of
	 * {@link CloudAdapterOptions}.
	 * 
	 * @param cloudAdapter
	 *            The cloud adapter instance that the {@link Server} will
	 *            publish.
	 * @param options
	 *            A set of options that control the behavior of the HTTPS
	 *            server.
	 * @return The created {@link Server}.
	 * @throws Exception
	 *             Thrown on a failure to initialize the {@link CloudAdapter} or
	 *             create the server.
	 */
	public static Server createServer(CloudAdapter cloudAdapter,
			CloudAdapterOptions options) throws Exception {

		JaxRsApplication application = new JaxRsApplication();
		// deploy pool handler
		application.addHandler(new PoolHandler(cloudAdapter));

		if (options.enableConfigHandler) {
			// optionally deploy config handler and config schema handler
			application.addHandler(new ConfigHandler(cloudAdapter));
			application.addHandler(new ConfigSchemaHandler(cloudAdapter));
		}

		if (options.enableExitHandler) {
			// optionally deploy exit handler
			application.addHandler(new ExitHandler());
		}

		if (options.config != null) {
			// optionally configure cloud adapter
			JsonObject configuration = parseJsonConfig(options.config);
			cloudAdapter.configure(configuration);
		}

		// build server
		ServletContainer restApiServlet = new ServletContainer(
				ResourceConfig.forApplication(application));
		ServletDefinition servlet = new ServletDefinition.Builder().servlet(
				restApiServlet).build();
		Server server = ServletServerBuilder.create()
				.httpsPort(options.httpsPort)
				.sslKeyStoreType(KeyStoreType.PKCS12)
				.sslKeyStorePath(options.sslKeyStore)
				.sslKeyStorePassword(options.sslKeyStorePassword)
				.sslTrustStoreType(KeyStoreType.JKS)
				.sslTrustStorePath(options.sslTrustStore)
				.sslTrustStorePassword(options.sslTrustStorePassword)
				.sslRequireClientCert(options.requireClientCert)
				.addServlet(servlet).build();

		return server;
	}

	private static JsonObject parseJsonConfig(String configFile)
			throws IOException {
		JsonObject configuration;
		try {
			configuration = JsonUtils.parseJsonFile(new File(configFile));
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"failed to parse JSON configuration file: "
							+ e.getMessage(), e);
		}
		return configuration;
	}

}
