package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link Context} and its validation.
 */
public class TestContext {

    /**
     * Verify that a valid context passes validation and getters return expected
     * values.
     */
    @Test
    public void validContext() {
        Context context = context("cluster", "user", "kube-system");
        context.validate();

        assertThat(context.getCluster(), is("cluster"));
        assertThat(context.getUser(), is("user"));
        assertThat(context.getNamespace(), is("kube-system"));
    }

    /**
     * When no namespace is given, it should default to 'default'.
     */
    @Test
    public void validContextWithDefaultNamespace() {
        Context context = context("cluster", "user");
        context.validate();

        assertThat(context.getCluster(), is("cluster"));
        assertThat(context.getUser(), is("user"));

        // default when no namespace is given
        assertThat(context.getNamespace(), is(Context.DEFAULT_NS));
    }

    /**
     * A context must specify a cluster.
     */
    @Test
    public void missingCluster() {
        try {
            context(null, "user").validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("context: missing cluster field"));
        }
    }

    /**
     * A context must specify a user.
     */
    @Test
    public void missingUser() {
        try {
            context("cluster", null).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("context: missing user field"));
        }
    }

    public static Context context(String clusterName, String userName) {
        Context context = new Context();
        context.cluster = clusterName;
        context.user = userName;
        context.namespace = null;
        return context;
    }

    public static Context context(String clusterName, String userName, String namespace) {
        Context context = new Context();
        context.cluster = clusterName;
        context.user = userName;
        context.namespace = namespace;
        return context;
    }
}
