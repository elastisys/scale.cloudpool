package com.elastisys.scale.cloudpool.splitter.lab;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.cloudpool.splitter.Splitter;
import com.elastisys.scale.cloudpool.splitter.config.PoolSizeCalculator;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.config.SplitterConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the {@link Splitter} {@link CloudPool} via
 * commands read from {@code stdin}.
 */
public class RunPool {
	static Logger LOG = LoggerFactory.getLogger(RunPool.class);

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPool splitter = new Splitter();

		// set up configuration
		PrioritizedCloudPool childPool1 = new PrioritizedCloudPool(50,
				"localhost", 8443, null, null);
		PrioritizedCloudPool childPool2 = new PrioritizedCloudPool(50,
				"localhost", 9443, null, null);
		SplitterConfig config = new SplitterConfig(PoolSizeCalculator.STRICT,
				Arrays.asList(childPool1, childPool2), 30);

		JsonObject jsonConfig = JsonUtils.toJson(config).getAsJsonObject();
		LOG.info("setting config: {}", jsonConfig);
		splitter.configure(jsonConfig);

		new CloudPoolCommandLineDriver(splitter).start();

		executorService.shutdownNow();
	}

}
