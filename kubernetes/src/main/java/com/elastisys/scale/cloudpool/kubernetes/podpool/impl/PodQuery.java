package com.elastisys.scale.cloudpool.kubernetes.podpool.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Charsets;

/**
 * A callable task that, when called, lists {@link Pod}s in a given namespace,
 * restricting the listing to only include {@link Pod}s with labels that match a
 * given <a href=
 * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
 * selector</a>.
 *
 */
public class PodQuery implements Callable<PodList> {
    /** List pods resource path. 0: namespace, 1: label selector. */
    public static final String LIST_PODS_PATH = "/api/v1/namespaces/{0}/pods?labelSelector={1}";

    /** An API server client. */
    private final ApiServerClient apiServer;
    /** The namespace that the {@link Pod} listing will be limited to. */
    private final String namespace;
    /**
     * Restricts the {@link Pod} listing to include only {@link Pod}s with
     * matching labels. See <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selectors</a>. An empty label selector matches all {@link Pod}s.
     */
    private final String labelSelector;

    /**
     * Creates a {@link PodQuery}.
     *
     * @param apiServer
     *            An API server client.
     * @param namespace
     *            The namespace that the {@link Pod} listing will be limited to.
     * @param labelSelector
     *            Restricts the {@link Pod} listing to include only {@link Pod}s
     *            with matching labels. See <a href=
     *            "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     *            selectors</a>. An empty label selector matches all
     *            {@link Pod}s.
     */
    public PodQuery(ApiServerClient apiServer, String namespace, String labelSelector) {
        checkArgument(apiServer != null, "apiServer cannot be null");
        checkArgument(namespace != null, "namespace cannot be null");
        checkArgument(labelSelector != null, "labelSelector cannot be null");
        this.apiServer = apiServer;
        this.namespace = namespace;
        this.labelSelector = labelSelector;
    }

    @Override
    public PodList call() throws KubernetesApiException {
        try {
            PodList podList = JsonUtils.toObject(this.apiServer.get(listPodsPath(this.labelSelector)), PodList.class);
            return podList;
        } catch (Exception e) {
            throw new KubernetesApiException("failed to get pods: " + e.getMessage(), e);
        }
    }

    private String listPodsPath(String labelSelectorExpression) {
        try {
            String encodedSelector = URLEncoder.encode(labelSelectorExpression, Charsets.UTF_8.name());
            return MessageFormat.format(LIST_PODS_PATH, this.namespace, encodedSelector);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to produce URL path to list pods: " + e.getMessage(), e);
        }
    }
}
