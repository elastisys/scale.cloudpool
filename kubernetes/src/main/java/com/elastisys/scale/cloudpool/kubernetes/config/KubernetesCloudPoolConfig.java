package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.commons.basepool.config.PoolUpdateConfig;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Carries configuration values for a {@link KubernetesCloudPool}. Declares
 * which Kubernetes API server to connect to (including authentication details)
 * and the
 * <a href="http://kubernetes.io/docs/user-guide/replication-controller/">
 * ReplicationController</a> whose size will be managed.
 *
 */
public class KubernetesCloudPoolConfig {
	/** Default apiserver. */
	private static final ApiServerConfig DEFAULT_API_SERVER_CONFIG = new ApiServerConfig(
			ApiServerConfig.DEFAULT_HOST, ApiServerConfig.DEFAULT_PORT);
	/** Default update interval. */
	private static final PoolUpdateConfig DEFAULT_UPDATE_CONFIG = new PoolUpdateConfig(
			new TimeInterval(10L, TimeUnit.SECONDS));

	/** Configuration declaring how to reach the Kubernetes apiserver. */
	private final ApiServerConfig apiServer;
	/**
	 * Configuration declaring the ReplicationController whose pod set is to be
	 * scaled.
	 */
	private final PodPoolConfig podPool;
	/** Client authentication configuration. */
	private final AuthConfig auth;

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 */
	private final PoolUpdateConfig poolUpdate;

	/**
	 * Creates a {@link KubernetesCloudPoolConfig}.
	 *
	 * @param apiServer
	 *            Configuration declaring how to reach the Kubernetes apiserver.
	 * @param podPool
	 *            Configuration declaring the ReplicationController whose pod
	 *            set is to be scaled.
	 * @param auth
	 *            Client authentication configuration.
	 */
	public KubernetesCloudPoolConfig(ApiServerConfig apiServer,
			PodPoolConfig podPool, AuthConfig auth,
			PoolUpdateConfig poolUpdate) {
		this.apiServer = apiServer;
		this.podPool = podPool;
		this.auth = auth;
		this.poolUpdate = poolUpdate;
	}

	/**
	 * Configuration declaring how to reach the Kubernetes apiserver.
	 *
	 * @return
	 */
	public ApiServerConfig getApiServer() {
		return Optional.ofNullable(this.apiServer)
				.orElse(DEFAULT_API_SERVER_CONFIG);
	}

	/**
	 * Configuration declaring the ReplicationController whose pod set is to be
	 * scaled.
	 *
	 * @return
	 */
	public PodPoolConfig getPodPool() {
		return this.podPool;
	}

	/**
	 * Client authentication configuration.
	 *
	 * @return
	 */
	public AuthConfig getAuth() {
		return this.auth;
	}

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 *
	 * @return
	 */
	public PoolUpdateConfig getPoolUpdate() {
		return Optional.ofNullable(this.poolUpdate)
				.orElse(DEFAULT_UPDATE_CONFIG);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getApiServer(), this.podPool, this.auth,
				getPoolUpdate());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KubernetesCloudPoolConfig) {
			KubernetesCloudPoolConfig that = (KubernetesCloudPoolConfig) obj;
			return Objects.equals(this.getApiServer(), that.getApiServer())
					&& Objects.equals(this.podPool, that.podPool)
					&& Objects.equals(this.auth, that.auth) && Objects
							.equals(this.getPoolUpdate(), that.getPoolUpdate());
		}
		return false;
	}

	public void validate() throws IllegalArgumentException {
		checkArgument(this.getApiServer() != null,
				"config: no apiServer given");
		checkArgument(this.podPool != null, "config: no podPool given");
		checkArgument(this.auth != null, "config: no auth given");

		this.getApiServer().validate();
		this.podPool.validate();
		this.auth.validate();
		this.getPoolUpdate().validate();
	}
}
