package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.makeMachine;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.makePool;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.nowOffset;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.terminationMarked;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.NEWEST_INSTANCE;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.OLDEST_INSTANCE;
import static com.elastisys.scale.commons.util.time.UtcTime.now;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.any;
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
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudpool.commons.termqueue.TerminationQueue;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link ResizePlanner}.
 */
public class TestResizePlanner {

    /** Default instance hour margin (in seconds) to use in most tests. */
    private final static long INSTANCE_HOUR_MARGIN = 300;

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2014-03-26T12:00.00Z"));
    }

    /**
     * Exercises the {@link ResizePlanner#getNetSize()} with different number of
     * instances in termination queue.
     */
    @Test
    public void testGetNetSize() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);

        // pool size: 0, termination queue size: 0
        ResizePlanner planner = new ResizePlanner(makePool(), new TerminationQueue(), OLDEST_INSTANCE,
                INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));

        // pool size: 2 (1 PENDING, 1 RUNNING), termination queue size: 0
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running));
        planner = new ResizePlanner(pool, new TerminationQueue(), OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));

        // allocated: 3 (1 REQUESTED, 1 PENDING, 1 RUNNING), termination queue
        // size: 0
        pool = makePool(UtcTime.now(), asList(requested, pending, running));
        TerminationQueue termq = new TerminationQueue();
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(3));

        // allocated: 1, termination queue size: 1
        pool = makePool(UtcTime.now(), asList(pending));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(pending, nowOffset(1500)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));

        // allocated: 2, termination queue size: 0
        pool = makePool(UtcTime.now(), asList(pending, running));
        termq = new TerminationQueue();
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));

        // allocated: 2, termination queue size: 1
        pool = makePool(UtcTime.now(), asList(pending, running));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(running, nowOffset(1500)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(1));

        // allocated: 2, termination queue size: 2
        pool = makePool(UtcTime.now(), asList(pending, running));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(pending, nowOffset(1500)));
        termq.add(new ScheduledTermination(running, nowOffset(2400)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
    }

    /**
     * Exercises the {@link ResizePlanner#getNetSize()} when there are machine
     * instances in terminal states in the pool.
     */
    @Test
    public void testGetNetSizeWithInstancesInTerminalState() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        Machine terminating = makeMachine(4, null, MachineState.TERMINATING);
        Machine terminated = makeMachine(5, null, MachineState.TERMINATED);
        Machine rejected = makeMachine(6, null, MachineState.REJECTED);

        // only three machines in a non-terminal state
        MachinePool pool = makePool(UtcTime.now(),
                asList(pending, running, requested, terminated, terminating, rejected));
        ResizePlanner planner = new ResizePlanner(pool, new TerminationQueue(), OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(3));
    }

    /**
     * Net size should not include pool members that are marked with an inactive
     * {@link MembershipStatus}.
     */
    @Test
    public void testGetNetSizeWithInactiveInstances() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        Machine blessed = makeMachine(3, null, MachineState.REQUESTED, MembershipStatus.blessed());
        Machine inactive1 = makeMachine(4, nowOffset(-7200), MachineState.RUNNING, MembershipStatus.awaitingService());
        Machine inactive2 = makeMachine(5, nowOffset(-7800), MachineState.RUNNING, new MembershipStatus(false, true));

        // allocated: 6, inactive: 2, termination queue size: 0
        MachinePool pool = makePool(UtcTime.now(), asList(requested, pending, running, blessed, inactive1, inactive2));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(4));
    }

    /**
     * Test scale-up with empty termination queue and verify that the correct
     * number of additional machines to request are suggested.
     */
    @Test
    public void scaleUpWithEmptyTerminationQueue() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-3600), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        // should not be considered as it is not active
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // empty machine pool
        ResizePlanner planner = new ResizePlanner(makePool(), new TerminationQueue(), OLDEST_INSTANCE,
                INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
        // 0 -> 1
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(0));
        // 0 -> 2
        plan = planner.calculateResizePlan(2);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(2));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(0));

        // pool size: 3 (allocated: 4, inactive: 1)
        MachinePool pool = makePool(now(), asList(requested, pending, running, outOfService));
        planner = new ResizePlanner(pool, new TerminationQueue(), OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(3));
        // 3 -> 4
        plan = planner.calculateResizePlan(4);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(0));
        // 3 -> 5
        plan = planner.calculateResizePlan(5);
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(2));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(0));
    }

    /**
     * Test scale-up with a non-empty termination queue and verify that the
     * {@link ResizePlanner} favors sparing termination-marked instances to
     * requesting additional instances when a scale-up is ordered.
     */
    @Test
    public void scaleUpWithNonEmptyTerminationQueue() {
        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        // should not be considered as it is not active
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 3 (allocated: 4, inactive: 1), termination queue: 1
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running, requested, outOfService));
        TerminationQueue termq = new TerminationQueue();
        termq.add(new ScheduledTermination(requested, nowOffset(0)));
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));
        // 2 -> 3
        ResizePlan plan = planner.calculateResizePlan(3);
        assertTrue(plan.hasScaleOutActions());
        // needs to spare one
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(1));
        assertThat(plan.getToTerminate().size(), is(0));
        // 2 -> 4
        plan = planner.calculateResizePlan(4);
        assertTrue(plan.hasScaleOutActions());
        // needs to spare one and request one additional
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToSpare(), is(1));
        assertThat(plan.getToTerminate().size(), is(0));

        // pool size: 2 (allocated: 3, inactive: 1), termination queue: 2
        pool = makePool(UtcTime.now(), asList(pending, running, outOfService));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(pending, nowOffset(1500)));
        termq.add(new ScheduledTermination(running, nowOffset(2400)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
        // 0 -> 1
        plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleOutActions());
        // needs to spare one
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(1));
        // 0 -> 2
        plan = planner.calculateResizePlan(2);
        assertTrue(plan.hasScaleOutActions());
        // needs to spare two
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(2));
        // 0 -> 3
        plan = planner.calculateResizePlan(3);
        assertTrue(plan.hasScaleOutActions());
        // needs to spare two and request one additional
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToSpare(), is(2));
    }

    /**
     * Test calculating resize plans when the desired size equals the net pool
     * size.
     */
    @Test
    public void testStayPutRequest() {
        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested = makeMachine(3, null, MachineState.REQUESTED);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 0 (allocated: 1, inactive: 1), termination queue: 0
        MachinePool pool = makePool(UtcTime.now(), asList(outOfService));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
        // 0 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

        pool = makePool(UtcTime.now(), asList(requested, pending));
        // pool size: 2, termination queue size: 0
        termq = new TerminationQueue();
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));
        // 2 -> 2
        plan = planner.calculateResizePlan(2);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

        // pool size: 2 (allocated: 3, inactive: 1), termination queue: 1
        pool = makePool(UtcTime.now(), asList(running, pending, outOfService));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(pending, nowOffset(1500)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(1));
        // 1 -> 1
        plan = planner.calculateResizePlan(1);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

        // pool size: 2 (allocated: 3, inactive: 1), termination queue: 2
        pool = makePool(UtcTime.now(), asList(running, pending, outOfService));
        termq = new TerminationQueue();
        termq.add(new ScheduledTermination(pending, nowOffset(1500)));
        termq.add(new ScheduledTermination(running, nowOffset(2400)));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
        // 0 -> 0
        plan = planner.calculateResizePlan(0);
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());

    }

    /**
     * Verify that scale-down actions are properly suggested when no instances
     * are termination marked.
     */
    @Test
    public void scaleDownWithEmptyTerminationQueue() {
        Machine pending = makeMachine(1, nowOffset(-1800), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-900), MachineState.RUNNING);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 1 (allocated: 2, inactive: 1), termination queue: 0
        MachinePool pool = makePool(UtcTime.now(), asList(pending, outOfService));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(1));
        // 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(1));

        // pool size: 2 (allocated: 3, inactive: 1), termination queue: 0
        pool = makePool(UtcTime.now(), asList(pending, running, outOfService));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));
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
    public void scaleDownWithInstancesInRequestedState() {
        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested1 = makeMachine(3, null, MachineState.REQUESTED);
        Machine requested2 = makeMachine(4, null, MachineState.REQUESTED);
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 4 (allocated: 5, inactive: 1), termination queue: 0
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running, requested1, requested2, outOfService));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(4));
        // 4 -> 3
        ResizePlan plan = planner.calculateResizePlan(3);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(1));
        assertThat(terminationMarked(plan), is(asList(requested1)));

        // 4 -> 2
        plan = planner.calculateResizePlan(2);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToSpare(), is(0));
        assertThat(plan.getToTerminate().size(), is(2));
        assertThat(terminationMarked(plan), is(asList(requested1, requested2)));
    }

    /**
     * Plan a scale-down with a non-empty termination queue and verify that only
     * instances not already marked for termination are selected for
     * termination.
     */
    @Test
    public void scaleDownWithNonEmptyTerminationQueue() {
        Machine machine1 = makeMachine(1, nowOffset(-1800));
        Machine machine2 = makeMachine(2, nowOffset(-900));
        Machine machine3 = makeMachine(3, nowOffset(-120));
        // should not be considered as it is inactive
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 3 (allocated: 4, inactive: 1), termination queue:
        // [machine1]
        MachinePool pool = makePool(UtcTime.now(), asList(machine1, machine2, machine3, outOfService));
        TerminationQueue termq = new TerminationQueue();
        termq.add(new ScheduledTermination(machine1, nowOffset(1500)));
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));
        // 2 -> 1
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleInActions());
        assertThat(plan.getToTerminate().size(), is(1));
        // [machine2] to be termination queued
        assertThat(terminationMarked(plan), is(asList(machine2)));
        assertFalse(plan.getToTerminate().equals(machine2));
        // none of the new termination-marked instances should already be marked
        // for termination
        assertFalse(any(terminationMarked(plan), in(termq.getQueuedInstances())));

        // 2 -> 0
        plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        // [machine2, machine3] to be termination queued
        assertThat(plan.getToTerminate().size(), is(2));
        assertThat(terminationMarked(plan), is(asList(machine2, machine3)));
        assertFalse(plan.getToTerminate().equals(machine2));
        // none of the new termination-marked instances should already be marked
        // for termination
        assertFalse(any(terminationMarked(plan), in(termq.getQueuedInstances())));
    }

    /**
     * Plan a scale-down when there are instances in the pool in terminal state
     * and verify that machines in a terminal state are never selected for
     * termination.
     */
    @Test
    public void scaleDownWithInstancesInTerminalStates() {
        Machine rejected = makeMachine(1, nowOffset(-3600), MachineState.REJECTED);
        Machine terminating = makeMachine(2, nowOffset(-3400), MachineState.TERMINATING);
        Machine terminated = makeMachine(3, nowOffset(-3200), MachineState.TERMINATED);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        // should not be counted as an evictable machine
        Machine outOfService = makeMachine(4, nowOffset(-7200), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        // pool size: 1 (allocated: 2, inactive: 1)
        MachinePool pool = makePool(UtcTime.now(), asList(rejected, terminating, terminated, running, outOfService));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(1));
        // 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        // only one candidate can be selected for termination
        assertThat(plan.getToTerminate().size(), is(1));
        assertThat(terminationMarked(plan), is(asList(running)));
    }

    /**
     * Plan a scale-down when the victim candidate set is empty (all marked for
     * termination or in an terminal state).
     */
    @Test
    public void scaleDownWithEmptyVictimCandidateSet() {
        Machine victim1 = makeMachine(1, nowOffset(-1800));
        Machine victim2 = makeMachine(2, nowOffset(-900));
        Machine terminated = makeMachine(3, null, MachineState.TERMINATED);

        MachinePool pool = makePool(UtcTime.now(), asList(victim1, victim2, terminated));
        TerminationQueue termq = new TerminationQueue();
        termq.add(new ScheduledTermination(victim1, nowOffset(1500)));
        termq.add(new ScheduledTermination(victim2, nowOffset(2400)));
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));
        // 0 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        // all instances marked for termination or in a terminal state => no
        // further scale-down possible
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
        TerminationQueue termq = new TerminationQueue();

        // victim selection policy: oldest instance
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(terminationMarked(planner.calculateResizePlan(2)), is(asList(machine1)));
        assertThat(terminationMarked(planner.calculateResizePlan(1)), is(asList(machine1, machine2)));
        assertThat(terminationMarked(planner.calculateResizePlan(0)), is(asList(machine1, machine2, machine3)));

        // victim selection policy: newest instance
        planner = new ResizePlanner(pool, termq, NEWEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(terminationMarked(planner.calculateResizePlan(2)), is(asList(machine3)));
        assertThat(terminationMarked(planner.calculateResizePlan(1)), is(asList(machine3, machine2)));
        assertThat(terminationMarked(planner.calculateResizePlan(0)), is(asList(machine3, machine2, machine1)));

        // victim selection policy: closest to instance hour
        planner = new ResizePlanner(pool, termq, CLOSEST_TO_INSTANCE_HOUR, INSTANCE_HOUR_MARGIN);
        assertThat(terminationMarked(planner.calculateResizePlan(2)), is(asList(machine2)));
        assertThat(terminationMarked(planner.calculateResizePlan(1)), is(asList(machine2, machine3)));
        assertThat(terminationMarked(planner.calculateResizePlan(0)), is(asList(machine2, machine3, machine1)));
    }

    /**
     * Tests that correct termination times are planned at scale-down when
     * immediate termination is specified (that is, a {@code 0} instance hour
     * margin).
     */
    @Test
    public void verifyImmediateTermination() {
        FrozenTime.setFixed(UtcTime.parse("2012-03-26T12:50:00Z"));
        Machine machine1 = makeMachine(1, UtcTime.parse("2012-03-26T10:45:00Z"));
        Machine machine2 = makeMachine(2, UtcTime.parse("2012-03-26T11:25:00Z"));
        Machine machine3 = makeMachine(3, UtcTime.parse("2012-03-26T12:34:00Z"));
        MachinePool pool = makePool(UtcTime.now(), asList(machine1, machine2, machine3));
        TerminationQueue termq = new TerminationQueue();

        int immediateTerminationMargin = 0;
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, immediateTerminationMargin);
        // 3 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertThat(terminationMarked(plan), is(asList(machine1, machine2, machine3)));

        // all termination times should be immediate
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.now()));
        assertThat(plan.getToTerminate().get(1).getTerminationTime(), is(UtcTime.now()));
        assertThat(plan.getToTerminate().get(2).getTerminationTime(), is(UtcTime.now()));
    }

    /**
     * Tests that correct termination times are planned at scale-down when late
     * termination is specified (that is, a non-zero instance hour margin).
     */
    @Test
    public void verifyLateTermination() {
        FrozenTime.setFixed(UtcTime.parse("2012-03-26T12:50:00Z"));
        // machine1: next instance hour starts 2012-03-26T13:45:30Z
        Machine machine1 = makeMachine(1, UtcTime.parse("2012-03-26T10:45:30Z"));
        // machine2: next instance hour starts 2012-03-26T13:25:00Z
        Machine machine2 = makeMachine(2, UtcTime.parse("2012-03-26T11:25:00Z"));
        // machine3: next instance hour starts 2012-03-26T13:34:00Z
        Machine machine3 = makeMachine(3, UtcTime.parse("2012-03-26T12:34:00Z"));
        MachinePool pool = makePool(UtcTime.now(), asList(machine1, machine2, machine3));
        TerminationQueue termq = new TerminationQueue();

        // instanceHourMargin: 5 minutes
        int instanceHourMargin = 300;
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, instanceHourMargin);
        // 3 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        assertThat(terminationMarked(plan), is(asList(machine1, machine2, machine3)));
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:40:30Z")));
        assertThat(plan.getToTerminate().get(1).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:20:00Z")));
        assertThat(plan.getToTerminate().get(2).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:29:00Z")));

        FrozenTime.setFixed(UtcTime.parse("2012-03-26T13:22:00Z"));
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, instanceHourMargin);
        plan = planner.calculateResizePlan(0);
        assertThat(terminationMarked(plan), is(asList(machine1, machine2, machine3)));
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:40:30Z")));
        // immediate termination (closer than 5 minutes to instance hour end)
        assertThat(plan.getToTerminate().get(1).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:22:00Z")));
        assertThat(plan.getToTerminate().get(2).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:29:00Z")));

        // instanceHourMargin: 10 minutes
        FrozenTime.setFixed(UtcTime.parse("2012-03-26T12:50:00Z"));
        instanceHourMargin = 600;
        planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, instanceHourMargin);
        // 3 -> 0
        plan = planner.calculateResizePlan(0);
        assertThat(terminationMarked(plan), is(asList(machine1, machine2, machine3)));
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:35:30Z")));
        assertThat(plan.getToTerminate().get(1).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:15:00Z")));
        assertThat(plan.getToTerminate().get(2).getTerminationTime(), is(UtcTime.parse("2012-03-26T13:24:00Z")));
    }

    /**
     * On scale-down, verify that machines in REQUESTED state are scheduled for
     * immediate termination (irrespective of instance hour margin) since they
     * typically don't incur any cost (yet).
     */
    @Test
    public void verifyThatRequestedStateInstancesAreTerminatedImmediately() {
        FrozenTime.setFixed(UtcTime.parse("2012-03-26T12:00:00.000Z"));

        Machine pending = makeMachine(1, nowOffset(-3600), MachineState.PENDING);
        Machine running = makeMachine(2, nowOffset(-1800), MachineState.RUNNING);
        Machine requested1 = makeMachine(3, null, MachineState.REQUESTED);
        Machine requested2 = makeMachine(4, nowOffset(-1800), MachineState.REQUESTED);

        // pool size: 4, termination queue size: 0
        MachinePool pool = makePool(UtcTime.now(), asList(pending, running, requested1, requested2));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(4));
        // 4 -> 3
        ResizePlan plan = planner.calculateResizePlan(3);
        assertThat(terminationMarked(plan), is(asList(requested1)));
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.now()));

        // 4 -> 2
        plan = planner.calculateResizePlan(2);
        assertThat(terminationMarked(plan), is(asList(requested1, requested2)));
        assertThat(plan.getToTerminate().get(0).getTerminationTime(), is(UtcTime.now()));
        assertThat(plan.getToTerminate().get(1).getTerminationTime(), is(UtcTime.now()));
    }

    /**
     * Any machine in the pool marked with a {@link MembershipStatus} of
     * inactive, should be replaced.
     */
    @Test
    public void verifyThatInactiveMachinesAreReplaced() {
        Machine inactive = makeMachine(1, nowOffset(-1800), MachineState.RUNNING, MembershipStatus.awaitingService());

        MachinePool pool = makePool(UtcTime.now(), asList(inactive));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);

        // inactive machine should not be counted
        assertThat(planner.getNetSize(), is(0));
        // replacement machines should be fired up for inactive instances
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
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(2));

        // scale down by one: 2 -> 1
        // make sure the younger machine gets terminated despite using
        // OLDEST_INSTANCE victim selection policy, since the blessed instance
        // is non-evictable
        ResizePlan plan = planner.calculateResizePlan(1);
        assertTrue(plan.hasScaleInActions());
        assertThat(terminationMarked(plan), is(asList(youngerMachine)));

        // scale down by two: not entirely possible, since there aren't enough
        // evictable candidates
        plan = planner.calculateResizePlan(0);
        assertTrue(plan.hasScaleInActions());
        assertThat(terminationMarked(plan), is(asList(youngerMachine)));
    }

    /**
     * test the situation where we cannot scale down since there aren't enough
     * evictable candidates
     */
    @Test
    public void scaleDownWithTooFewEvictableCandidates() {
        Machine notEvictable1 = makeMachine(1, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.blessed());
        Machine notEvictable2 = makeMachine(2, nowOffset(-3600), MachineState.RUNNING,
                MembershipStatus.awaitingService());

        MachinePool pool = makePool(UtcTime.now(), asList(notEvictable1, notEvictable2));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(1));

        // scale down by one: 1 -> 0
        ResizePlan plan = planner.calculateResizePlan(0);
        // not possible, since there aren't enough evictable candidates
        assertFalse(plan.hasScaleOutActions());
        assertFalse(plan.hasScaleInActions());
    }

    /**
     * Any disposable (with inactive, evictable {@link MembershipStatus})
     * machines should be marked for termination.
     */
    @Test
    public void disposableMachinesShouldBeMarkedForTermination() {
        Machine disposable1 = makeMachine(1, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.disposable());
        Machine disposable2 = makeMachine(2, nowOffset(-3600), MachineState.RUNNING, MembershipStatus.disposable());

        MachinePool pool = makePool(UtcTime.now(), asList(disposable1, disposable2));
        TerminationQueue termq = new TerminationQueue();
        ResizePlanner planner = new ResizePlanner(pool, termq, OLDEST_INSTANCE, INSTANCE_HOUR_MARGIN);
        assertThat(planner.getNetSize(), is(0));

        // same desired size
        ResizePlan plan = planner.calculateResizePlan(0);
        // not possible, since there aren't enough evictable candidates
        assertTrue(plan.hasScaleInActions());
        // assertTrue(plan.isScaleOut());

        // assertThat(plan.getToRequest(), is(1));
        assertThat(terminationMarked(plan), is(asList(disposable1, disposable2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void callWithIllegalDesiredSize() {
        ResizePlanner planner = new ResizePlanner(makePool(), new TerminationQueue(),
                VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 600);
        planner.calculateResizePlan(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullPool() {
        new ResizePlanner(null, new TerminationQueue(), VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 600);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullTerminationQueue() {
        new ResizePlanner(makePool(), null, VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 600);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullVictimSelectionPolicy() {
        new ResizePlanner(makePool(), new TerminationQueue(), null, 600);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNegativeInstanceHourMargin() {
        new ResizePlanner(makePool(), new TerminationQueue(), VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithTooLongInstanceHourMargin() {
        new ResizePlanner(makePool(), new TerminationQueue(), VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 3600);
    }

}
