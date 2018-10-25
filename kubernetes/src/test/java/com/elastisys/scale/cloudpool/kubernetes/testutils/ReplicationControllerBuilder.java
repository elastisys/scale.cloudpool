package com.elastisys.scale.cloudpool.kubernetes.testutils;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Map;
import java.util.TreeMap;

import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.PodTemplateSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationController;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationControllerSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationControllerStatus;

/**
 * Builds {@link ReplicationController} instances to be used in testing.
 */
public class ReplicationControllerBuilder {
    private String namespace = "my-ns";
    private String name = "my-nginx";
    private String podName = this.name;
    private int desiredReplicas = 1;
    private int numReplicas = 1;
    /**
     * The {@code spec.selector} expression used to select pods that belong to
     * this {@link ReplicationController}.
     */
    private Map<String, String> selector = null;
    /**
     * the {@code spec.template.labels} which will be set on pods created by
     * this {@link ReplicationController}.
     */
    private Map<String, String> labels = new TreeMap<>();

    public ReplicationController build() {
        checkArgument(!this.labels.isEmpty(), "no pod labels given");
        ReplicationController rc = new ReplicationController();
        rc.kind = "ReplicationController";
        rc.apiVersion = "v1";

        rc.metadata = new ObjectMeta();
        rc.metadata.namespace = this.namespace;
        rc.metadata.name = this.name;

        rc.spec = new ReplicationControllerSpec();
        if (this.selector != null) {
            rc.spec.selector = this.selector;
        } else {
            // https://kubernetes.io/docs/user-guide/replication-controller/#pod-selector
            rc.spec.selector = this.labels;
        }
        rc.spec.replicas = this.desiredReplicas;
        rc.spec.template = new PodTemplateSpec();
        rc.spec.template.metadata = new ObjectMeta();
        rc.spec.template.metadata.name = this.podName;
        rc.spec.template.metadata.labels = this.labels;

        rc.status = new ReplicationControllerStatus();
        rc.status.replicas = this.numReplicas;
        return rc;
    }

    public static ReplicationControllerBuilder create() {
        return new ReplicationControllerBuilder();
    }

    public ReplicationControllerBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ReplicationControllerBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a label to the {@code spec.selector} expression used to select pods
     * that belong to this {@link ReplicationController}.
     *
     * @param label
     * @param value
     * @return
     */
    public ReplicationControllerBuilder addSelectorLabel(String label, String value) {
        if (this.selector == null) {
            this.selector = new TreeMap<>();
        }
        this.selector.put(label, value);
        return this;
    }

    /**
     * Add a label to the {@code spec.template.labels} which will be set on pods
     * created by this {@link ReplicationController}.
     *
     * @param label
     * @param value
     * @return
     */
    public ReplicationControllerBuilder addTemplateLabel(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Sets {@code spec.replicas}, the desired number of replicas.
     *
     * @param desiredSize
     * @return
     */
    public ReplicationControllerBuilder desiredReplicas(int desiredSize) {
        this.desiredReplicas = desiredSize;
        return this;
    }

    /**
     * Sets {@code status.replicas}, the actual number of pod replicas in this
     * {@link ReplicationController}.
     *
     * @param desiredSize
     * @return
     */
    public ReplicationControllerBuilder replicas(int actualReplicas) {
        this.numReplicas = actualReplicas;
        return this;
    }

}