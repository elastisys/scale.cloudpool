package com.elastisys.scale.cloudadapters.openstack.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.MachineState;

/**
 * Exercises the {@link StatusToMachineState} class.
 */
public class TestStatusToMachineState {

	@Test
	public void testStateConversion() {
		assertThat(convert(Status.ACTIVE), is(MachineState.RUNNING));
		assertThat(convert(Status.ERROR), is(MachineState.RUNNING));
		assertThat(convert(Status.PASSWORD), is(MachineState.RUNNING));

		assertThat(convert(Status.BUILD), is(MachineState.PENDING));
		assertThat(convert(Status.REBUILD), is(MachineState.PENDING));
		assertThat(convert(Status.REBOOT), is(MachineState.PENDING));
		assertThat(convert(Status.HARD_REBOOT), is(MachineState.PENDING));
		assertThat(convert(Status.RESIZE), is(MachineState.PENDING));
		assertThat(convert(Status.REVERT_RESIZE), is(MachineState.PENDING));
		assertThat(convert(Status.VERIFY_RESIZE), is(MachineState.PENDING));

		assertThat(convert(Status.STOPPED), is(MachineState.TERMINATED));
		assertThat(convert(Status.SHUTOFF), is(MachineState.TERMINATED));
		assertThat(convert(Status.PAUSED), is(MachineState.TERMINATED));
		assertThat(convert(Status.SUSPENDED), is(MachineState.TERMINATED));
		assertThat(convert(Status.DELETED), is(MachineState.TERMINATED));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertUnrecognizedStatus() {
		convert(Status.SHELVED_OFFLOADED);
	}

	private static MachineState convert(Status state) {
		return new StatusToMachineState().apply(state);
	}
}
