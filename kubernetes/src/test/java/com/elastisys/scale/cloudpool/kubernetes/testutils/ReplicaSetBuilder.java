package com.elastisys.scale.cloudpool.kubernetes.testutils;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.elastisys.scale.cloudpool.kubernetes.types.LabelSelector;
import com.elastisys.scale.cloudpool.kubernetes.types.LabelSelectorRequirement;
import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.PodTemplateSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSet;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSetSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSetStatus;

/**
 * Builds {@link ReplicaSet} instances to be used in testing.
 */
public class ReplicaSetBuilder {
    private String namespace = "my-ns";
    private String name = "my-nginx";
    private String podName = this.name;
    private int desiredReplicas = 1;
    private int numReplicas = 1;
    /**
     * The {@code spec.selector} used to select pods that belong to this
     * {@link ReplicaSet}.
     */
    private LabelSelector selector = null;
    /**
     * the {@code spec.template.labels} which will be set on pods created by
     * this {@link ReplicaSet}.
     */
    private Map<String, String> labels = new TreeMap<>();

    public ReplicaSet build() {
        checkArgument(!this.labels.isEmpty(), "no pod labels given");
        ReplicaSet rs = new ReplicaSet();
        rs.kind = "ReplicaSet";
        rs.apiVersion = "extensions/v1beta1";

        rs.metadata = new ObjectMeta();
        rs.metadata.namespace = this.namespace;
        rs.metadata.name = this.name;

        rs.spec = new ReplicaSetSpec();
        if (this.selector != null) {
            rs.spec.selector = this.selector;
        } else {
            // see https://kubernetes.io/docs/user-guide/replicasets/
            rs.spec.selector = new LabelSelector();
            rs.spec.selector.matchLabels = this.labels;
        }
        rs.spec.replicas = this.desiredReplicas;
        rs.spec.template = new PodTemplateSpec();
        rs.spec.template.metadata = new ObjectMeta();
        rs.spec.template.metadata.name = this.podName;
        rs.spec.template.metadata.labels = this.labels;

        rs.status = new ReplicaSetStatus();
        rs.status.replicas = this.numReplicas;
        return rs;
    }

    public static ReplicaSetBuilder create() {
        return new ReplicaSetBuilder();
    }

    public ReplicaSetBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ReplicaSetBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a key-value pair to the {@code spec.selector.matchLabels} used to
     * select pods that belong to this {@link ReplicaSet}.
     *
     * @param label
     * @param value
     * @return
     */
    public ReplicaSetBuilder addMatchLabel(String label, String value) {
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
     * this {@link ReplicaSet}.
     *
     * @param labelSelectorRequirement
     * @return
     */
    public ReplicaSetBuilder addMatchExpression(LabelSelectorRequirement labelSelectorRequirement) {
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
     * created by this {@link ReplicaSet}.
     *
     * @param label
     * @param value
     * @return
     */
    public ReplicaSetBuilder addTemplateLabel(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Sets {@code spec.replicas}, the desired number of replicas.
     *
     * @param desiredSize
     * @return
     */
    public ReplicaSetBuilder desiredReplicas(int desiredSize) {
        this.desiredReplicas = desiredSize;
        return this;
    }

    /**
     * Sets {@code status.replicas}, the actual number of pod replicas in this
     * {@link ReplicaSet}.
     *
     * @param desiredSize
     * @return
     */
    public ReplicaSetBuilder replicas(int actualReplicas) {
        this.numReplicas = actualReplicas;
        return this;
    }

}