package com.elastisys.scale.cloudpool.kubernetes.podpool;

import java.util.List;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;

/**
 * Uniform interface to manage a group of Kubernetes pod replicas, which
 * abstracts away the details of the particular type of kubernetes construct
 * used to define the group of pods (replication controllers, replica sets,
 * deployments).
 */
public interface PodPool {

    /**
     * Configures this {@link PodPool} to use a particular API client and manage
     * a certain API object (replication controller, replica set, deployment).
     * <p/>
     * Note: the particular {@link PodPool} implementation may be limited to
     * handle a particular kind of API object (such as replication controller).
     *
     * @param apiServerClient
     *            A client used to access a particular Kubernetes apiserver.
     * @param apiObjectNamespace
     *            The Kubernetes namespace where the managed API object
     *            (replication controller, replica set, deployment) exists. For
     *            example, {@code default}.
     * @param apiObjectName
     *            The name of the managed Kubernetes API object (replication
     *            controller, replica set, deployment). For example,
     *            {@code my-nginx-deployment}.
     * @return The {@link PodPool} itself (to allow method chaining).
     */
    PodPool configure(ApiServerClient apiServerClient, String apiObjectNamespace, String apiObjectName);

    /**
     * Returns the collection of {@link Pod}s that this {@link PodPool}
     * currently consists of.
     *
     * @return
     * @throws KubernetesApiException
     *             if the request could not be completed.
     *
     */
    List<Pod> getPods() throws KubernetesApiException;

    /**
     * Returns the size of the {@link PodPool}, both the desired and actual
     * number of replicas.
     *
     * @return
     * @throws KubernetesApiException
     */
    PodPoolSize getSize() throws KubernetesApiException;

    /**
     * Updates the desired number of {@link Pod}s for this {@link PodPool}.
     *
     * @param desiredSize
     * @throws KubernetesApiException
     *             if the request could not be completed.
     */
    void setDesiredSize(int desiredSize) throws KubernetesApiException;
}
