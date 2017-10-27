package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.toTerminate;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.victim;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;

/**
 * Exercises the {@link ResizePlan} class.
 */
public class TestResizePlan {

    /**
     * Verify proper behavior for plans representing scale-up decisions.
     */
    @Test
    public void testScaleUpPlans() {
        List<Machine> victimList = toTerminate();
        ResizePlan plan = new ResizePlan(1, victimList);
        assertFalse(plan.hasScaleInActions());
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToTerminate(), is(victimList));

        plan = new ResizePlan(1, victimList);
        assertFalse(plan.hasScaleInActions());
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToTerminate(), is(victimList));

        plan = new ResizePlan(2, victimList);
        assertFalse(plan.hasScaleInActions());
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(2));
        assertThat(plan.getToTerminate(), is(victimList));
    }

    /**
     * Verify proper behavior for plans representing scale-down decisions.
     */
    @Test
    public void testScaleDownPlans() {
        List<Machine> victimList = toTerminate(victim("i-1"));
        ResizePlan plan = new ResizePlan(0, victimList);
        assertTrue(plan.hasScaleInActions());
        assertFalse(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToTerminate(), is(victimList));

        victimList = toTerminate(victim("i-1"), victim("i-2"));
        plan = new ResizePlan(0, victimList);
        assertTrue(plan.hasScaleInActions());
        assertFalse(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(0));
        assertThat(plan.getToTerminate(), is(victimList));
    }

    /**
     * A {@link ResizePlan} can suggest both scale-out actions and scale-in
     * actions. For example, when a member has been marked disposable
     * (membership status inactive and evictable) at the same time as pool needs
     * to grow.
     */
    @Test
    public void testMixedPlans() {
        List<Machine> victimList = toTerminate(victim("i-1"));
        ResizePlan plan = new ResizePlan(1, victimList);
        assertTrue(plan.hasScaleInActions());
        assertTrue(plan.hasScaleOutActions());
        assertThat(plan.getToRequest(), is(1));
        assertThat(plan.getToTerminate(), is(victimList));
    }

    /**
     * Verify proper behavior for plans representing neither a scale-down nor a
     * scale-up decision.
     */
    @Test
    public void testNoChangePlans() {
        ResizePlan noChangePlan = new ResizePlan(0, toTerminate());
        assertFalse(noChangePlan.hasScaleInActions());
        assertFalse(noChangePlan.hasScaleOutActions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithIllegalToRequestValue() {
        new ResizePlan(-1, toTerminate());
    }

    @Test
    public void createWithNullToTerminateValue() {
        ResizePlan plan = new ResizePlan(0, null);
        assertThat(plan.getToTerminate().size(), is(0));
    }

}
