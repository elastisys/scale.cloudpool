package com.elastisys.scale.cloudadapters.aws.ec2.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.Ec2ScalingGroup;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.AwsEc2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.MailServerSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudadapters.commons.util.cli.CloudadapterCommandLineDriver;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Simple lab program that exercises the EC2 {@link CloudAdapter} via commands
 * read from {@code stdin}.
 */
public class RunAdapter extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunAdapter.class);

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

		CloudAdapter adapter = new BaseCloudAdapter(new Ec2ScalingGroup(
				new AwsEc2Client()));

		// set up configuration
		int instanceHourMargin = 0;
		ScaleUpConfig scaleUpConfig = new BaseCloudAdapterConfig.ScaleUpConfig(
				size, image, keyPair, securityGroups, bootScript);
		ScaleDownConfig scaleDownConfig = new ScaleDownConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] scaling group alert for ec2-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 30;
		BaseCloudAdapterConfig config = new BaseCloudAdapterConfig(
				new ScalingGroupConfig("ec2-cluster", ec2ClientConfig()),
				scaleUpConfig, scaleDownConfig, alertsConfig, poolUpdatePeriod);
		adapter.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudadapterCommandLineDriver(adapter).start();

		executorService.shutdownNow();
	}
}
