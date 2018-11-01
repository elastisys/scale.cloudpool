package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestCluster.CA_CERT_PATH;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestCluster.clusterCaCertPath;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestClusterEntry.clusterEntry;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestContext.context;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestContextEntry.contextEntry;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUser.CLIENT_CERT_PATH;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUser.CLIENT_KEY_PATH;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUser.certAuthUserByPath;
import static com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.TestUserEntry.userEntry;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;

/**
 * Exercise {@link KubeConfig} and its validation.
 */
public class TestKubeConfig {

    /**
     * A valid kubeconfig should pass validation and its getters should return
     * expected values.
     */
    @Test
    public void validKubeConfig() {
        String currentContext = "cluster1-context";
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                        clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                        userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1")),
                        contextEntry("cluster2-context", context("cluster2", "user2"))),
                currentContext);

        kubeConfig.validate();
        assertThat(kubeConfig.getClusters(), is(clusterEntries( //
                clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH)))));
        assertThat(kubeConfig.getUsers(), is(userEntries( //
                userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)))));
        assertThat(kubeConfig.getContexts(), is(contextEntries(//
                contextEntry("cluster1-context", context("cluster1", "user1")),
                contextEntry("cluster2-context", context("cluster2", "user2")))));
        assertThat(kubeConfig.getCurrentContext(), is(currentContext));

        // defaults
        assertThat(kubeConfig.getApiVersion(), is(KubeConfig.DEFAULT_API_VERSION));
        assertThat(kubeConfig.getKind(), is(KubeConfig.DEFAULT_KIND));
    }

    /**
     * At least one cluster must be specified in kubeconfig for it to be useful
     * as an auth source for the {@link KubernetesCloudPool}.
     */
    @Test
    public void missingClusters() {
        List<ClusterEntry> nullClusterEntries = null;
        KubeConfig kubeConfig = kubeConfig(//
                nullClusterEntries, //
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                        userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1")),
                        contextEntry("cluster2-context", context("cluster2", "user2"))),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("kubeconfig: missing cluster entries"));
        }
    }

    /**
     * At least one user must be specified in kubeconfig for it to be useful as
     * an auth source for the {@link KubernetesCloudPool}.
     */
    @Test
    public void missingUsers() {
        List<UserEntry> nullUserEntries = null;
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                        clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH))), //
                nullUserEntries, //
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1")),
                        contextEntry("cluster2-context", context("cluster2", "user2"))),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("kubeconfig: missing user entries"));
        }
    }

    /**
     * At least one context must be specified in kubeconfig for it to be useful
     * as an auth source for the {@link KubernetesCloudPool}.
     */
    @Test
    public void missingContext() {
        List<ContextEntry> nullContextEntries = null;
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                        clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                        userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))), //
                nullContextEntries, //
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("kubeconfig: missing context entries"));
        }
    }

    /**
     * A current context must be specified in kubeconfig for it to be useful as
     * an auth source for the {@link KubernetesCloudPool}.
     */
    @Test
    public void missingCurrentContext() {
        String nullCurrentContext = null;
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                        clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                        userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))), //
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1")),
                        contextEntry("cluster2-context", context("cluster2", "user2"))), //
                nullCurrentContext);

        try {
            kubeConfig.validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("kubeconfig: missing current-context"));
        }
    }

    /**
     * Validation should pass-through to clusters. An illegal cluster definition
     * should fail entire kubeconfig validation.
     */
    @Test
    public void onIllegalClusterDefinition() {
        ClusterEntry illegalCluster = clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", "bad/ca.pem"));
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries(illegalCluster), //
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))), //
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1"))),
                "cluster1-context");
        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    "kubeconfig: clusters: cluster entry 'cluster1': cluster: failed to load ca cert: java.io.FileNotFoundException: bad/ca.pem (No such file or directory)"));
        }
    }

    /**
     * Validation should pass-through to users. An illegal user definition
     * should fail entire kubeconfig validation.
     */
    @Test
    public void onIllegalUserDefinition() {
        UserEntry illegalUser = userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, "bad/key.pem"));
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH))),
                userEntries( //
                        illegalUser),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1"))),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    "kubeconfig: users: invalid user 'user1': user: failed to load key: java.io.FileNotFoundException: bad/key.pem (No such file or directory)"));
        }
    }

    /**
     * Validation should pass-through to contexts. An illegal context definition
     * should fail entire kubeconfig validation.
     */
    @Test
    public void onIllegalContextDefinition() {
        ContextEntry illegalContext = contextEntry("cluster1-context", context(null, "user1"));
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        illegalContext),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    "kubeconfig: contexts: context entry 'cluster1-context': invalid context: context: missing cluster field"));
        }
    }

    /**
     * A context must reference a cluster in the kubeconfig.
     */
    @Test
    public void contextReferencesNonExistingCluster() {
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster-X", "user1"))),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                    is("kubeconfig: context 'cluster1-context' references unknown cluster 'cluster-X'"));
        }
    }

    /**
     * A context must reference a user in the kubeconfig.
     */
    @Test
    public void contextReferencesNonExistingUser() {
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user-X"))),
                "cluster1-context");

        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("kubeconfig: context 'cluster1-context' references unknown user 'user-X'"));
        }
    }

    /**
     * current-context must must reference a context in the kubeconfig.
     */
    @Test
    public void currentContextReferencesNonExistingContext() {
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1"))),
                "clusterX-context");

        try {
            kubeConfig.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    "kubeconfig: failed to load current-context: could not find a matching context for current-context: clusterX-context"));
        }
    }

    /**
     * {@link KubeConfig#loadCurrentContext()} should load the currently set
     * context.
     */
    @Test
    public void loadCurrentContext() {
        String currentContext = "cluster1-context";
        KubeConfig kubeConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("cluster1", clusterCaCertPath("https://apiserver1", CA_CERT_PATH)),
                        clusterEntry("cluster2", clusterCaCertPath("https://apiserver2", CA_CERT_PATH))),
                userEntries( //
                        userEntry("user1", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)),
                        userEntry("user2", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("cluster1-context", context("cluster1", "user1")),
                        contextEntry("cluster2-context", context("cluster2", "user2"))),
                currentContext);

        Triple<Cluster, User, String> context = kubeConfig.loadCurrentContext();
        assertThat(context.getLeft(), is(clusterCaCertPath("https://apiserver1", CA_CERT_PATH)));
        assertThat(context.getMiddle(), is(certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH)));
        assertThat(context.getRight(), is("default"));
    }

    /**
     * {@link KubeConfig#load(File)} should be able to load a kubeconfig file
     * from the file system.
     */
    @Test
    public void loadFromFile() throws IOException {
        KubeConfig expectedConfig = kubeConfig(//
                clusterEntries( //
                        clusterEntry("test-kube", clusterCaCertPath("https://192.168.99.104:8443", CA_CERT_PATH))),
                userEntries( //
                        userEntry("admin", certAuthUserByPath(CLIENT_CERT_PATH, CLIENT_KEY_PATH))),
                contextEntries(//
                        contextEntry("test-kube-context", context("test-kube", "admin"))),
                "test-kube-context");

        KubeConfig loadedKubeConfig = KubeConfig.load(new File("src/test/resources/kubeconfig/kubeconfig.yaml"));
        assertThat(loadedKubeConfig, is(expectedConfig));
    }

    public static KubeConfig kubeConfig(List<ClusterEntry> clusters, List<UserEntry> users, List<ContextEntry> contexts,
            String currentContext) {
        KubeConfig kubeConfig = new KubeConfig();
        kubeConfig.clusters = clusters;
        kubeConfig.users = users;
        kubeConfig.contexts = contexts;
        kubeConfig.currentContext = currentContext;
        return kubeConfig;
    }

    public static List<ClusterEntry> clusterEntries(ClusterEntry... entries) {
        return Arrays.asList(entries);
    }

    public static List<UserEntry> userEntries(UserEntry... entries) {
        return Arrays.asList(entries);
    }

    public static List<ContextEntry> contextEntries(ContextEntry... entries) {
        return Arrays.asList(entries);
    }
}
