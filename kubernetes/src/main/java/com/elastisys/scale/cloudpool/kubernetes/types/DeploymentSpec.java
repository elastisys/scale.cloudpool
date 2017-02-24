package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#deploymentspec-v1beta1
 */
public class DeploymentSpec {
    public Integer minReadySeconds;
    public Boolean paused;
    public Integer progressDeadlineSeconds;
    public Integer replicas;
    public Integer revisionHistoryLimit;
    public RollbackConfig rollbackTo;
    public LabelSelector selector;
    public DeploymentStrategy strategy;
    public PodTemplateSpec template;

    @Override
    public int hashCode() {
        return Objects.hash(this.minReadySeconds, this.paused, this.progressDeadlineSeconds, this.replicas,
                this.revisionHistoryLimit, this.rollbackTo, this.selector, this.strategy, this.template);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeploymentSpec) {
            DeploymentSpec that = (DeploymentSpec) obj;
            return Objects.equals(this.minReadySeconds, that.minReadySeconds) //
                    && Objects.equals(this.paused, that.paused) //
                    && Objects.equals(this.progressDeadlineSeconds, that.progressDeadlineSeconds) //
                    && Objects.equals(this.replicas, that.replicas) //
                    && Objects.equals(this.revisionHistoryLimit, that.revisionHistoryLimit) //
                    && Objects.equals(this.rollbackTo, that.rollbackTo) //
                    && Objects.equals(this.selector, that.selector) //
                    && Objects.equals(this.strategy, that.strategy) //
                    && Objects.equals(this.template, that.template);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
