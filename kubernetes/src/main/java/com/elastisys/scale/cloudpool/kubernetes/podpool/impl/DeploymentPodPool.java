package com.elastisys.scale.cloudpool.kubernetes.podpool.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.text.MessageFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPoolSize;
import com.elastisys.scale.cloudpool.kubernetes.types.Deployment;
import com.elastisys.scale.cloudpool.kubernetes.types.DeploymentSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A {@link PodPool} that manages a group of pods via a
 * <a href= "https://kubernetes.io/docs/user-guide/deployments/">Kubernetes
 * deployment</a>.
 */
public class DeploymentPodPool implements PodPool {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentPodPool.class);

    /** Deployment resource path. 0: namespace, 1: deployment name. */
    public static final String DEPLOYMENT_PATH = "/apis/extensions/v1beta1/namespaces/{0}/deployments/{1}";

    private ApiServerClient apiServer;
    /** The kubernetes namespace under which the deployment exists. */
    private String namespace;
    /** The name of the deployment. */
    private String deployment;

    @Override
    public PodPool configure(ApiServerClient apiServerClient, String apiObjectNamespace, String apiObjectName) {
        checkArgument(apiServerClient != null, "apiServerClient cannot be null");
        checkArgument(apiObjectNamespace != null, "apiObjectNamespace cannot be null");
        checkArgument(apiObjectName != null, "apiObjectName cannot be null");

        this.apiServer = apiServerClient;
        this.namespace = apiObjectNamespace;
        this.deployment = apiObjectName;
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
            Deployment deployment = getDeployment();
            String podSelector = determinePodSelector(deployment);
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
            Deployment deployment = getDeployment();
            Integer desired = deployment.spec.replicas;
            Integer actual = 0;
            if (deployment.status.replicas != null) {
                // it seems to be the case that if 'deployment.spec.replicas' is
                // 0 then 'deployment.status.replicas' may be missing. if so we
                // assume it's 0.
                actual = deployment.status.replicas;
            }
            return new PodPoolSize(desired, actual);
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
     * curl [options] -X PATCH --header 'Content-Type: application/merge-patch+json' \
     *     -d '{"spec": {"replicas": 2}}' https://host:443/apis/extensions/v1beta1/namespaces/default/deployments/nginx
     * </pre>
     *
     * @see com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool#setDesiredSize(
     *      int)
     */
    @Override
    public void setDesiredSize(int desiredSize) throws KubernetesApiException {
        ensureConfigured();

        Deployment deployment = new Deployment();
        deployment.spec = new DeploymentSpec();
        deployment.spec.replicas = desiredSize;
        JsonObject update = JsonUtils.toJson(deployment).getAsJsonObject();

        try {
            this.apiServer.patch(deploymentPath(), update);
        } catch (Exception e) {
            throw new KubernetesApiException(String.format("failed to update number of replicas for deployment %s: %s",
                    qualifiedName(), e.getMessage()), e);
        }
    }

    /**
     * This call is analogous to:
     *
     * <pre>
     * kubectl get deployment nginx --output=json
     * </pre>
     *
     * @return
     * @throws KubernetesApiException
     */
    private Deployment getDeployment() throws KubernetesApiException {
        try {
            JsonObject response = this.apiServer.get(deploymentPath());
            return JsonUtils.toObject(response, Deployment.class);
        } catch (Exception e) {
            throw new KubernetesApiException(
                    String.format("failed to get deployment %s: %s", qualifiedName(), e.getMessage()), e);
        }
    }

    /**
     * Determines the <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a> that matches the {@link Pod}s that belong to a given
     * {@link Deployment}.
     *
     * @param deployment
     * @return
     */
    private String determinePodSelector(Deployment deployment) {
        // The '.spec.selector' field holds the label selectors. If not
        // explicitly set at creation time, spec.selector.matchLabels will be
        // set to match '.spec.template.metadata.labels'.
        // See https://kubernetes.io/docs/user-guide/deployments/#selector
        checkArgument(deployment.spec.selector != null, "deployment missing .spec.selector field");

        return deployment.spec.selector.toLabelSelectorExpression();
    }

    private String deploymentPath() {
        return MessageFormat.format(DEPLOYMENT_PATH, this.namespace, this.deployment);
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.apiServer != null && this.namespace != null && this.deployment != null,
                "cannot use {} before it has been configured", getClass().getSimpleName());
    }

    private String qualifiedName() {
        return this.namespace + "/" + this.deployment;
    }
}
