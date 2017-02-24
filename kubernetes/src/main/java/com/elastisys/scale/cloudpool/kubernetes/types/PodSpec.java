package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#podspec-v1
 */
public class PodSpec {
    public Integer activeDeadlineSeconds;
    public List<JsonObject> containers;
    public String dnsPolicy;
    public Boolean hostIPC;
    public Boolean hostNetwork;
    public Boolean hostPID;
    public String hostname;
    public List<JsonObject> imagePullSecrets;
    public String nodeName;
    public Map<String, String> nodeSelector;
    public String restartPolicy;
    public JsonObject securityContext;
    public String serviceAccount;
    public String serviceAccountName;
    public String subdomain;
    public Integer terminationGracePeriodSeconds;
    public List<JsonObject> volumes;

    @Override
    public int hashCode() {
        return Objects.hash(this.activeDeadlineSeconds, this.containers, this.dnsPolicy, this.hostIPC, this.hostNetwork,
                this.hostPID, this.hostname, this.imagePullSecrets, this.nodeName, this.nodeSelector,
                this.restartPolicy, this.securityContext, this.serviceAccount, this.serviceAccountName, this.subdomain,
                this.terminationGracePeriodSeconds, this.volumes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodSpec) {
            PodSpec that = (PodSpec) obj;
            return Objects.equals(this.activeDeadlineSeconds, that.activeDeadlineSeconds) //
                    && Objects.equals(this.containers, that.containers) //
                    && Objects.equals(this.dnsPolicy, that.dnsPolicy) //
                    && Objects.equals(this.hostIPC, that.hostIPC) //
                    && Objects.equals(this.hostNetwork, that.hostNetwork) //
                    && Objects.equals(this.hostPID, that.hostPID) //
                    && Objects.equals(this.hostname, that.hostname) //
                    && Objects.equals(this.imagePullSecrets, that.imagePullSecrets) //
                    && Objects.equals(this.nodeName, that.nodeName) //
                    && Objects.equals(this.nodeSelector, that.nodeSelector) //
                    && Objects.equals(this.restartPolicy, that.restartPolicy) //
                    && Objects.equals(this.securityContext, that.securityContext) //
                    && Objects.equals(this.serviceAccount, that.serviceAccount) //
                    && Objects.equals(this.serviceAccountName, that.serviceAccountName) //
                    && Objects.equals(this.subdomain, that.subdomain) //
                    && Objects.equals(this.terminationGracePeriodSeconds, that.terminationGracePeriodSeconds) //
                    && Objects.equals(this.volumes, that.volumes);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
