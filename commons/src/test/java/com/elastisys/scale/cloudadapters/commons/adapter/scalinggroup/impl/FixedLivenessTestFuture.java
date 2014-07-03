package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;

/**
 * A {@link LivenessTestResult} {@link Future} that returns a given
 * {@link LivenessTestResult} when its {@link #get()} method is called. Intended
 * for testing.
 * 
 * 
 * 
 */
public class FixedLivenessTestFuture implements Future<LivenessTestResult> {

	private final LivenessTestResult fixedTestResult;

	public FixedLivenessTestFuture(LivenessTestResult fixedTestResult) {
		this.fixedTestResult = fixedTestResult;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public LivenessTestResult get() throws InterruptedException,
			ExecutionException {
		return this.fixedTestResult;
	}

	@Override
	public LivenessTestResult get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.fixedTestResult;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

}
