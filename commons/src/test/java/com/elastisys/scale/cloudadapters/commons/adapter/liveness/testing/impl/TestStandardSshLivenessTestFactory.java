package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machine;
import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.validLivenessConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTestFactory;

/**
 * Exercises the {@link StandardSshLivenessTestFactory}.
 * 
 * 
 * 
 */
public class TestStandardSshLivenessTestFactory {

	/** Object under test. */
	private SshLivenessTestFactory factory;

	@Before
	public void onSetup() {
		this.factory = new StandardSshLivenessTestFactory();
	}

	@Test
	public void testCreateBootTimeCheck() {
		Machine machine = machine("i-1", MachineState.PENDING);
		SshLivenessTest livenessTest = this.factory.createBootTimeCheck(
				machine, validLivenessConfig());

		assertThat(livenessTest, is(StandardSshLivenessTest.class));
		assertThat(livenessTest.getMachine(), is(machine));
	}

	@Test
	public void testCreateRunTimeCheck() {
		Machine machine = machine("i-1", MachineState.PENDING);
		SshLivenessTest livenessTest = this.factory.createRunTimeCheck(machine,
				validLivenessConfig());

		assertThat(livenessTest, is(StandardSshLivenessTest.class));
		assertThat(livenessTest.getMachine(), is(machine));
	}

}
