package com.elastisys.scale.cloudpool.aws.ec2.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.cloudpool.aws.ec2.driver.client.AwsEc2Client;
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

/**
 * Simple lab program that exercises the EC2 {@link CloudPool} via commands read
 * from {@code stdin}.
 */
public class RunPool extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunPool.class);

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
		CloudPool pool = new BaseCloudPool(
				new Ec2PoolDriver(new AwsEc2Client()));

		// set up configuration
		int instanceHourMargin = 0;
		ScaleOutConfig scaleUpConfig = new BaseCloudPoolConfig.ScaleOutConfig(
				size, image, keyPair, securityGroups, bootScript);
		ScaleInConfig scaleDownConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] cloud pool alert for ec2-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 30;
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(
				new CloudPoolConfig("ec2-cluster", ec2ClientConfig()),
				scaleUpConfig, scaleDownConfig, alertsConfig, poolUpdatePeriod);
		pool.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}
}
