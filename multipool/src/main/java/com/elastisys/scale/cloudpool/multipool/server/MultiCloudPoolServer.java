package com.elastisys.scale.cloudpool.multipool.server;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.logging.InstanceLogContextFilter;
import com.elastisys.scale.cloudpool.multipool.restapi.restapi.impl.MultiCloudPoolRestApiImpl;
import com.elastisys.scale.commons.cli.CommandLineParser;
import com.elastisys.scale.commons.rest.filters.RequestLogFilter;
import com.elastisys.scale.commons.rest.server.JaxRsApplication;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;

/**
 * Methods for creating a HTTP(S) server that publishes a REST API for a
 * {@link MultiCloudPool}.
 */
public class MultiCloudPoolServer {
    private final static Logger LOG = LoggerFactory.getLogger(MultiCloudPoolServer.class);

    /**
     * Parses command-line arguments into {@link MultiCloudPoolOptions}. On
     * failure to process the command-line arguments, an error will be written
     * together with a usage string and then the program will exit with a
     * failure code.
     * <p/>
     * The {@code --help} will cause the usage text to be written before exiting
     * the program (with a zero exit code).
     *
     * @param args
     * @return
     */
    public static MultiCloudPoolOptions parseArgs(String[] args) {
        CommandLineParser<MultiCloudPoolOptions> parser = new CommandLineParser<>(MultiCloudPoolOptions.class);
        return parser.parseCommandLine(args);
    }

    /**
     * Parse command-line arguments and start an HTTP(S) server that serves REST
     * API requests for a given {@link MultiCloudPool}.
     * <p/>
     * A failure to parse the command-line arguments will cause the program to
     * print a usage message and exit with an error code. The function blocks
     * until the started server is stopped.
     *
     * @param multiCloudPool
     *            The {@link MultiCloudPool} that the started server will
     *            publish.
     * @param args
     *            The command-line arguments.
     * @throws Exception
     */
    public static void main(MultiCloudPool multiCloudPool, String[] args) throws Exception {
        MultiCloudPoolOptions options = parseArgs(args);
        Server server = createServer(multiCloudPool, options);

        // start server and wait
        LOG.info("starting server ...");
        try {
            server.start();
        } catch (Exception e) {
            LOG.error("failed to start server: " + e.getMessage(), e);
            System.exit(-1);
        }
        if (options.httpPort != null) {
            LOG.info("server listening on HTTP port {}", options.httpPort);
        }
        if (options.httpsPort != null) {
            LOG.info("server listening on HTTPS port {}", options.httpsPort);
        }

        server.join();
    }

    /**
     * Creates a HTTP(S) server that serves REST API requests for a given
     * {@link MultiCloudPool}.
     * <p/>
     * The created {@link Server} is returned with the {@link CloudPool} REST
     * API deployed, but in an <i>unstarted</i> state, so the client is
     * responsible for starting the server.
     * <p/>
     * The behavior of the HTTP(S) server is controlled via a set of
     * {@link MultiCloudPoolOptions}.
     *
     * @param multiCloudPool
     *            The {@link MultiCloudPool} that the {@link Server} will
     *            publish.
     * @param options
     *            A set of options that control the behavior of the server.
     * @return The created {@link Server}.
     * @throws Exception
     *             Thrown on a failure to initialize the {@link CloudPool} or
     *             create the server.
     */
    public static Server createServer(MultiCloudPool multiCloudPool, MultiCloudPoolOptions options) throws Exception {
        JaxRsApplication application = new JaxRsApplication();

        MultiCloudPoolRestApiImpl restApiHandler = new MultiCloudPoolRestApiImpl(multiCloudPool);
        application.addHandler(restApiHandler);

        ResourceConfig appConfig = ResourceConfig.forApplication(application);
        appConfig.register(new InstanceLogContextFilter());
        appConfig.register(new RequestLogFilter());

        // build server
        ServletContainer restApiServlet = new ServletContainer(appConfig);
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(restApiServlet)
                .requireBasicAuth(options.requireBasicAuth).realmFile(options.realmFile)
                .requireRole(options.requireRole).build();

        ServletServerBuilder server = ServletServerBuilder.create();
        if (options.httpPort != null) {
            server.httpPort(options.httpPort);
        }
        if (options.httpsPort != null) {
            server.httpsPort(options.httpsPort).sslKeyStoreType(SslKeyStoreType.PKCS12)
                    .sslKeyStorePath(options.sslKeyStore).sslKeyStorePassword(options.sslKeyStorePassword)
                    .sslTrustStoreType(SslKeyStoreType.JKS).sslTrustStorePath(options.sslTrustStore)
                    .sslTrustStorePassword(options.sslTrustStorePassword)
                    .sslRequireClientCert(options.requireClientCert);
        }
        server.addServlet(servlet);
        return server.build();
    }

}
