package com.elastisys.scale.cloudadapters.commons.termqueue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;

/**
 * Verifies the behavior of the {@link ScheduledTermination} class.
 *
 *
 *
 */
public class TestScheduledTermination {

	@Test
	public void create() {
		DateTime terminationTime = UtcTime.now();
		Machine instance = instance("i-123");
		ScheduledTermination termination = new ScheduledTermination(instance,
				terminationTime);
		assertThat(termination.getInstance(), is(instance));
		assertThat(termination.getTerminationTime(), is(terminationTime));
	}

	@Test(expected = NullPointerException.class)
	public void createWithNullInstance() {
		new ScheduledTermination(null, UtcTime.now());
	}

	@Test(expected = NullPointerException.class)
	public void createWithNullTime() {
		new ScheduledTermination(instance("i-123"), null);
	}

	@Test
	public void testEqualsAndHashCode() {
		DateTime t1 = UtcTime.now().minusSeconds(1);
		DateTime t2 = UtcTime.now().plusSeconds(1);
		Machine instance1 = instance("i-1");
		Machine instance2 = instance("i-2");

		// equals
		assertThat(new ScheduledTermination(instance1, t1),
				is(new ScheduledTermination(instance1, t1)));
		assertThat(new ScheduledTermination(instance1, t1),
				is(not(new ScheduledTermination(instance1, t2))));
		assertThat(new ScheduledTermination(instance1, t1),
				is(not(new ScheduledTermination(instance2, t1))));
		assertThat(new ScheduledTermination(instance1, t1),
				is(not(new ScheduledTermination(instance2, t2))));

		// hashcode
		assertThat(new ScheduledTermination(instance1, t1).hashCode(),
				is(new ScheduledTermination(instance1, t1).hashCode()));
		assertThat(new ScheduledTermination(instance1, t1).hashCode(),
				is(not(new ScheduledTermination(instance1, t2).hashCode())));
		assertThat(new ScheduledTermination(instance1, t1).hashCode(),
				is(not(new ScheduledTermination(instance2, t1).hashCode())));
		assertThat(new ScheduledTermination(instance1, t1).hashCode(),
				is(not(new ScheduledTermination(instance2, t2).hashCode())));
	}

	@Test
	public void testCompareTo() {
		DateTime t1 = UtcTime.now().minusSeconds(1);
		DateTime t2 = UtcTime.now().plusSeconds(1);
		Machine instance = instance("i-1");

		ScheduledTermination termination1 = new ScheduledTermination(instance,
				t1);
		ScheduledTermination termination2 = new ScheduledTermination(instance,
				t2);

		assertThat(termination1.compareTo(termination1), is(0));
		assertThat(termination1.compareTo(termination2), is(-1));
		assertThat(termination2.compareTo(termination1), is(1));
	}

	private Machine instance(String withId) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(withId, MachineState.RUNNING, UtcTime.now(),
				publicIps, privateIps);
	}
}
