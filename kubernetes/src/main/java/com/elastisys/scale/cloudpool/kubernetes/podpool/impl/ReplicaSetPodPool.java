package com.elastisys.scale.cloudpool.kubernetes.podpool.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.text.MessageFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPoolSize;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSet;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSetSpec;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A {@link PodPool} that manages a group of pods via a
 * <a href= "https://kubernetes.io/docs/user-guide/replicasets/">Kubernetes
 * ReplicaSet</a>.
 */
public class ReplicaSetPodPool implements PodPool {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicaSetPodPool.class);

    /** ReplicaSet resource path. 0: namespace, 1: replicaSet name. */
    public static final String REPLICA_SET_PATH = "/apis/extensions/v1beta1/namespaces/{0}/replicasets/{1}";

    private ApiServerClient apiServer;
    /** The kubernetes namespace under which the replicationSet exists. */
    private String namespace;
    /** The name of the replicationSet. */
    private String replicaSet;

    @Override
    public PodPool configure(ApiServerClient apiServerClient, String apiObjectNamespace, String apiObjectName) {
        checkArgument(apiServerClient != null, "apiServerClient cannot be null");
        checkArgument(apiObjectNamespace != null, "apiObjectNamespace cannot be null");
        checkArgument(apiObjectName != null, "apiObjectName cannot be null");

        this.apiServer = apiServerClient;
        this.namespace = apiObjectNamespace;
        this.replicaSet = apiObjectName;
        return this;
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl get pods --selector="app=nginx,version in (1.11)" --output=json
     * </pre>
     *
     * @see com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool#getPods()
     */
    @Override
    public List<Pod> getPods() throws KubernetesApiException {
        ensureConfigured();
        try {
            ReplicaSet rs = getReplicaSet();
            String podSelector = determinePodSelector(rs);
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
            ReplicaSet rs = getReplicaSet();
            return new PodPoolSize(rs.spec.replicas, rs.status.replicas);
        } catch (Exception e) {
            throw new KubernetesApiException("failed to determine size: " + e.getMessage(), e);
        }
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl scale --replicas=2 -n default rs/nginx
     * </pre>
     *
     * or
     *
     * <pre>
     * curl [options] -X PATCH --header 'Content-Type: application/merge-patch+json' \
     *     -d '{"spec": {"replicas": 2}}' https://host:443/apis/extensions/v1beta1/namespaces/default/replicasets/nginx
     * </pre>
     *
     * @see com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool#setDesiredSize(
     *      int)
     */
    @Override
    public void setDesiredSize(int desiredSize) throws KubernetesApiException {
        ensureConfigured();

        ReplicaSet rs = new ReplicaSet();
        rs.spec = new ReplicaSetSpec();
        rs.spec.replicas = desiredSize;
        JsonObject update = JsonUtils.toJson(rs).getAsJsonObject();

        try {
            this.apiServer.patch(replicaSetPath(), update);
        } catch (Exception e) {
            throw new KubernetesApiException(String.format("failed to update number of replicas for replica set %s: %s",
                    qualifiedName(), e.getMessage()), e);
        }
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl get rs nginx --output=json
     * </pre>
     *
     * @return
     * @throws KubernetesApiException
     */
    private ReplicaSet getReplicaSet() {
        try {
            JsonObject response = this.apiServer.get(replicaSetPath());
            return JsonUtils.toObject(response, ReplicaSet.class);
        } catch (Exception e) {
            throw new KubernetesApiException(
                    String.format("failed to get replica set %s: %s", qualifiedName(), e.getMessage()), e);
        }
    }

    /**
     * Determines the <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a> that matches the {@link Pod}s that belong to a given
     * {@link ReplicaSet}.
     *
     * @param rs
     * @return
     */
    private String determinePodSelector(ReplicaSet rs) {
        // The '.spec.selector' field holds the label selectors. If not
        // explicitly set at creation time, spec.selector.matchLabels will be
        // set to match '.spec.template.metadata.labels'.
        // See https://kubernetes.io/docs/user-guide/replicasets/
        checkArgument(rs.spec.selector != null, "replica set missing .spec.selector field");

        return rs.spec.selector.toLabelSelectorExpression();
    }

    private String replicaSetPath() {
        return MessageFormat.format(REPLICA_SET_PATH, this.namespace, this.replicaSet);
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.apiServer != null && this.namespace != null && this.replicaSet != null,
                "cannot use {} before it has been configured", getClass().getSimpleName());
    }

    private String qualifiedName() {
        return this.namespace + "/" + this.replicaSet;
    }
}
