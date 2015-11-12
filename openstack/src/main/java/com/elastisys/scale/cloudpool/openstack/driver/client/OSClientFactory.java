package com.elastisys.scale.cloudpool.openstack.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV2Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;

/**
 * A factory for creating authenticated {@link OSClient} objects ready for use.
 */
public class OSClientFactory {

	/**
	 * The {@link OSClientCreator} used to instantiate {@link OSClient}s for
	 * different auth schemes.
	 */
	private final OSClientCreator creator;

	/**
	 * Creates an {@link OSClientFactory} with a {@link StandardOSClientCreator}
	 * creating clients with the given connection timeouts.
	 *
	 * @param connectionTimeout
	 *            The timeout in milliseconds until a connection is established.
	 * @param socketTimeout
	 *            The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which
	 *            is the timeout for waiting for data or, put differently, a
	 *            maximum period inactivity between two consecutive data
	 *            packets).
	 *
	 */
	public OSClientFactory(int connectionTimeout, int socketTimeout) {
		this(new StandardOSClientCreator(connectionTimeout, socketTimeout));
	}

	/**
	 * Creates an {@link OSClientFactory} with a custom {@link OSClientCreator}.
	 */
	public OSClientFactory(OSClientCreator creator) {
		this.creator = creator;
	}

	/**
	 * Creates an {@link OSClient} that has been authenticated against a
	 * Keystone identity service using the authentication scheme specified in an
	 * {@link AuthConfig}.
	 *
	 * @param authConfig
	 *            Specified how to authenticate.
	 * @return An authenticated {@link OSClient} ready for use.
	 */
	public OSClient createAuthenticatedClient(AuthConfig authConfig) {
		checkArgument(authConfig.getKeystoneUrl() != null,
				"cannot authenticate without a keystone endpoint URL");
		checkArgument(authConfig.isV2Auth() ^ authConfig.isV3Auth(),
				"*either* version 2 or version 3 style "
						+ "authentication needs to be specified");
		if (authConfig.isV2Auth()) {
			AuthV2Credentials v2Creds = authConfig.getV2Credentials();
			return this.creator.fromV2Auth(authConfig.getKeystoneUrl(),
					v2Creds.getTenantName(), v2Creds.getUserName(),
					v2Creds.getPassword());
		} else {
			AuthV3Credentials v3Creds = authConfig.getV3Credentials();
			checkArgument(v3Creds.isDomainScoped() ^ v3Creds.isProjectScoped(),
					"version 3 type credentials msut be either domain- or project-scoped");
			if (v3Creds.isDomainScoped()) {
				return this.creator.fromDomainScopedV3Auth(
						authConfig.getKeystoneUrl(),
						v3Creds.getScope().getDomainId(), v3Creds.getUserId(),
						v3Creds.getPassword());
			} else {
				// project scoped v3 auth
				return this.creator.fromProjectScopedV3Auth(
						authConfig.getKeystoneUrl(),
						v3Creds.getScope().getProjectId(), v3Creds.getUserId(),
						v3Creds.getPassword());
			}
		}
	}

	/**
	 * {@link OSClient} creation methods for different kinds of authentication
	 * schemes.
	 */
	interface OSClientCreator {
		OSClient fromV2Auth(String keystoneUrl, String tenantName, String user,
				String password);

		OSClient fromProjectScopedV3Auth(String keystoneUrl, String projectId,
				String userId, String password);

		OSClient fromDomainScopedV3Auth(String keystoneUrl, String domainId,
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
		public OSClient fromV2Auth(String keystoneUrl, String tenantName,
				String user, String password) {
			return OSFactory.builder().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(user, password)
					.tenantName(tenantName).authenticate();
		}

		private Config clientConfig() {
			return Config.newConfig()
					.withConnectionTimeout(this.connectionTimeout)
					.withReadTimeout(this.socketTimeout);
		}

		@Override
		public OSClient fromProjectScopedV3Auth(String keystoneUrl,
				String projectId, String userId, String password) {
			return OSFactory.builderV3().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(userId, password)
					.scopeToProject(Identifier.byId(projectId),
							Identifier.byId(projectId))
					.authenticate();
		}

		@Override
		public OSClient fromDomainScopedV3Auth(String keystoneUrl,
				String domainId, String userId, String password) {
			return OSFactory.builderV3().withConfig(clientConfig())
					.endpoint(keystoneUrl).credentials(userId, password)
					.scopeToDomain(Identifier.byId(domainId)).authenticate();
		}
	}
}
