package com.elastisys.scale.cloudadapters.aws.autoscaling.lab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroup;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.BootTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.MailServerSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.RunTimeLivenessCheck;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Lab program that exercises the AWS Auto Scaling Group {@link CloudAdapter}
 * (driving a {@link AwsAsScalingGroup}).
 * <p/>
 * The program accepts pool size adjustments (read from {@code stdin}) and feeds
 * each pool size value to the {@link AwsAsScalingGroup}, which is asked to
 * carry out the corresponding pool adjustments. Each value is expected to be a
 * positive integer which is the pool. The program continues to run until
 * {@code stdin} is closed.
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

	// TODO: set to user name used to log in (over SSH) to machine instances
	private static final String loginUser = "ubuntu";
	// TODO: set to machine instance login key (private key of key pair used to
	// launch new instances)
	private static final String loginKeyPath = System
			.getenv("EC2_INSTANCE_KEY");;
	// TODO: set to liveness test command
	private static final String livenessTestCommand = "service apache2 status | grep 'is running'";

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
		LivenessConfig liveness = new LivenessConfig(22, loginUser,
				loginKeyPath, new BootTimeLivenessCheck(livenessTestCommand,
						15, 20), new RunTimeLivenessCheck(livenessTestCommand,
						60, 5, 20));
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] scaling group alert for " + autoScalingGroup,
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 45;
		BaseCloudAdapterConfig config = new BaseCloudAdapterConfig(
				scalingGroupConfig, scaleUpConfig, scaleDownConfig, liveness,
				alertsConfig, poolUpdatePeriod);
		adapter.configure(JsonUtils.toJson(config).getAsJsonObject());

		MachinePoolPrinter poolPrinterTask = new MachinePoolPrinter(adapter);
		executorService.scheduleWithFixedDelay(poolPrinterTask, 60, 60,
				TimeUnit.SECONDS);

		System.err.println("Input machine pool size (CTRL-D to exit) >> ");
		Scanner scanner = new Scanner(System.in);
		while (scanner.hasNext()) {
			try {
				int newPoolSize = scanner.nextInt();
				adapter.resizeMachinePool(newPoolSize);
			} catch (Exception e) {
				System.err.println("Failed to resize: " + e.getMessage());
				e.printStackTrace();
				scanner.next(); // ignore
			}
			System.err.println("Input machine pool size (CTRL-D to exit) >> ");
		}
		scanner.close();

		executorService.shutdownNow();
	}

	private static class MachinePoolPrinter implements Runnable {
		private final CloudAdapter adapter;

		public MachinePoolPrinter(CloudAdapter adapter) {
			this.adapter = adapter;
		}

		@Override
		public void run() {
			try {
				printMachinePool(this.adapter);
			} catch (CloudAdapterException e) {
				logger.error("failed to print machine pool: " + e.getMessage(),
						e);
			}
		}

		private void printMachinePool(CloudAdapter adapter)
				throws CloudAdapterException {
			MachinePool machinePool = adapter.getMachinePool();
			StringBuilder status = new StringBuilder("Machine pool at "
					+ machinePool.getTimestamp() + ":");
			if (machinePool.getMachines().isEmpty()) {
				status.append(" {}");
			} else {
				status.append("\n");
				for (Machine machine : machinePool.getMachines()) {
					status.append("  " + machine + "\n");
				}
			}
			logger.info(status.toString());
		}
	}
}
