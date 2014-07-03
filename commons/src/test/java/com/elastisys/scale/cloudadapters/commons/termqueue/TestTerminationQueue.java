package com.elastisys.scale.cloudadapters.commons.termqueue;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link TerminationQueue}.
 * 
 * 
 * 
 */
public class TestTerminationQueue {

	/** Object under test. */
	private TerminationQueue queue;

	@Before
	public void onSetup() {
		this.queue = new TerminationQueue();
	}

	@Test
	public void add() {
		assertThat(this.queue.size(), is(0));
		ScheduledTermination termination1 = scheduledTermination("i-1",
				"2013-06-01T12:00:00Z");
		ScheduledTermination termination2 = scheduledTermination("i-2",
				"2013-06-01T13:00:00Z");
		ScheduledTermination termination3 = scheduledTermination("i-3",
				"2013-06-01T12:30:00Z");
		this.queue.add(termination1);
		assertThat(this.queue.size(), is(1));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1)));
		this.queue.add(termination2);
		assertThat(this.queue.size(), is(2));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination2)));
		this.queue.add(termination3);
		assertThat(this.queue.size(), is(3));
		// note: returns terminations in order
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination3, termination2)));
	}

	@Test
	public void addAll() {
		assertThat(this.queue.size(), is(0));
		ScheduledTermination termination1 = scheduledTermination("i-1",
				"2013-06-01T12:00:00Z");
		ScheduledTermination termination2 = scheduledTermination("i-2",
				"2013-06-01T13:00:00Z");
		ScheduledTermination termination3 = scheduledTermination("i-3",
				"2013-06-01T12:30:00Z");
		this.queue.addAll(Arrays.asList(termination1));
		assertThat(this.queue.size(), is(1));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1)));
		this.queue.addAll(Arrays.asList(termination2, termination3));
		assertThat(this.queue.size(), is(3));
		// note: returns terminations in order
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination3, termination2)));
	}

	@Test
	public void popOverdueInstances() {
		ScheduledTermination termination1 = scheduledTermination("i-1",
				"2013-06-01T12:00:00Z");
		ScheduledTermination termination2 = scheduledTermination("i-2",
				"2013-06-01T13:00:00Z");
		ScheduledTermination termination3 = scheduledTermination("i-3",
				"2013-06-01T12:30:00Z");
		ScheduledTermination termination4 = scheduledTermination("i-4",
				"2013-06-01T12:01:00Z");

		this.queue.addAll(Arrays.asList(termination1, termination2,
				termination3, termination4));
		assertThat(this.queue.size(), is(4));
		assertThat(
				this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination4, termination3,
						termination2)));

		// no overdue instances
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T11:00:00Z"));
		this.queue.popOverdueInstances();
		assertThat(this.queue.size(), is(4));
		// no overdue instances
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T11:59:59Z"));
		this.queue.popOverdueInstances();
		assertThat(this.queue.size(), is(4));

		// terminate i-1
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00Z"));
		this.queue.popOverdueInstances();
		assertThat(this.queue.size(), is(3));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination4, termination3, termination2)));

		// terminate i-4 and i-3
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:31:00Z"));
		this.queue.popOverdueInstances();
		assertThat(this.queue.size(), is(1));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination2)));

		// terminate i-2
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T13:00:01Z"));
		this.queue.popOverdueInstances();
		assertThat(this.queue.size(), is(0));
		assertThat(this.queue.getQueuedInstances(), is(instanceList()));
	}

	@Test
	public void spare() {
		ScheduledTermination termination1 = scheduledTermination("i-1",
				"2013-06-01T12:00:00Z");
		ScheduledTermination termination2 = scheduledTermination("i-2",
				"2013-06-01T13:00:00Z");
		ScheduledTermination termination3 = scheduledTermination("i-3",
				"2013-06-01T12:30:00Z");
		ScheduledTermination termination4 = scheduledTermination("i-4",
				"2013-06-01T12:01:00Z");

		this.queue.addAll(Arrays.asList(termination1, termination2,
				termination3, termination4));
		assertThat(this.queue.size(), is(4));
		assertThat(
				this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination4, termination3,
						termination2)));

		// spare none
		List<ScheduledTermination> spared = this.queue.spare(0);
		assertThat(spared.size(), is(0));

		// spare i-1
		spared = this.queue.spare(1);
		assertThat(spared, is(asList(termination1)));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination4, termination3, termination2)));

		// spare i-4 and i-3
		spared = this.queue.spare(2);
		assertThat(spared, is(asList(termination4, termination3)));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination2)));

		// spare i-2
		spared = this.queue.spare(1);
		assertThat(spared, is(asList(termination2)));
		assertThat(this.queue.getQueuedInstances(), is(instanceList()));

	}

	@Test
	public void filter() {
		ScheduledTermination termination1 = scheduledTermination("i-1",
				"2013-06-01T12:00:00Z");
		ScheduledTermination termination2 = scheduledTermination("i-2",
				"2013-06-01T13:00:00Z");
		ScheduledTermination termination3 = scheduledTermination("i-3",
				"2013-06-01T12:30:00Z");
		ScheduledTermination termination4 = scheduledTermination("i-4",
				"2013-06-01T12:01:00Z");

		this.queue.addAll(Arrays.asList(termination1, termination2,
				termination3, termination4));
		assertThat(this.queue.size(), is(4));
		assertThat(
				this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination4, termination3,
						termination2)));

		// filter that keeps all instances
		this.queue.filter(Arrays.asList(termination1.getInstance(),
				termination2.getInstance(), termination3.getInstance(),
				termination4.getInstance()));
		assertThat(this.queue.size(), is(4));
		assertThat(
				this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination4, termination3,
						termination2)));

		// filter that keeps all but i-2
		this.queue.filter(Arrays.asList(termination1.getInstance(),
				termination3.getInstance(), termination4.getInstance()));
		assertThat(this.queue.size(), is(3));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination1, termination4, termination3)));

		// filter that only keeps i-4
		this.queue.filter(Arrays.asList(termination4.getInstance()));
		assertThat(this.queue.size(), is(1));
		assertThat(this.queue.getQueuedInstances(),
				is(instanceList(termination4)));
	}

	private ScheduledTermination scheduledTermination(String withInstanceId,
			String time) {
		return new ScheduledTermination(instance(withInstanceId),
				UtcTime.parse(time));
	}

	private List<Machine> instanceList(ScheduledTermination... terminations) {
		List<Machine> instanceList = Lists.newArrayList();
		for (ScheduledTermination termination : terminations) {
			instanceList.add(termination.getInstance());
		}
		return instanceList;
	}

	private Machine instance(String withId) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(withId, MachineState.RUNNING, UtcTime.now(),
				publicIps, privateIps, new JsonObject());
	}

}
