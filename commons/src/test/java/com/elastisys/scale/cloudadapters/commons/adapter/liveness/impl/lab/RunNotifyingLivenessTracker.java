package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl.lab;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.BootTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.RunTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl.NotifyingLivenessTracker;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl.StandardSshLivenessTestFactory;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

/**
 * Lab program for exercising the {@link NotifyingLivenessTracker}.
 *
 *
 *
 */
public class RunNotifyingLivenessTracker {
	static Logger LOG = LoggerFactory
			.getLogger(RunNotifyingLivenessTracker.class);

	// TODO: set to the IP address of the machine whose liveness is to be
	// checked
	private static final String MACHINE_IP = "<IP address>";

	// TODO: set to name of ssh login user
	private static final String LOGIN_USER = "ubuntu";
	// TODO: set to ssh private key path
	private static final String LOGIN_KEY = System
			.getenv("OS_INSTANCE_KEYPATH");

	// TODO: set to liveness test command
	private static final String LIVENESS_COMMAND = "service apache2 status | grep 'is running'";

	public static void main(String[] args) throws CloudAdapterException,
			InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		EventBus eventBus = new EventBus();

		LOG.info("login key: {}", LOGIN_KEY);

		NotifyingLivenessTracker livenessTracker = new NotifyingLivenessTracker(
				new StandardSshLivenessTestFactory(), eventBus, executor);
		BootTimeLivenessCheck bootTimeCheck = new BootTimeLivenessCheck(
				LIVENESS_COMMAND, 30, 20);
		RunTimeLivenessCheck runTimeCheck = new RunTimeLivenessCheck(
				LIVENESS_COMMAND, 60, 10, 15);
		livenessTracker.configure(new LivenessConfig(22, LOGIN_USER, LOGIN_KEY,
				bootTimeCheck, runTimeCheck));

		Machine machine = machine();

		LOG.info("liveness state for {}: {}", machine.getId(),
				livenessTracker.getLiveness(machine));
		// perform a runtime liveness test agains the machine
		LOG.info("submitting liveness test for: {}", machine);
		Future<LivenessTestResult> test = livenessTracker
				.checkRuntimeLiveness(machine);
		LOG.info("awaiting liveness test result ...");
		LivenessTestResult livenessTestResult = test.get();
		LOG.info("liveness test result: {}", livenessTestResult);
		LOG.info("liveness state for {}: {}", machine.getId(),
				livenessTracker.getLiveness(machine));

		executor.shutdownNow();
	}

	private static Machine machine() {
		List<String> publicIps = Lists.newArrayList(MACHINE_IP);
		List<String> privateIps = Collections.emptyList();
		return new Machine("machine@" + MACHINE_IP, MachineState.RUNNING,
				UtcTime.now(), publicIps, privateIps, new JsonObject());
	}
}
