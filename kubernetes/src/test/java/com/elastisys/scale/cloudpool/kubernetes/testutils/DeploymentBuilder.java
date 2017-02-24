package com.elastisys.scale.cloudpool.kubernetes.testutils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.elastisys.scale.cloudpool.kubernetes.types.Deployment;
import com.elastisys.scale.cloudpool.kubernetes.types.DeploymentSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.DeploymentStatus;
import com.elastisys.scale.cloudpool.kubernetes.types.LabelSelector;
import com.elastisys.scale.cloudpool.kubernetes.types.LabelSelectorRequirement;
import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.PodTemplateSpec;

/**
 * Builds {@link Deployment} instances to be used in testing.
 */
public class DeploymentBuilder {
    private String namespace = "my-ns";
    private String name = "my-nginx";
    private String podName = this.name;
    private int desiredReplicas = 1;
    private int numReplicas = 1;
    /**
     * The {@code spec.selector} used to select pods that belong to this
     * {@link Deployment}.
     */
    private LabelSelector selector = null;
    /**
     * the {@code spec.template.labels} which will be set on pods created by
     * this {@link Deployment}.
     */
    private Map<String, String> labels = new TreeMap<>();

    public Deployment build() {
        checkArgument(!this.labels.isEmpty(), "no pod labels given");
        Deployment deployment = new Deployment();
        deployment.kind = "Deployment";
        deployment.apiVersion = "extensions/v1beta1";

        deployment.metadata = new ObjectMeta();
        deployment.metadata.namespace = this.namespace;
        deployment.metadata.name = this.name;

        deployment.spec = new DeploymentSpec();
        if (this.selector != null) {
            deployment.spec.selector = this.selector;
        } else {
            // see https://kubernetes.io/docs/user-guide/deployments/#selector
            deployment.spec.selector = new LabelSelector();
            deployment.spec.selector.matchLabels = this.labels;
        }
        deployment.spec.replicas = this.desiredReplicas;
        deployment.spec.template = new PodTemplateSpec();
        deployment.spec.template.metadata = new ObjectMeta();
        deployment.spec.template.metadata.name = this.podName;
        deployment.spec.template.metadata.labels = this.labels;

        deployment.status = new DeploymentStatus();
        deployment.status.replicas = this.numReplicas;
        return deployment;
    }

    public static DeploymentBuilder create() {
        return new DeploymentBuilder();
    }

    public DeploymentBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeploymentBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a key-value pair to the {@code spec.selector.matchLabels} used to
     * select pods that belong to this {@link Deployment}.
     *
     * @param label
     * @param value
     * @return
     */
    public DeploymentBuilder addMatchLabel(String label, String value) {
        if (this.selector == null) {
            this.selector = new LabelSelector();
        }
        if (this.selector.matchLabels == null) {
            this.selector.matchLabels = new TreeMap<>();
        }
        this.selector.matchLabels.put(label, value);
        return this;
    }

    /**
     * Add a {@link LabelSelectorRequirement} to the
     * {@code spec.selector.matchExpressions} used to select pods that belong to
     * this {@link Deployment}.
     *
     * @param labelSelectorRequirement
     * @return
     */
    public DeploymentBuilder addMatchExpression(LabelSelectorRequirement labelSelectorRequirement) {
        if (this.selector == null) {
            this.selector = new LabelSelector();
        }
        if (this.selector.matchExpressions == null) {
            this.selector.matchExpressions = new ArrayList<>();
        }
        this.selector.matchExpressions.add(labelSelectorRequirement);
        return this;
    }

    /**
     * Add a label to the {@code spec.template.labels} which will be set on pods
     * created by this {@link Deployment}.
     *
     * @param label
     * @param value
     * @return
     */
    public DeploymentBuilder addTemplateLabel(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Sets {@code spec.replicas}, the desired number of replicas.
     *
     * @param desiredSize
     * @return
     */
    public DeploymentBuilder desiredReplicas(int desiredSize) {
        this.desiredReplicas = desiredSize;
        return this;
    }

    /**
     * Sets {@code status.replicas}, the actual number of pod replicas in this
     * {@link Deployment}.
     *
     * @param desiredSize
     * @return
     */
    public DeploymentBuilder replicas(int actualReplicas) {
        this.numReplicas = actualReplicas;
        return this;
    }

}