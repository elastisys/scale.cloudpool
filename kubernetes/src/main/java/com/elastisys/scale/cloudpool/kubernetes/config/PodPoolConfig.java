package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;

/**
 * Describes the
 * <a href="http://kubernetes.io/docs/user-guide/replication-controller/">
 * ReplicationController</a> whose size will be managed by a
 * {@link KubernetesCloudPool}.
 *
 * @see KubernetesCloudPoolConfig
 */
public class PodPoolConfig {

	/** The default Kubernetes namespace. */
	public static final String DEFAULT_NAMESPACE = "default";

	/**
	 * The <a href="http://kubernetes.io/docs/user-guide/namespaces/">Kubernetes
	 * namespace</a> that the ReplicationController being managed lives in.
	 * Optional. Default: {@code default}.
	 */
	private final String namespace;

	/**
	 * The name of the ReplicationController (in the given {@link #namespace})
	 * whose size will be managed by the {@link KubernetesCloudPool}.
	 */
	private final String replicationController;

	/**
	 * Creates a {@link PodPoolConfig}.
	 *
	 * @param namespace
	 *            The
	 *            <a href="http://kubernetes.io/docs/user-guide/namespaces/">
	 *            Kubernetes namespace</a> that the ReplicationController being
	 *            managed lives in. Optional. Default: {@code default}.
	 * @param replicationController
	 *            The name of the ReplicationController (in the given
	 *            {@link #namespace}) whose size will be managed by the
	 *            {@link KubernetesCloudPool}.
	 */
	public PodPoolConfig(String namespace, String replicationController) {
		this.namespace = namespace;
		this.replicationController = replicationController;
	}

	/**
	 * The
	 * <a href="http://kubernetes.io/docs/user-guide/namespaces/"> Kubernetes
	 * namespace</a> that the ReplicationController being managed lives in.
	 *
	 * @return
	 */
	public String getNamespace() {
		return Optional.ofNullable(this.namespace).orElse(DEFAULT_NAMESPACE);
	}

	/**
	 * The name of the ReplicationController (in the given {@link #namespace})
	 * whose size will be managed by the {@link KubernetesCloudPool}.
	 *
	 * @return
	 */
	public String getReplicationController() {
		return this.replicationController;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getNamespace(), getReplicationController());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PodPoolConfig) {
			PodPoolConfig that = (PodPoolConfig) obj;
			return Objects.equals(this.getNamespace(), that.getNamespace())
					&& Objects.equals(this.replicationController,
							that.replicationController);
		}
		return false;
	}

	public void validate() throws IllegalArgumentException {
		checkArgument(this.replicationController != null,
				"podPool: missing replicationController name");
	}
}
