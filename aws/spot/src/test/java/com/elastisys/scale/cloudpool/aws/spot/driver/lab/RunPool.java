package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * * Lab program that exercises the AWS Spot Instance {@link CloudPool} via
 * commands read from {@code stdin}.
 */
public class RunPool {
	static Logger LOG = LoggerFactory.getLogger(RunPool.class);
	/**
	 * TODO: set up a cloud pool configuration file for the
	 * {@link OpenStackPoolDriver}. Relative paths are relative to base
	 * directory of enclosing Maven project.
	 */
	private static final Path cloudPoolConfig = Paths.get(".", "myconfig.json");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPool pool = new BaseCloudPool(new SpotPoolDriver(
				new AwsSpotClient()));

		JsonObject config = JsonUtils.parseJsonFile(cloudPoolConfig.toFile())
				.getAsJsonObject();
		pool.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}

}