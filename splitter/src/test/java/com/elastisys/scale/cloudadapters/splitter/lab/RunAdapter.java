package com.elastisys.scale.cloudadapters.splitter.lab;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapters.commons.util.cli.CloudadapterCommandLineDriver;
import com.elastisys.scale.cloudadapters.splitter.Splitter;
import com.elastisys.scale.cloudadapters.splitter.config.PoolSizeCalculator;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.config.SplitterConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the {@link Splitter} {@link CloudAdapter}
 * via commands read from {@code stdin}.
 */
public class RunAdapter {
	static Logger LOG = LoggerFactory.getLogger(RunAdapter.class);

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudAdapter splitter = new Splitter();

		// set up configuration
		PrioritizedCloudAdapter childAdapter1 = new PrioritizedCloudAdapter(50,
				"localhost", 8443, null, null);
		PrioritizedCloudAdapter childAdapter2 = new PrioritizedCloudAdapter(50,
				"localhost", 9443, null, null);
		SplitterConfig config = new SplitterConfig(PoolSizeCalculator.STRICT,
				Arrays.asList(childAdapter1, childAdapter2), 30);

		JsonObject jsonConfig = JsonUtils.toJson(config).getAsJsonObject();
		LOG.info("setting config: {}", jsonConfig);
		splitter.configure(jsonConfig);

		new CloudadapterCommandLineDriver(splitter).start();

		executorService.shutdownNow();
	}

}
