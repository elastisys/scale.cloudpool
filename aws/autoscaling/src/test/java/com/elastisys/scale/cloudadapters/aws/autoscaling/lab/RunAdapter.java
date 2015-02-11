package com.elastisys.scale.cloudadapters.aws.autoscaling.lab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroup;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AwsAutoScalingClient;
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
 * Lab program that exercises the AWS Auto Scaling Group {@link CloudAdapter}
 * via commands read from {@code stdin}.
 * <p/>
 * Note that an Auto Scaling group with the specified name (
 * {@value #autoScalingGroup}) must exist in the selected region (
 * {@value #region}) before running the program.The
 * {@code CreateLoadBalancerMain}, {@code CreateLaunchConfigurationMain} and
 * {@code CreateAutoScalingGroupMain} lab programs could get you started.
 *
 */
public class RunAdapter extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunAdapter.class);

	// TODO: set to the Auto Scaling Group you wish to manage
	protected static final String autoScalingGroup = "end2end-scalinggroup";

	// TODO: set to reveiver of alert emails
	private static final String emailRecipient = System.getenv("EMAIL_ADDRESS");
	// TODO: set to SMTP server to use for sending alert emails
	private static final String emailServer = System.getenv("EMAIL_SERVER");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudAdapter adapter = new BaseCloudAdapter(new AwsAsScalingGroup(
				new AwsAutoScalingClient()));

		// set up configuration
		int instanceHourMargin = 0;
		List<String> empty = Collections.emptyList();
		// launch configuration parameters is already taken care of by
		// LaunchConfiguration set on the Auto Scaling Group.
		ScalingGroupConfig scalingGroupConfig = new ScalingGroupConfig(
				autoScalingGroup, awsClientConfig());
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("N/A", "N/A", "N/A",
				empty, empty);
		ScaleDownConfig scaleDownConfig = new ScaleDownConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] scaling group alert for " + autoScalingGroup,
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 30;
		BaseCloudAdapterConfig config = new BaseCloudAdapterConfig(
				scalingGroupConfig, scaleUpConfig, scaleDownConfig,
				alertsConfig, poolUpdatePeriod);
		adapter.configure(JsonUtils.toJson(config).getAsJsonObject());

		new CloudadapterCommandLineDriver(adapter).start();

		executorService.shutdownNow();
	}
}
