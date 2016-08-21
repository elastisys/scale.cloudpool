package com.elastisys.scale.cloudpool.openstack.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.OSClient.OSClientV2;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.OSClientSession.OSClientSessionV2;
import org.openstack4j.openstack.internal.OSClientSession.OSClientSessionV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV2Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

/**
 * A factory for creating authenticated {@link OSClient} objects ready for use.
 */
public class OSClientFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(OSClientFactory.class);
	/**
	 * API access configuration that describes how to authenticate with and
	 * communicate over the OpenStack API.
	 */
	private final OpenStackPoolDriverConfig apiAccessConfig;

	/**
	 * The {@link OSClientCreator} used to instantiate {@link OSClient}s for
	 * different auth schemes.
	 */
	private final OSClientCreator creator;

	/**
	 * An authenticated client that, after initialization, will serve as a
	 * template for creating additional per-thread {@link OSClient}s that re-use
	 * the same authentication token. As explained
	 * <a href="http://www.openstack4j.com/learn/threads/">here</a>, client
	 * sessions are thread-scoped but an authentication token from an
	 * authenticated client can be re-used by {@link OSClient}s bound to other
	 * threads.
	 */
	private OSClient authenticatedClient = null;

	/** Mutex to protect critical sections. */
	private Object lock = new Object();

	/**
	 * Creates an {@link OSClientFactory} with a {@link StandardOSClientCreator}
	 * creating clients according to the given configuration.
	 *
	 * @param apiAccessConfig
	 *            API access configuration that describes how to authenticate
	 *            with and communicate over the OpenStack API.
	 */
	public OSClientFactory(OpenStackPoolDriverConfig apiAccessConfig) {
		this(apiAccessConfig,
				new StandardOSClientCreator(
						apiAccessConfig.getConnectionTimeout(),
						apiAccessConfig.getSocketTimeout()));
	}

	/**
	 * Creates an {@link OSClientFactory} with a custom {@link OSClientCreator}.
	 *
	 * @param apiAccessConfig
	 *            API access configuration that describes how to authenticate
	 *            with and communicate over the OpenStack API.
	 *
	 */
	public OSClientFactory(OpenStackPoolDriverConfig apiAccessConfig,
			OSClientCreator creator) {
		checkArgument(apiAccessConfig != null, "no apiAccessConfig given");
		checkArgument(creator != null, "no creator given");
		apiAccessConfig.validate();

		this.apiAccessConfig = apiAccessConfig;
		this.creator = creator;
	}

	/**
	 * Returns an OpenStack API client, authenticated and configured according
	 * to the {@link OpenStackPoolDriverConfig} provided at construction-time.
	 * <p/>
	 * <b>Note</b>: the returned {@link OSClient} is bound to the calling thread
	 * (since Openstack4j uses thread-scoped sessions) and should therefore
	 * never be used by a different thread. If another thread needs to make use
	 * of an {@link OSClient}, pass the {@link OSClientFactory} to that thread
	 * and make a new call to {@link #authenticatedClient()}.
	 *
	 * @return An authenticated {@link OSClient} ready for use.
	 */
	public OSClient authenticatedClient() {
		// check if we need to do the first-time initialization of the seed
		// client
		synchronized (this.lock) {
			if (this.authenticatedClient == null) {
				this.authenticatedClient = acquireAuthenticatedClient();
				this.authenticatedClient
						.useRegion(this.apiAccessConfig.getRegion());
			}
		}

		// check if a client session is already bound to this thread and, if so,
		// return that client.
		if (OSClientSession.getCurrent() != null) {
			if (this.authenticatedClient instanceof OSClientV2) {
			    return (OSClientSessionV2) OSClientSession.getCurrent();
			} else {
			    return (OSClientSessionV3) OSClientSession.getCurrent();
			}
		} else {
			// if no client session is already bound to this thread, a copy
			// that reuses the same auth token as the template client is bound
			// to serve the current thread.
			OSClient threadClient;
			if (this.authenticatedClient instanceof OSClientV2) {
				OSClientV2 client = (OSClientV2) this.authenticatedClient;
				threadClient = OSFactory.clientFromAccess(client.getAccess());
			} else {
				OSClientV3 client = (OSClientV3) this.authenticatedClient;
				threadClient = OSFactory.clientFromToken(client.getToken());
			}
			return threadClient.useRegion(this.apiAccessConfig.getRegion());
		}
	}

	/**
	 * Creates a new {@link OSClient} by authenticating against a Keystone
	 * identity service using the authentication scheme specified in the
	 * {@link OpenStackPoolDriverConfig} supplied at construction-time.
	 *
	 * @return An authenticated {@link OSClient} ready for use.
	 */
	OSClient acquireAuthenticatedClient() {
		AuthConfig auth = this.apiAccessConfig.getAuth();
		checkArgument(auth.getKeystoneUrl() != null,
				"cannot authenticate without a keystone endpoint URL");
		checkArgument(auth.isV2Auth() ^ auth.isV3Auth(),
				"*either* version 2 or version 3 style "
						+ "authentication needs to be specified");

		LOG.debug("acquiring an authenticated openstack client ...");

		if (auth.isV2Auth()) {
			AuthV2Credentials v2Creds = auth.getV2Credentials();
			return this.creator.fromV2Auth(auth.getKeystoneUrl(),
					v2Creds.getTenantName(), v2Creds.getUserName(),
					v2Creds.getPassword());
		} else {
			AuthV3Credentials v3Creds = auth.getV3Credentials();
			checkArgument(v3Creds.isDomainScoped() ^ v3Creds.isProjectScoped(),
					"version 3 type credentials msut be either domain- or project-scoped");
			if (v3Creds.isDomainScoped()) {
				return this.creator.fromDomainScopedV3Auth(
						auth.getKeystoneUrl(), v3Creds.getScope().getDomainId(),
						v3Creds.getUserId(), v3Creds.getPassword());
			} else {
				// project scoped v3 auth
				return this.creator.fromProjectScopedV3Auth(
						auth.getKeystoneUrl(),
						v3Creds.getScope().getProjectId(), v3Creds.getUserId(),
						v3Creds.getPassword());
			}
		}
	}

	/**
	 * Returns the API access configuration that describes how to authenticate
	 * with and communicate over the OpenStack API.
	 *
	 * @return
	 */
	public OpenStackPoolDriverConfig getApiAccessConfig() {
		return this.apiAccessConfig;
	}

	/**
	 * {@link OSClient} creation methods for different kinds of authentication
	 * schemes.
	 */
	interface OSClientCreator {
		OSClientV2 fromV2Auth(String keystoneUrl, String tenantName, String user,
				String password);

		OSClientV3 fromProjectScopedV3Auth(String keystoneUrl, String projectId,
				String userId, String password);

		OSClientV3 fromDomainScopedV3Auth(String keystoneUrl, String domainId,
				String userId, String password);
	}

	/**
	 * Default {@link OSClientCreator} implementation.
	 */
	private static class StandardOSClientCreator implements OSClientCreator {

		/**
		 * The timeout in milliseconds until a connection is established.
		 */
		private final int connectionTimeout;

		/**
		 * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
		 * timeout for waiting for data or, put differently, a maximum period
		 * inactivity between two consecutive data packets).
		 */
		private final int socketTimeout;

		/**
		 * Creates a new {@link StandardOSClientCreator}.
		 *
		 * @param connectionTimeout
		 *            The timeout in milliseconds until a connection is
		 *            established.
		 * @param socketTimeout
		 *            The socket timeout ({@code SO_TIMEOUT}) in milliseconds,
		 *            which is the timeout for waiting for data or, put
		 *            differently, a maximum period inactivity between two
		 *            consecutive data packets).
		 */
		public StandardOSClientCreator(int connectionTimeout,
				int socketTimeout) {
			this.connectionTimeout = connectionTimeout;
			this.socketTimeout = socketTimeout;
		}

		@Override
		public OSClientV2 fromV2Auth(String keystoneUrl, String tenantName,
				String user, String password) {
			return OSFactory.builderV2().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(user, password)
					.tenantName(tenantName).authenticate();
		}

		private Config clientConfig() {
			return Config.newConfig()
					.withConnectionTimeout(this.connectionTimeout)
					.withReadTimeout(this.socketTimeout);
		}

		@Override
		public OSClientV3 fromProjectScopedV3Auth(String keystoneUrl,
				String projectId, String userId, String password) {
			return OSFactory.builderV3().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(userId, password)
					.scopeToProject(Identifier.byId(projectId),
							Identifier.byId(projectId))
					.authenticate();
		}

		@Override
		public OSClientV3 fromDomainScopedV3Auth(String keystoneUrl,
				String domainId, String userId, String password) {
			return OSFactory.builderV3().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(userId, password)
					.scopeToDomain(Identifier.byId(domainId)).authenticate();
		}
	}
}
