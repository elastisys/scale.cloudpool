package com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands;

import org.junit.Test;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands.GetMachinePoolCommand;

public class TestGetMachinePoolCommand {
	/*
	 * More tests in TestStandardPrioritizedRemoteCloudAdapter, since they
	 * require a server being set up.
	 */

	@Test(expected = NullPointerException.class)
	public void testNullAdapter() {
		new GetMachinePoolCommand(null);
	}

}
