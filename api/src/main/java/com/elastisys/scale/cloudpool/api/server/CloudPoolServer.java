package com.elastisys.scale.cloudpool.api.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.restapi.CloudPoolHandler;
import com.elastisys.scale.cloudpool.api.restapi.ConfigHandler;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.responsehandlers.ExitHandler;
import com.elastisys.scale.commons.rest.server.JaxRsApplication;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Factory methods for creating a HTTPS server that publishes a REST API for a
 * {@link CloudPool} instance.
 * <p/>
 * The REST API is fully covered in the <a
 * href="http://cloudpoolrestapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud pool REST API documentation</a>.
 */
public class CloudPoolServer {
	private final static Logger LOG = LoggerFactory
			.getLogger(CloudPoolServer.class);

	/**
	 * Parse command-line arguments and start an HTTPS server that serves REST
	 * API requests for a given {@link CloudPool}.
	 * <p/>
	 * A failure to parse the command-line arguments will cause the program to
	 * print a usage message and exit with an error code. The function blocks
	 * until the started server is stopped.
	 *
	 * @param cloudPool
	 *            The cloud pool that the started server will publish.
	 * @param args
	 *            The command-line arguments.
	 * @throws Exception
	 */
	public static void main(CloudPool cloudPool, String[] args)
			throws Exception {
		CloudPoolOptions arguments = new CloudPoolOptions();
		ParserProperties parserConfig = ParserProperties.defaults()
				.withUsageWidth(80);
		CmdLineParser parser = new CmdLineParser(arguments, parserConfig);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			LOG.error("error: " + e.getMessage());
			parser.printUsage(System.err);
			System.exit(-1);
		}

		if (arguments.help) {
			parser.printUsage(System.err);
			System.exit(0);
		}

		if (arguments.version) {
			System.err.println(IoUtils.toString("VERSION.txt", Charsets.UTF_8));
			System.exit(0);
		}

		Server server = createServer(cloudPool, arguments);

		// start server and wait
		LOG.info("Starting Jetty server with HTTPS port " + arguments.httpsPort);
		try {
			server.start();
		} catch (Exception e) {
			LOG.error("failed to start server: " + e.getMessage(), e);
			System.exit(-1);
		}
		LOG.info("Started Jetty server with HTTPS port " + arguments.httpsPort);
		server.join();
	}

	/**
	 * Creates a HTTPS server that serves REST API requests for a given
	 * {@link CloudPool}.
	 * <p/>
	 * The created server is returned with the {@link CloudPool} REST API
	 * deployed, but in an <i>unstarted</i> state, so the client is responsible
	 * for starting the server.
	 * <p/>
	 * The behavior of the HTTPS server is controlled via a set of
	 * {@link CloudPoolOptions}.
	 *
	 * @param cloudPool
	 *            The cloud pool that the {@link Server} will publish.
	 * @param options
	 *            A set of options that control the behavior of the HTTPS
	 *            server.
	 * @return The created {@link Server}.
	 * @throws Exception
	 *             Thrown on a failure to initialize the {@link CloudPool} or
	 *             create the server.
	 */
	public static Server createServer(CloudPool cloudPool,
			CloudPoolOptions options) throws Exception {

		JaxRsApplication application = new JaxRsApplication();
		// deploy pool handler
		application.addHandler(new CloudPoolHandler(cloudPool));

		ConfigHandler configHandler = new ConfigHandler(cloudPool,
				options.storageDir);
		if (options.config != null) {
			// use explicitly specified configuration file
			JsonObject config = parseJsonConfig(options.config);
			cloudPool.configure(config);
			configHandler.storeConfig(config);
		} else {
			// restore cloudpool config from storage directory (if available)
			Optional<JsonObject> config = restoreConfig(configHandler
					.getCloudPoolConfigPath());
			if (config.isPresent()) {
				cloudPool.configure(config.get());
			}
		}

		if (options.enableConfigHandler) {
			// optionally deploy config handler resource
			application.addHandler(configHandler);
		}

		if (options.enableExitHandler) {
			// optionally deploy exit handler
			application.addHandler(new ExitHandler());
		}

		// build server
		ServletContainer restApiServlet = new ServletContainer(
				ResourceConfig.forApplication(application));
		ServletDefinition servlet = new ServletDefinition.Builder()
				.servlet(restApiServlet)
				.requireBasicAuth(options.requireBasicAuth)
				.realmFile(options.realmFile).requireRole(options.requireRole)
				.build();
		Server server = ServletServerBuilder.create()
				.httpsPort(options.httpsPort)
				.sslKeyStoreType(SslKeyStoreType.PKCS12)
				.sslKeyStorePath(options.sslKeyStore)
				.sslKeyStorePassword(options.sslKeyStorePassword)
				.sslTrustStoreType(SslKeyStoreType.JKS)
				.sslTrustStorePath(options.sslTrustStore)
				.sslTrustStorePassword(options.sslTrustStorePassword)
				.sslRequireClientCert(options.requireClientCert)
				.addServlet(servlet).build();

		return server;
	}

	/**
	 * Loads any previously set {@link CloudPool} configuration file from the
	 * storage directory, if one exists.
	 *
	 * @param storageDir
	 * @return The previously set {@link CloudPool} configuration, if available.
	 * @throws IOException
	 */
	private static Optional<JsonObject> restoreConfig(Path configPath)
			throws IOException {
		File cloudPoolConfig = configPath.toFile();
		LOG.info("restoring cloudpool config from {}",
				cloudPoolConfig.getAbsolutePath());
		if (!cloudPoolConfig.isFile()) {
			LOG.info("no cloud pool configuration found at {}. "
					+ "starting without config ...",
					cloudPoolConfig.getAbsolutePath());
			return Optional.absent();
		}

		return Optional.of(parseJsonConfig(cloudPoolConfig.getAbsolutePath()));
	}

	private static JsonObject parseJsonConfig(String configFile)
			throws IOException {
		JsonObject configuration;
		try {
			configuration = JsonUtils.parseJsonFile(new File(configFile));
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format(
					"failed to parse JSON configuration file %s: %s",
					configFile, e.getMessage()), e);
		}
		return configuration;
	}

}
