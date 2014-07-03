package com.elastisys.scale.cloudadapters.commons.scaledown.strategies;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.scaledown.AbstractScaledownTest;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionStrategy;
import com.elastisys.scale.cloudadapters.commons.scaledown.strategies.NewestInstanceVictimSelectionStrategy;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

public class TestNewestInstanceVictimSelectionStrategy extends
		AbstractScaledownTest {

	/** Object under test. */
	private final VictimSelectionStrategy strategy = NewestInstanceVictimSelectionStrategy.INSTANCE;

	@Test(expected = NullPointerException.class)
	public void onNullCandidateSet() {
		this.strategy.selectVictim(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onEmptyCandidateSet() {
		List<Machine> empty = Arrays.asList();
		this.strategy.selectVictim(empty);
	}

	@Test
	public void onSingletonCandidateSet() {
		List<Machine> set = Arrays
				.asList(instance("i-1", "2012-06-01T12:00:00"));
		Machine victim = this.strategy.selectVictim(set);
		assertThat(victim, is(getOnlyElement(set)));
	}

	@Test
	public void onMultiCandidateSet() {
		FrozenTime.setFixed(UtcTime.parse("2012-06-01T10:00:00"));

		// newest instance
		Machine instance1 = instance("i-1", "2012-06-01T09:45:00");
		Machine instance2 = instance("i-2", "2012-06-01T09:30:00");
		Machine instance3 = instance("i-3", "2012-06-01T09:15:00");

		// two candidates
		List<Machine> set = asList(instance1, instance2);
		assertThat(this.strategy.selectVictim(set), is(instance1));

		// three candidates
		set = asList(instance1, instance2, instance3);
		assertThat(this.strategy.selectVictim(set), is(instance1));

		// candidate order should not matter
		set = asList(instance2, instance3, instance1);
		assertThat(this.strategy.selectVictim(set), is(instance1));
		set = asList(instance3, instance1, instance2);
		assertThat(this.strategy.selectVictim(set), is(instance1));
	}
}
