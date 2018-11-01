package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link UserEntry} class and its validation.
 */
public class TestUserEntry {

    /**
     * Exercise basic properties of UserEntry. A valid user should pass
     * validation and getters should return expected values.
     */
    @Test
    public void validUserEntry() {
        UserEntry userEntry = userEntry("foo",
                TestUser.certAuthUserByPath(TestUser.CLIENT_CERT_PATH, TestUser.CLIENT_KEY_PATH));
        userEntry.validate();

        assertThat(userEntry.getName(), is("foo"));
        assertThat(userEntry.getUser(),
                is(TestUser.certAuthUserByPath(TestUser.CLIENT_CERT_PATH, TestUser.CLIENT_KEY_PATH)));
    }

    /**
     * One must specify a name for the user entry.
     */
    @Test
    public void missingName() {
        UserEntry userEntry = userEntry(null,
                TestUser.certAuthUserByPath(TestUser.CLIENT_CERT_PATH, TestUser.CLIENT_KEY_PATH));
        try {
            userEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("users: user entry missing name field"));
        }
    }

    /**
     * One must specify a {@link User} for the {@link UserEntry}.
     */
    @Test
    public void missingUser() {
        UserEntry userEntry = userEntry("foo", null);
        try {
            userEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("users: user entry 'foo' missing user field"));
        }
    }

    /**
     * {@link UserEntry} validation should call through to {@link User}'s
     * validate.
     */
    @Test
    public void validateCallThrough() {
        User invalidUser = TestUser.certAuthUserByPath("bad/path.pem", TestUser.CLIENT_KEY_PATH);
        UserEntry userEntry = userEntry("foo", invalidUser);
        try {
            userEntry.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("users: invalid user 'foo': user: failed to load cert"));
        }
    }

    public static UserEntry userEntry(String name, User user) {
        UserEntry userEntry = new UserEntry();
        userEntry.name = name;
        userEntry.user = user;
        return userEntry;
    }
}
