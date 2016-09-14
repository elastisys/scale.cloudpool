package com.elastisys.scale.cloudpool.kubernetes.client;

import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;

/**
 * A Kubernetes apiserver access client, supporting only the operations required
 * by the {@link KubernetesCloudPool} to manage the pods of a
 * ReplicationController.
 * <p/>
 * Before using the client, it must be configured via a call to
 * {@code configure()}.
 */
public interface KubernetesClient {
    /**
     * Configures the {@link KubernetesClient} in terms of how to connect to the
     * Kubernetes apiserver and what ReplicationController to manage.
     *
     * @param config
     * @throws IllegalArgumentException
     */
    public void configure(KubernetesCloudPoolConfig config) throws IllegalArgumentException;

    /**
     * Returns the size of the configured ReplicationController (the number of
     * pods it is managing).
     *
     * @return
     * @throws KubernetesApiException
     */
    public PoolSizeSummary getPoolSize() throws KubernetesApiException;

    /**
     * Retrieves metadata about all pods managed by the configured replication
     * controller, as described <a href=
     * "http://kubernetes.io/docs/api-reference/v1/operations/#_list_or_watch_objects_of_kind_pod">
     * in the API docs</a>. This call is analogous to
     *
     * <pre>
     * kubectl get pods --selector="app=nginx" --output=json
     * </pre>
     *
     * @return @throws KubernetesApiException
     */
    public MachinePool getMachinePool() throws KubernetesApiException;

    /**
     * Sets the desired size of the configured ReplicationController. This call
     * is analogous to:
     *
     * <pre>
     * curl [options]  -X PATCH  -d '{"spec": {"replicas": 2}}' \
     *   --header 'Content-Type: application/merge-patch+json' \
     *   https://kubernetes:443/api/v1/namespaces/default/replicationcontrollers/nginx
     * </pre>
     *
     * or
     *
     * <pre>
     * kubectl scale --replicas=2 rc/nginx
     * </pre>
     *
     * @param desiredSize
     *            The desired size of the ReplicationController.
     * @throws KubernetesApiException
     */
    public void setDesiredSize(int desiredSize) throws KubernetesApiException;
}
