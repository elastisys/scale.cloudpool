package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Triple;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A Java representation of a kubeconfig YAML file. It is used to declare
 * authentication credentials for a set of clusters (and users). Given that a
 * {@code current-context} has been specified, a kubeconfig file can be used as
 * a source of connection/authentication settings for a
 * {@link KubernetesCloudPool}.
 *
 * @see https://godoc.org/k8s.io/client-go/tools/clientcmd/api#Config
 * @see https://github.com/eBay/Kubernetes/blob/master/docs/user-guide/kubeconfig-file.md
 */
public class KubeConfig {

    public static final String DEFAULT_API_VERSION = "v1";
    public static final String DEFAULT_KIND = "Config";

    /** Version metadata intended for a parser. May be null. Default: "v1". */
    @JsonProperty("apiVersion")
    String apiVersion;

    /**
     * Schema metadata intended for a parser. May be null. Default: "Config".
     */
    @JsonProperty("kind")
    String kind;

    /**
     * The clusters defined in this {@link KubeConfig}. A cluster contains
     * endpoint data for a kubernetes cluster. This includes the fully qualified
     * url for the kubernetes apiserver, as well as the cluster's certificate
     * authority or insecure-skip-tls-verify: true, if the cluster's serving
     * certificate is not signed by a system trusted certificate authority.
     */
    @JsonProperty("clusters")
    List<ClusterEntry> clusters;

    /**
     * The users defined in this {@link KubeConfig}. A user defines client
     * credentials for authenticating to a kubernetes cluster.
     */
    @JsonProperty("users")
    List<UserEntry> users;

    /**
     * The contexts defined in this {@link KubeConfig}. A context defines a
     * named {@code {cluster,user,namespace}} tuple which is used to send
     * requests to the specified cluster using the provided authentication info
     * and namespace.
     */
    @JsonProperty("contexts")
    List<ContextEntry> contexts;

    /**
     * The currently set context. Essentially the nickname or 'key' for the
     * {@code {cluster,user,namespace}} tuple to use when extracting
     * connection/authentication data from this {@link KubeConfig}.
     *
     */
    @JsonProperty("current-context")
    String currentContext;

    /** Optional (and currently unused) kubectl preferences. May be null. */
    @JsonProperty("preferences")
    Map<String, Object> preferences;

    /**
     * Loads a {@link KubeConfig} from a YAML-formatted file.
     *
     * @param kubeConfigFilehttps://github.com/eBay/Kubernetes/blob/master/docs/user-guide/kubeconfig-file.md
     *            A kubeconfig file to be loaded from the file system.
     * @return
     * @throws IOException
     */
    public static KubeConfig load(File kubeConfigFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        KubeConfig kubeConfig = mapper.readValue(kubeConfigFile, KubeConfig.class);
        return kubeConfig;
    }

    /**
     * Version metadata intended for a parser. Typically "v1".
     *
     * @return
     */
    public String getApiVersion() {
        return Optional.ofNullable(this.apiVersion).orElse(DEFAULT_API_VERSION);
    }

    /**
     * Schema metadata intended for a parser. Typically "Config".
     *
     * @return
     */
    public String getKind() {
        return Optional.ofNullable(this.kind).orElse(DEFAULT_KIND);
    }

    /**
     * The clusters defined in this {@link KubeConfig}. A cluster contains
     * endpoint data for a kubernetes cluster. This includes the fully qualified
     * url for the kubernetes apiserver, as well as the cluster's certificate
     * authority or insecure-skip-tls-verify: true, if the cluster's serving
     * certificate is not signed by a system trusted certificate authority.
     *
     * @return
     */
    public List<ClusterEntry> getClusters() {
        return Optional.ofNullable(this.clusters).orElse(Collections.emptyList());
    }

    /**
     * The users defined in this {@link KubeConfig}. A user defines client
     * credentials for authenticating to a kubernetes cluster.
     *
     * @return
     */
    public List<UserEntry> getUsers() {
        return Optional.ofNullable(this.users).orElse(Collections.emptyList());
    }

    /**
     * The contexts defined in this {@link KubeConfig}. A context defines a
     * named {@code {cluster,user,namespace}} tuple which is used to send
     * requests to the specified cluster using the provided authentication info
     * and namespace.
     *
     * @return
     */
    public List<ContextEntry> getContexts() {
        return Optional.ofNullable(this.contexts).orElse(Collections.emptyList());
    }

