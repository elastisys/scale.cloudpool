package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.termination;
import static com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanTestUtils.toTerminate;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlan;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link ResizePlan} class.
 * 
 * 
 * 
 */
public class TestResizePlan {

	/**
	 * Verify proper behavior for plans representing scale-up decisions.
	 */
	@Test
	public void testScaleUpPlans() {
		List<ScheduledTermination> victimList = toTerminate();
		ResizePlan plan = new ResizePlan(1, 0, victimList);
		assertFalse(plan.isScaleDown());
		assertTrue(plan.isScaleUp());
		assertThat(plan.getToRequest(), is(1));
		assertThat(plan.getToSpare(), is(0));
		assertThat(plan.getToTerminate(), is(victimList));

		plan = new ResizePlan(1, 1, victimList);
		assertFalse(plan.isScaleDown());
		assertTrue(plan.isScaleUp());
		assertThat(plan.getToRequest(), is(1));
		assertThat(plan.getToSpare(), is(1));
		assertThat(plan.getToTerminate(), is(victimList));

		plan = new ResizePlan(2, 3, victimList);
		assertFalse(plan.isScaleDown());
		assertTrue(plan.isScaleUp());
		assertThat(plan.getToRequest(), is(2));
		assertThat(plan.getToSpare(), is(3));
		assertThat(plan.getToTerminate(), is(victimList));
	}

	/**
	 * Verify proper behavior for plans representing scale-up decisions.
	 */
	@Test
	public void testScaleDownPlans() {
		List<ScheduledTermination> victimList = toTerminate(termination("i-1",
				UtcTime.now()));
		ResizePlan plan = new ResizePlan(0, 0, victimList);
		assertTrue(plan.isScaleDown());
		assertFalse(plan.isScaleUp());
		assertThat(plan.getToRequest(), is(0));
		assertThat(plan.getToSpare(), is(0));
		assertThat(plan.getToTerminate(), is(victimList));

		victimList = toTerminate(termination("i-1", UtcTime.now()),
				termination("i-2", UtcTime.now()));
		plan = new ResizePlan(0, 0, victimList);
		assertTrue(plan.isScaleDown());
		assertFalse(plan.isScaleUp());
		assertThat(plan.getToRequest(), is(0));
		assertThat(plan.getToSpare(), is(0));
		assertThat(plan.getToTerminate(), is(victimList));
	}

	/**
	 * Verify proper behavior for plans representing neither a scale-down nor a
	 * scale-up decision.
	 */
	@Test
	public void testNoChangePlans() {
		ResizePlan noChangePlan = new ResizePlan(0, 0, toTerminate());
		assertFalse(noChangePlan.isScaleDown());
		assertFalse(noChangePlan.isScaleUp());
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalToSpareValue() {
		new ResizePlan(0, -1, toTerminate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalToRequestValue() {
		new ResizePlan(-1, 0, toTerminate());
	}

	@Test
	public void createWithNullToTerminateValue() {
		ResizePlan plan = new ResizePlan(0, 0, null);
		assertThat(plan.getToTerminate().size(), is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalCombinationOfValues() {
		new ResizePlan(1, 0, toTerminate(termination("i-1", UtcTime.now())));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalCombinationOfValues2() {
		new ResizePlan(0, 1, toTerminate(termination("i-1", UtcTime.now())));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalCombinationOfValues3() {
		new ResizePlan(1, 1, toTerminate(termination("i-1", UtcTime.now())));
	}

}
