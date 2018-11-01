package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link ContextEntry} and its validation.
 */
public class TestContextEntry {

    /**
     * Exercise basic properties of UserEntry. A valid user should pass
     * validation and getters should return expected values.
     */
    @Test
    public void validContextEntry() {
        ContextEntry contextEntry = contextEntry("foo", TestContext.context("cluster", "user"));
        contextEntry.validate();

        assertThat(contextEntry.getName(), is("foo"));
        assertThat(contextEntry.getContext(), is(TestContext.context("cluster", "user")));
    }

    /**
     * One must specify a name for the context entry.
     */
    @Test
    public void missingName() {
        ContextEntry contextEntry = contextEntry(null, TestContext.context("cluster", "user"));
        try {
            contextEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("contexts: context entry missing name field"));
        }
    }

    /**
     * One must specify a {@link Context} for the {@link ContextEntry}.
     */
    @Test
    public void missingContext() {
        ContextEntry contextEntry = contextEntry("foo", null);
        try {
            contextEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("contexts: context entry 'foo' missing context field"));
        }
    }

    /**
     * {@link ContextEntry} validation should call through to {@link Context}'s
     * validate.
     */
    @Test
    public void validateCallThrough() {
        // context missing user name
        ContextEntry contextEntry = contextEntry("foo", TestContext.context("cluster", null));
        try {
            contextEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                    is("contexts: context entry 'foo': invalid context: context: missing user field"));
        }
    }

    public static ContextEntry contextEntry(String name, Context context) {
        ContextEntry contextEntry = new ContextEntry();
        contextEntry.name = name;
        contextEntry.context = context;
        return contextEntry;
    }
}