    /**
     * The currently set context. Essentially the nickname or 'key' for the
     * {@code {cluster,user,namespace}} tuple to use when extracting
     * connection/authentication data from this {@link KubeConfig}.
     *
     * @return
     */
    public String getCurrentContext() {
        return this.currentContext;
    }

    /**
     * Loads the current context as a {@code cluster,user,namespace} tuple.
     *
     * @return
     */
    public Triple<Cluster, User, String> loadCurrentContext() {
        // note: assumed to be validated and that there in fact *is* a current
        // context set
        Optional<ContextEntry> ctx = getContexts().stream().filter(c -> c.getName().equals(this.currentContext))
                .findFirst();
        checkArgument(ctx.isPresent(), "could not find a matching context for current-context: %s",
                this.currentContext);
        Context context = ctx.get().getContext();

        Cluster cluster = loadCurrentCluster(context.getCluster());
        User user = loadCurrentUser(context.getUser());
        return Triple.of(cluster, user, context.getNamespace());
    }

    private Cluster loadCurrentCluster(String clusterName) {
        Optional<ClusterEntry> cluster = getClusters().stream().filter(c -> c.getName().equals(clusterName))
                .findFirst();
        checkArgument(cluster.isPresent(), "could not find cluster referenced by current-context: %s", clusterName);
        return cluster.get().getCluster();
    }

    private User loadCurrentUser(String userName) {
        Optional<UserEntry> user = getUsers().stream().filter(u -> u.getName().equals(userName)).findFirst();
        checkArgument(user.isPresent(), "could not find user referenced by current-context: %s", userName);
        return user.get().getUser();

    }

    /**
     * Optional (and currently unused) kubectl preferences.
     *
     * @return
     */
    public Map<String, Object> getPreferences() {
        return Optional.ofNullable(this.preferences).orElse(Collections.emptyMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiVersion, this.kind, this.clusters, this.users, this.contexts, this.currentContext,
                this.preferences);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KubeConfig) {
            KubeConfig that = (KubeConfig) obj;
            return Objects.equals(getApiVersion(), that.getApiVersion()) //
                    && Objects.equals(getKind(), that.getKind()) //
                    && Objects.equals(getClusters(), that.getClusters()) //
                    && Objects.equals(getUsers(), that.getUsers()) //
                    && Objects.equals(getContexts(), that.getContexts()) //
                    && Objects.equals(getCurrentContext(), that.getCurrentContext()) //
                    && Objects.equals(getPreferences(), that.getPreferences());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.clusters != null && !this.clusters.isEmpty(), "kubeconfig: missing cluster entries");
        checkArgument(this.users != null && !this.users.isEmpty(), "kubeconfig: missing user entries");
        checkArgument(this.contexts != null && !this.contexts.isEmpty(), "kubeconfig: missing context entries");
        checkArgument(this.currentContext != null, "kubeconfig: missing current-context");

        // pass through validation
        try {
            this.clusters.forEach(c -> c.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException("kubeconfig: " + e.getMessage(), e);
        }
        try {
            this.users.forEach(u -> u.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException("kubeconfig: " + e.getMessage(), e);
        }
        try {
            this.contexts.forEach(c -> c.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException("kubeconfig: " + e.getMessage(), e);
        }

        // verify that contexts reference existing clusters/users
        this.contexts.stream().forEach(ctx -> {
            String clusterName = ctx.getContext().getCluster();
            String userName = ctx.getContext().getUser();

            if (!clusterExists(clusterName)) {
                throw new IllegalArgumentException(String.format(
                        "kubeconfig: context '%s' references unknown cluster '%s'", ctx.getName(), clusterName));
            }

            if (!userExists(userName)) {
                throw new IllegalArgumentException(String
                        .format("kubeconfig: context '%s' references unknown user '%s'", ctx.getName(), userName));
            }
        });

        // current context must be possible to load
        try {
            loadCurrentContext();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("kubeconfig: failed to load current-context: %s", e.getMessage()), e);
        }
    }

    private boolean clusterExists(String clusterName) {
        return getClusters().stream().anyMatch(c -> c.getName().equals(clusterName));
    }

    private boolean userExists(String userName) {
        return getUsers().stream().anyMatch(u -> u.getName().equals(userName));
    }
}
