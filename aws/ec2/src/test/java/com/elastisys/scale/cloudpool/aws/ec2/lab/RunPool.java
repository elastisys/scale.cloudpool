package com.elastisys.scale.cloudpool.aws.ec2.lab;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsEc2Client;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the EC2 {@link CloudPool} via commands read
 * from {@code stdin}.
 */
public class RunPool extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunPool.class);

	private static final Path configFile = Paths.get(".", "myconfig.json");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPool pool = new BaseCloudPool(
				new Ec2PoolDriver(new AwsEc2Client()));

		JsonObject config = JsonUtils.parseJsonFile(configFile.toFile())
				.getAsJsonObject();
		pool.configure(config);

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}
}
