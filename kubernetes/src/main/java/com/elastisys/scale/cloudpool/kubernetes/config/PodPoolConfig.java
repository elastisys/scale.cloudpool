package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;

/**
 * Describes the group of pod replicas that will be managed by the
 * {@link KubernetesCloudPool}. The {@link KubernetesCloudPool} can control pod
 * replicas either via a ReplicationController, or via a ReplicaSet, or via a
 * Deployment.
 *
 * @see KubernetesCloudPoolConfig
 */
public class PodPoolConfig {

    /** The default Kubernetes namespace. */
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * The <a href="http://kubernetes.io/docs/user-guide/namespaces/">Kubernetes
     * namespace</a> that the managed API construct (either a
     * {@link #replicationController}, or a {@link #replicaSet}, or a
     * {@link #deployment}) exists in. Optional. Default: {@code default}.
     */
    private final String namespace;

    /**
     * The name of a ReplicationController (in the given {@link #namespace})
     * whose group of pod replicas will be managed by the
     * {@link KubernetesCloudPool}. May be <code>null</code> if either
     * {@link #replicaSet} or {@link #deployment} is specified.
     */
    private final String replicationController;

    /**
     * The name of a ReplicaSet (in the given {@link #namespace}) whose group of
     * pod replicas will be managed by the {@link KubernetesCloudPool}. May be
     * <code>null</code> if either {@link #replicationController} or
     * {@link #deployment} is specified.
     */
    private final String replicaSet;

    /**
     * The name of a Deployment (in the given {@link #namespace}) whose group of
     * pod replicas will be managed by the {@link KubernetesCloudPool}. May be
     * <code>null</code> if either {@link #replicationController} or
     * {@link #replicaSet} is specified.
     */
    private final String deployment;

    /**
     * Creates a {@link PodPoolConfig}.
     *
     * @param namespace
     *            The <a href=
     *            "http://kubernetes.io/docs/user-guide/namespaces/">Kubernetes
     *            namespace</a> that the managed API construct (either a
     *            {@link #replicationController}, or a {@link #replicaSet}, or a
     *            {@link #deployment}) exists in. Optional. Default:
     *            {@code default}. Default: {@code default}.
     * @param replicationController
     *            The name of a ReplicationController (in the given
     *            {@link #namespace}) whose group of pod replicas will be
     *            managed by the {@link KubernetesCloudPool}. May be
     *            <code>null</code> if either {@link #replicaSet} or
     *            {@link #deployment} is specified.
     * @param replicaSet
     *            The name of a ReplicaSet (in the given {@link #namespace})
     *            whose group of pod replicas will be managed by the
     *            {@link KubernetesCloudPool}. May be <code>null</code> if
     *            either {@link #replicationController} or {@link #deployment}
     *            is specified.
     * @param deployment
     *            The name of a Deployment (in the given {@link #namespace})
     *            whose group of pod replicas will be managed by the
     *            {@link KubernetesCloudPool}. May be <code>null</code> if
     *            either {@link #replicationController} or {@link #replicaSet}
     *            is specified.
     */
    public PodPoolConfig(String namespace, String replicationController, String replicaSet, String deployment) {
        this.namespace = namespace;
        this.replicationController = replicationController;
        this.replicaSet = replicaSet;
        this.deployment = deployment;
    }

    /**
     * The <a href="http://kubernetes.io/docs/user-guide/namespaces/">Kubernetes
     * namespace</a> that the managed API construct (either a
     * {@link #replicationController}, or a {@link #replicaSet}, or a
     * {@link #deployment}) exists in.
     *
     * @return
     */
    public String getNamespace() {
        return Optional.ofNullable(this.namespace).orElse(DEFAULT_NAMESPACE);
    }

    /**
     * The name of the ReplicationController (in the given {@link #namespace})
     * whose group of pod replicas will be managed by the
     * {@link KubernetesCloudPool}. Will be <code>null</code> if set up to
     * manage a {@link #replicaSet} or a {@link #deployment}.
     *
     * @return
     */
    public String getReplicationController() {
        return this.replicationController;
    }

    /**
     * The name of the ReplicaSet (in the given {@link #namespace}) whose group
     * of pod replicas will be managed by the {@link KubernetesCloudPool}. Will
     * be <code>null</code> if set up to manage a {@link #replicationController}
     * or a {@link #deployment}.
     *
     * @return
     */
    public String getReplicaSet() {
        return this.replicaSet;
    }

    /**
     * The name of the Deployment (in the given {@link #namespace}) whose group
     * of pod replicas will be managed by the {@link KubernetesCloudPool}. Will
     * be <code>null</code> if set up to manage a {@link #replicationController}
     * or a {@link #replicaSet}.
     *
     * @return
     */
    public String getDeployment() {
        return this.deployment;
    }

    /**
     * Returns the type-qualified name of the Kuberntes API object that this
     * {@link PodPoolConfig} represents. For example
     * {@code rc/my-replication-controller}, {@code rs/my-replica-set}, or
     * {@code deployment/my-deployment}
     *
     * @return
     */
    public String getApiObject() {
        if (this.replicationController != null) {
            return "rc/" + this.replicationController;
        } else if (this.replicaSet != null) {
            return "rs/" + this.replicaSet;
        } else {
            return "deployment/" + this.deployment;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.replicationController, this.replicaSet, this.deployment);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodPoolConfig) {
            PodPoolConfig that = (PodPoolConfig) obj;
            return Objects.equals(this.namespace, that.namespace) //
                    && Objects.equals(this.replicationController, that.replicationController) //
                    && Objects.equals(this.replicaSet, that.replicaSet) //
                    && Objects.equals(this.deployment, that.deployment);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        // at least one
        checkArgument(this.replicationController != null || this.replicaSet != null || this.deployment != null,
                "podPool: missing API construct to manage [replicationController, replicaSet, deployment]");
        // at most one
        checkArgument(numSpecifiedApiconstructs() == 1,
                "podPool: can only specify one of [replicationController, replicaSet, deployment]");
    }

    private int numSpecifiedApiconstructs() {
        return (this.replicationController != null ? 1 : 0) + (this.replicaSet != null ? 1 : 0)
                + (this.deployment != null ? 1 : 0);
    }

}
