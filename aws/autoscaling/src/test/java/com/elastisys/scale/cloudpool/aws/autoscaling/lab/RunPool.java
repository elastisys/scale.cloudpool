package com.elastisys.scale.cloudpool.aws.autoscaling.lab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AwsAutoScalingClient;
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
 * Lab program that exercises the AWS Auto Scaling Group {@link CloudPool} via
 * commands read from {@code stdin}.
 * <p/>
 * Note that an Auto Scaling group with the specified name (
 * {@value #autoScalingGroup}) must exist in the selected region (
 * {@value #region}) before running the program.The
 * {@code CreateLoadBalancerMain}, {@code CreateLaunchConfigurationMain} and
 * {@code CreateAutoScalingGroupMain} lab programs could get you started.
 *
 */
public class RunPool extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunPool.class);

	// TODO: set to the Auto Scaling Group you wish to manage
	protected static final String autoScalingGroup = "end2end-scalinggroup";

	// TODO: set to reveiver of alert emails
	private static final String emailRecipient = System.getenv("EMAIL_ADDRESS");
	// TODO: set to SMTP server to use for sending alert emails
	private static final String emailServer = System.getenv("EMAIL_SERVER");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPool pool = new BaseCloudPool(new AwsAsPoolDriver(
				new AwsAutoScalingClient()));

		// set up configuration
		int instanceHourMargin = 0;
		List<String> empty = Collections.emptyList();
		// launch configuration parameters is already taken care of by
		// LaunchConfiguration set on the Auto Scaling Group.
		CloudPoolConfig scalingGroupConfig = new CloudPoolConfig(
				autoScalingGroup, awsClientConfig());
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("N/A", "N/A", "N/A",
				empty, empty);
		ScaleInConfig scaleDownConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] cloud pool alert for " + autoScalingGroup,
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 30;
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(
				scalingGroupConfig, scaleUpConfig, scaleDownConfig,
				alertsConfig, poolUpdatePeriod);
		pool.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}
}
