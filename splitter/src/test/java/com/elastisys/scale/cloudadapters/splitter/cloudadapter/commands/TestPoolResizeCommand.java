package com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands;

import org.junit.Test;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.MockPrioritizedRemoteCloudAdapter;

public class TestPoolResizeCommand {
	/*
	 * More tests in TestStandardPrioritizedRemoteCloudAdapter, since they
	 * require a server being set up.
	 */

	@Test(expected = NullPointerException.class)
	public void testNullAdapter() {
		new PoolResizeCommand(null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativePoolSize() {
		new PoolResizeCommand(new MockPrioritizedRemoteCloudAdapter(), -1);
	}
}
