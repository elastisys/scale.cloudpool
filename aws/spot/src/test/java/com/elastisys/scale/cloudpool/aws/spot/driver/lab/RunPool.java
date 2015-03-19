package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.MailServerSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * * Lab program that exercises the AWS Spot Instance {@link CloudPool} via
 * commands read from {@code stdin}.
 */
public class RunPool extends BaseClient {

	// TODO: set launch configuration parameters for new group instances
	private static final String size = "t1.micro";
	private static final String image = "ami-3cf8b154";
	private static final List<String> bootScript = Arrays.asList("#!bin/bash",
			"sudo apt-get update -qy", "sudo apt-get install -qy apache2");
	private static final String keyPair = System.getenv("EC2_KEYPAIR");
	private static final List<String> securityGroups = Arrays
			.asList("webserver");

	// TODO: set to reveiver of alert emails
	private static final String emailRecipient = System.getenv("EMAIL_ADDRESS");
	// TODO: set to SMTP server to use for sending alert emails
	private static final String emailServer = System.getenv("EMAIL_SERVER");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPool pool = new BaseCloudPool(new SpotPoolDriver(
				new AwsSpotClient()));

		// set up configuration
		int instanceHourMargin = 0;
		ScaleOutConfig scaleUpConfig = new BaseCloudPoolConfig.ScaleOutConfig(
				size, image, keyPair, securityGroups, bootScript);
		ScaleInConfig scaleDownConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] cloud pool alert for spot-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 30;
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(
				new CloudPoolConfig("spot-cluster", spotClientConfig(0.0070,
						60, 60)), scaleUpConfig, scaleDownConfig, alertsConfig,
				poolUpdatePeriod);
		pool.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}

	protected static JsonObject spotClientConfig(double bidPrice,
			long bidReplacementPeriod, long danglingInstanceCleanupPeriod) {
		SpotPoolDriverConfig config = new SpotPoolDriverConfig(awsAccessKeyId,
				awsSecretAccessKey, region, bidPrice, bidReplacementPeriod,
				danglingInstanceCleanupPeriod);
		return JsonUtils.toJson(config).getAsJsonObject();
	}

}
