package com.elastisys.scale.cloudpool.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the behavior of the {@link MembershipStatus} class.
 */
public class TestMembershipStatus {

    @Test
    public void testBasicSanity() {
        MembershipStatus activeEvictable = new MembershipStatus(true, true);
        assertThat(activeEvictable.isActive(), is(Boolean.TRUE));
        assertThat(activeEvictable.isEvictable(), is(Boolean.TRUE));

        MembershipStatus activeNotEvictable = new MembershipStatus(true, false);
        assertThat(activeNotEvictable.isActive(), is(Boolean.TRUE));
        assertThat(activeNotEvictable.isEvictable(), is(Boolean.FALSE));

        MembershipStatus inactiveEvictable = new MembershipStatus(false, true);
        assertThat(inactiveEvictable.isActive(), is(Boolean.FALSE));
        assertThat(inactiveEvictable.isEvictable(), is(Boolean.TRUE));

        MembershipStatus inactiveNotEvictable = new MembershipStatus(false, false);
        assertThat(inactiveNotEvictable.isActive(), is(Boolean.FALSE));
        assertThat(inactiveNotEvictable.isEvictable(), is(Boolean.FALSE));
    }

    @Test
    public void testEquality() {
        MembershipStatus activeEvictable = new MembershipStatus(true, true);
        MembershipStatus activeNotEvictable = new MembershipStatus(true, false);
        MembershipStatus inactiveEvictable = new MembershipStatus(false, true);
        MembershipStatus inactiveNotEvictable = new MembershipStatus(false, false);

        // test equality
        assertTrue(activeEvictable.equals(activeEvictable));
        assertTrue(activeNotEvictable.equals(activeNotEvictable));
        assertTrue(inactiveEvictable.equals(inactiveEvictable));
        assertTrue(inactiveNotEvictable.equals(inactiveNotEvictable));

        // test inequality
        assertFalse(activeEvictable.equals(activeNotEvictable));
        assertFalse(activeEvictable.equals(inactiveEvictable));
        assertFalse(activeEvictable.equals(inactiveNotEvictable));

        assertFalse(activeNotEvictable.equals(activeEvictable));
        assertFalse(activeNotEvictable.equals(inactiveEvictable));
        assertFalse(activeNotEvictable.equals(inactiveNotEvictable));

        assertFalse(inactiveEvictable.equals(activeEvictable));
        assertFalse(inactiveEvictable.equals(activeNotEvictable));
        assertFalse(inactiveEvictable.equals(inactiveNotEvictable));

        assertFalse(inactiveNotEvictable.equals(activeEvictable));
        assertFalse(inactiveNotEvictable.equals(activeNotEvictable));
        assertFalse(inactiveNotEvictable.equals(inactiveEvictable));
    }

    @Test
    public void testHashcode() {
        MembershipStatus activeEvictable = new MembershipStatus(true, true);
        MembershipStatus activeNotEvictable = new MembershipStatus(true, false);
        MembershipStatus inactiveEvictable = new MembershipStatus(false, true);
        MembershipStatus inactiveNotEvictable = new MembershipStatus(false, false);

        assertTrue(activeEvictable.hashCode() == new MembershipStatus(true, true).hashCode());
        assertTrue(activeNotEvictable.hashCode() == new MembershipStatus(true, false).hashCode());
        assertTrue(inactiveEvictable.hashCode() == new MembershipStatus(false, true).hashCode());
        assertTrue(inactiveNotEvictable.hashCode() == new MembershipStatus(false, false).hashCode());

        // test inequality
        assertFalse(activeEvictable.hashCode() == activeNotEvictable.hashCode());
        assertFalse(activeEvictable.hashCode() == inactiveEvictable.hashCode());
        assertFalse(activeEvictable.hashCode() == inactiveNotEvictable.hashCode());

        assertFalse(activeNotEvictable.hashCode() == activeEvictable.hashCode());
        assertFalse(activeNotEvictable.hashCode() == inactiveEvictable.hashCode());
        assertFalse(activeNotEvictable.hashCode() == inactiveNotEvictable.hashCode());

        assertFalse(inactiveEvictable.hashCode() == activeEvictable.hashCode());
        assertFalse(inactiveEvictable.hashCode() == activeNotEvictable.hashCode());
        assertFalse(inactiveEvictable.hashCode() == inactiveNotEvictable.hashCode());

        assertFalse(inactiveNotEvictable.hashCode() == activeEvictable.hashCode());
        assertFalse(inactiveNotEvictable.hashCode() == activeNotEvictable.hashCode());
        assertFalse(inactiveNotEvictable.hashCode() == inactiveEvictable.hashCode());
    }

    /**
     * Default {@link MembershipStatus} is active and evictable.
     */
    @Test
    public void testDefault() {
        assertThat(MembershipStatus.defaultStatus(), is(new MembershipStatus(true, true)));
    }

    /**
     * Blessed {@link MembershipStatus} is active and not evictable.
     */
    @Test
    public void testBlessed() {
        assertThat(MembershipStatus.blessed(), is(new MembershipStatus(true, false)));
    }

    /**
     * "Awaiting service" {@link MembershipStatus} is inactive and not evictable
     * (don't terminate to allow for troubleshooting).
     */
    @Test
    public void testAwaitingService() {
        assertThat(MembershipStatus.awaitingService(), is(new MembershipStatus(false, false)));
    }

}
