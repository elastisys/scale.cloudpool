package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.makeMachine;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.makePool;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.nowOffset;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.NEWEST;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.OLDEST;
import static com.elastisys.scale.commons.util.time.UtcTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link ResizePlanner}.
 */
public class TestResizePlanner {

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2014-03-26T12:00.00Z"));
    }

    /**
     * Verifies that {@link ResizePlanner#getActiveSize()} recognizes machines
     * in active states (REQUESTED, PENDING, RUNNING).
     */
    @Test
    public void testGetActiveSize() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);

        // pool size: 0
        ResizePlanner planner = new ResizePlanner(makePool(), OLDEST);
        assertThat(planner.getActiveSize(), is(0));

        // pool size: 2 (1 PENDING, 1 RUNNING)
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running));
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(2));

        // allocated: 3 (1 REQUESTED, 1 PENDING, 1 RUNNING)
        pool = makePool(UtcTime.now(), asList(requested, pending, running));
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(3));
    }

    /**
     * Exercises the {@link ResizePlanner#getActiveSize()} when there are
     * machine in terminal states in the pool.
     */
    @Test
    public void testGetActiveSizeWithMachinesInTerminalState() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        Machine terminating = makeMachine(4, null, MachineState.TERMINATING);
        Machine terminated = makeMachine(5, null, MachineState.TERMINATED);
        Machine rejected = makeMachine(6, null, MachineState.REJECTED);

        // only three machines in a non-terminal state
        MachinePool pool = makePool(UtcTime.now(),
                asList(pending, running, requested, terminated, terminating, rejected));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(3));
    }

    /**
     * Active size should not include pool members that are marked with an
     * inactive {@link MembershipStatus}.
     */
    @Test
    public void testGetActiveSizeWithInactiveMachines() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        Machine blessed = makeMachine(3, null, MachineState.REQUESTED, MembershipStatus.blessed());
        Machine inactive1 = makeMachine(4, nowOffset(-7200), MachineState.RUNNING, MembershipStatus.awaitingService());
        Machine inactive2 = makeMachine(5, nowOffset(-7800), MachineState.RUNNING, new MembershipStatus(false, true));

        // allocated: 6, inactive: 2
        MachinePool pool = makePool(UtcTime.now(), asList(requested, pending, running, blessed, inactive1, inactive2));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(4));
    }

    /**
     * Test scale-ups and verify that the correct number of additional machines
     * to request are suggested.
     */
    @Test
    public void scaleUp() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        // should not be considered as it is not active
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // empty machine pool
        ResizePlanner planner = new ResizePlanner(makePool(), OLDEST);
        assertThat(planner.getActiveSize(), is(0));
        // 0 -> 1
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToTerminate().size(), is(0));
        // 0 -> 2
        plan = planner.calculateResizePlan(2);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(2));
        assertThat(plan.getToTerminate().size(), is(0));

        // pool size: 3 (allocated: 4, inactive: 1)
        MachinePool pool = makePool(now(), asList(requested, pending, running, outOfService));
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(3));
        // 3 -> 4
        plan = planner.calculateResizePlan(4);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToTerminate().size(), is(0));
        // 3 -> 5
        plan = planner.calculateResizePlan(5);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(2));
        assertThat(plan.getToTerminate().size(), is(0));
    }

    /**
     * Test calculating resize plans when the desired size equals the active
     * pool size.
     */
    @Test
    public void testStayPutRequest() {
        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 0 (allocated: 1, inactive: 1)
        MachinePool pool = makePool(UtcTime.now(), asList(outOfService));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(0));
        // 0 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

        pool = makePool(UtcTime.now(), asList(requested, pending));
        // pool size: 2
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(2));
        // 2 -> 2
        plan = planner.calculateResizePlan(2);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

        // pool size: 2 (allocated: 3, inactive: 1)
        pool = makePool(UtcTime.now(), asList(running, pending, outOfService));
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(2));
        // 2 -> 2
        plan = planner.calculateResizePlan(2);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());
    }

    /**
     * Verify that scale-down actions are properly suggested.
     */
    @Test
    public void scaleDown() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-900), MachineState.RUNNING);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 1 (allocated: 2, inactive: 1)
        MachinePool pool = makePool(UtcTime.now(), asList(pending, outOfService));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(1));
        // 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToTerminate().size(), is(1));

        // pool size: 2 (allocated: 3, inactive: 1)
        pool = makePool(UtcTime.now(), asList(pending, running, outOfService));
        planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(2));
        // 2 -> 1
        plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate().size(), is(1));
        // 2 -> 0
        plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate().size(), is(2));
    }

    /**
     * On scale-down, verify that machines in REQUESTED state are selected for
     * termination before PENDING and RUNNING machines.
     */
    @Test
    public void scaleDownWithMachinesInRequestedState() {
        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested1 = makeMachine(3, null, MachineState.REQUESTED);
        Machine requested2 = makeMachine(4, null, MachineState.REQUESTED);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 4 (allocated: 5, inactive: 1)
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running, requested1, requested2, outOfService));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(4));
        // 4 -> 3
        ResizePlan plan = planner.calculateResizePlan(3);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToTerminate().size(), is(1));
        assertThat(plan.getToTerminate(), is(asList(requested1)));

        // 4 -> 2
        plan = planner.calculateResizePlan(2);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToTerminate().size(), is(2));
        assertThat(plan.getToTerminate(), is(asList(requested1, requested2)));
    }

    /**
     * Plan a scale-down when there are machines in the pool in terminal states
     * and verify that machines in a terminal state are never selected for
     * termination.
     */
    @Test
    public void scaleDownWithMachinesInTerminalStates() {
        Machine rejected = makeMachine(1, nowOffset(-3600), MachineState.REJECTED);
        Machine terminating = makeMachine(2, nowOffset(-3400), MachineState.TERMINATING);
        Machine terminated = makeMachine(3, nowOffset(-3200), MachineState.TERMINATED);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        // should not be counted as an evictable machine
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 1 (allocated: 2, inactive: 1)
        MachinePool pool = makePool(UtcTime.now(), asList(rejected, terminating, terminated, running, outOfService));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(1));
        // 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        // only one candidate can be selected for termination
        assertThat(plan.getToTerminate().size(), is(1));
        assertThat(plan.getToTerminate(), is(asList(running)));
    }

    /**
     * Plan a scale-down when the victim candidate set is empty (all marked for
     * termination or not evictable).
     */
    @Test
    public void scaleDownWithEmptyVictimCandidateSet() {
        Machine terminated = makeMachine(3, null, MachineState.TERMINATED);
        Machine blessed = makeMachine(4, nowOffset(-7200), MachineState.RUNNING, MembershipStatus.blessed());

        MachinePool pool = makePool(UtcTime.now(), asList(terminated, blessed));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(1));
        // 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        // all machines either in terminal state or not evictable => no further
        // scale-down possible
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());
    }

    /**
     * Verify that the specified {@link VictimSelectionPolicy} is honored when a
     * scale-down is planned.
     */
    @Test
    public void verifyThatVictimSelectionPolicyIsHonored() {
        FrozenTime.setFixed(UtcTime.parse("2012-03-26T12:50:00Z"));
        Machine machine1 = makeMachine(1, UtcTime.parse("2012-03-26T10:45:00Z"));
        Machine machine2 = makeMachine(2, UtcTime.parse("2012-03-26T11:25:00Z"));
        Machine machine3 = makeMachine(3, UtcTime.parse("2012-03-26T12:34:00Z"));
        MachinePool pool = makePool(UtcTime.now(), asList(machine1, machine2, machine3));

        // victim selection policy: oldest instance
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.calculateResizePlan(2).getToTerminate(), is(asList(machine1)));
        assertThat(planner.calculateResizePlan(1).getToTerminate(), is(asList(machine1, machine2)));
        assertThat(planner.calculateResizePlan(0).getToTerminate(), is(asList(machine1, machine2, machine3)));

        // victim selection policy: newest instance
        planner = new ResizePlanner(pool, NEWEST);
        assertThat(planner.calculateResizePlan(2).getToTerminate(), is(asList(machine3)));
        assertThat(planner.calculateResizePlan(1).getToTerminate(), is(asList(machine3, machine2)));
        assertThat(planner.calculateResizePlan(0).getToTerminate(), is(asList(machine3, machine2, machine1)));
    }

    /**
     * Any machine in the pool marked with a {@link MembershipStatus} of
     * inactive, should be replaced.
     */
    @Test
    public void verifyThatInactiveMachinesAreReplaced() {
        Machine inactive = makeMachine(1, nowOffset(-1800), MachineState.RUNNING, MembershipStatus.awaitingService());

        MachinePool pool = makePool(UtcTime.now(), asList(inactive));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);

        // inactive machine should not be counted
        assertThat(planner.getActiveSize(), is(0));
        // replacement machines should be fired up for inactive machines
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(1));
    }

    /**
     * Make sure that blessed members (with a {@link MembershipStatus} that is
     * non-evictable) are never selected for termination.
     */
    @Test
    public void scaleDownWithNonEvictablePoolMembers() {
        Machine blessed = makeMachine(1, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.blessed());
        Machine youngerMachine = makeMachine(2, nowOffset(-1800));

        MachinePool pool = makePool(UtcTime.now(), asList(blessed, youngerMachine));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(2));

        // scale down by one: 2 -> 1
        // make sure the younger machine gets terminated despite using
        // OLDEST_INSTANCE victim selection policy, since the blessed instance
        // is not evictable
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate(), is(asList(youngerMachine)));

        // scale down by two: not entirely possible, since there aren't enough
        // evictable candidates
        plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate(), is(asList(youngerMachine)));
    }

    /**
     * Test the situation where we cannot scale down since there aren't enough
     * evictable candidates
     */
    @Test
    public void scaleDownWithTooFewEvictableCandidates() {
        Machine notEvictable1 = makeMachine(1, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.blessed());
        Machine notEvictable2 = makeMachine(2, nowOffset(-3600), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        MachinePool pool = makePool(UtcTime.now(), asList(notEvictable1, notEvictable2));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(1));

        // scale down by one: 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        // not possible, since there aren't enough evictable candidates
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());
    }

    /**
     * Any disposable (with inactive, evictable {@link MembershipStatus})
     * machines should be marked for termination (in order to be replaced).
     */
    @Test
    public void disposableMachinesShouldBeMarkedForTermination() {
        Machine disposable1 = makeMachine(1, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.disposable());
        Machine disposable2 = makeMachine(2, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.disposable());

        MachinePool pool = makePool(UtcTime.now(), asList(disposable1, disposable2));
        ResizePlanner planner = new ResizePlanner(pool, OLDEST);
        assertThat(planner.getActiveSize(), is(0));

        // same desired size
        ResizePlan plan = planner.calculateResizePlan(0);
        // not possible, since there aren't enough evictable candidates
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate(), is(asList(disposable1, disposable2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void callWithIllegalDesiredSize() {
        ResizePlanner planner = new ResizePlanner(makePool(), VictimSelectionPolicy.OLDEST);
        planner.calculateResizePlan(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullPool() {
        new ResizePlanner(null, VictimSelectionPolicy.OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullVictimSelectionPolicy() {
        new ResizePlanner(makePool(), null);
    }

}
