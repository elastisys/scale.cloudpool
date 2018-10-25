package com.elastisys.scale.cloudpool.kubernetes.podpool.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPoolSize;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationController;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationControllerSpec;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A {@link PodPool} that manages a group of pods via a <a href=
 * "https://kubernetes.io/docs/user-guide/replication-controller/">Replication
 * Controller</a>.
 */
public class ReplicationControllerPodPool implements PodPool {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicationControllerPodPool.class);

    /**
     * Replication controller resource path. 0: namespace, 1: replication
     * controller name.
     */
    public static final String REPLICATION_CONTROLLER_PATH = "/api/v1/namespaces/{0}/replicationcontrollers/{1}";

    private ApiServerClient apiServer;
    /**
     * The kubernetes namespace under which the replication controller exists.
     */
    private String namespace;
    /** The name of the replication controller. */
    private String replicationController;

    @Override
    public PodPool configure(ApiServerClient apiServerClient, String apiObjectNamespace, String apiObjectName) {
        checkArgument(apiServerClient != null, "apiServerClient cannot be null");
        checkArgument(apiObjectNamespace != null, "apiObjectNamespace cannot be null");
        checkArgument(apiObjectName != null, "apiObjectName cannot be null");
        this.apiServer = apiServerClient;
        this.namespace = apiObjectNamespace;
        this.replicationController = apiObjectName;
        return this;
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl get pods --selector="app=nginx,env=production" --output=json
     * </pre>
     *
     * @see com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool#getPods()
     */
    @Override
    public List<Pod> getPods() throws KubernetesApiException {
        ensureConfigured();

        try {
            ReplicationController rc = getReplicationController();
            String podSelector = determinePodSelector(rc);
            LOG.debug("pod selector: {}", podSelector);
            PodList podList = new PodQuery(this.apiServer, this.namespace, podSelector).call();
            return podList.items;
        } catch (Exception e) {
            throw new KubernetesApiException("failed to get pods: " + e.getMessage(), e);
        }
    }

    @Override
    public PodPoolSize getSize() throws KubernetesApiException {
        ensureConfigured();
        try {
            ReplicationController rc = getReplicationController();
            return new PodPoolSize(rc.spec.replicas, rc.status.replicas);
        } catch (Exception e) {
            throw new KubernetesApiException("failed to determine size: " + e.getMessage(), e);
        }
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl scale --replicas=2 -n default rc/nginx
     * </pre>
     *
     * or
     *
     * <pre>
     * curl [options]  -X PATCH  -d '{"spec": {"replicas": 2}}' \
     *   --header 'Content-Type: application/merge-patch+json' \
     *   https://host:443/api/v1/namespaces/default/replicationcontrollers/nginx
     * </pre>
     *
     * @see com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool#setDesiredSize(
     *      int)
     */
    @Override
    public void setDesiredSize(int desiredSize) throws KubernetesApiException {
        ensureConfigured();

        ReplicationController rc = new ReplicationController();
        rc.spec = new ReplicationControllerSpec();
        rc.spec.replicas = desiredSize;
        JsonObject update = JsonUtils.toJson(rc).getAsJsonObject();
        try {
            this.apiServer.patch(replicationControllerPath(), update);
        } catch (Exception e) {
            throw new KubernetesApiException(
                    String.format("failed to update number of replicas for replication controller %s: %s",
                            qualifiedName(), e.getMessage()),
                    e);
        }
    }

    /**
     * Determines the <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a> that matches the {@link Pod}s that belong to a given
     * {@link ReplicationController}.
     *
     * @param rc
     * @return
     */
    private String determinePodSelector(ReplicationController rc) {
        // The label selector is found in the '.spec.selector' field. If none is
        // specified at creation-time, the '.spec.selector' will be set to the
        // value of '.spec.template.metadata.labels'.
        // https://kubernetes.io/docs/api-reference/v1.5/#replicationcontrollerspec-v1
        checkArgument(rc.spec.selector != null, "ReplicationController missing .spec.selector field");

        List<String> selectors = new ArrayList<>();
        for (Entry<String, String> selector : rc.spec.selector.entrySet()) {
            selectors.add(String.format("%s=%s", selector.getKey(), selector.getValue()));
        }
        return String.join(",", selectors);
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl get rc nginx --output=json
     * </pre>
     *
     * @return
     * @throws KubernetesApiException
     */
    private ReplicationController getReplicationController() throws KubernetesApiException {
        try {
            JsonObject response = this.apiServer.get(replicationControllerPath());
            return JsonUtils.toObject(response, ReplicationController.class);
        } catch (Exception e) {
            throw new KubernetesApiException(
                    String.format("failed to get replication controller %s: %s", qualifiedName(), e.getMessage()), e);
        }
    }

    private String replicationControllerPath() {
        return MessageFormat.format(REPLICATION_CONTROLLER_PATH, this.namespace, this.replicationController);
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.apiServer != null && this.namespace != null && this.replicationController != null,
                "cannot use {} before it has been configured", getClass().getSimpleName());
    }

    private String qualifiedName() {
        return this.namespace + "/" + this.replicationController;
    }

}
