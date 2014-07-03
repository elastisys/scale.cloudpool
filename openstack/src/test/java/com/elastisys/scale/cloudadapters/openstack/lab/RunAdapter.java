package com.elastisys.scale.cloudadapters.openstack.lab;

import java.util.Arrays;
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
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudadapters.openstack.requests.lab.AbstractClient;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroup;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.StandardOpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that runs the OpenStack {@link CloudAdapter}, accepting
 * the desired pool size to be input via {@code stdin}.
 *
 *
 *
 */
public class RunAdapter extends AbstractClient {
	static Logger logger = LoggerFactory.getLogger(RunAdapter.class);

	// TODO: set launch configuration parameters for new group instances
	private static final String size = "m1.small";
	private static final String image = "Ubuntu Server 14.04 64 bit";
	private static final List<String> bootScript = Arrays.asList(
			"sudo apt-get update", "sudo apt-get install -y apache2");
	private static final String keyPair = System.getenv("OS_INSTANCE_KEYPAIR");
	private static final List<String> securityGroups = Arrays.asList("web");
	private static final boolean assignFloatingIp = true;

	// TODO: set to user name used to log in (over SSH) to machine instances
	private static final String loginUser = "ubuntu";
	// TODO: set to machine instance login key (private key of key pair used to
	// launch new instances)
	private static final String loginKeyPath = System
			.getenv("OS_INSTANCE_KEYPATH");
	// TODO: set to liveness test command
	private static final String livenessTestCommand = "sudo service apache2 status | grep 'is running'";

	// TODO: set to reveiver of alert emails
	private static final String emailRecipient = System.getenv("EMAIL_ADDRESS");
	// TODO: set to SMTP server to use for sending alert emails
	private static final String emailServer = System.getenv("EMAIL_SERVER");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		ScalingGroup openstackScalingGroup = new OpenStackScalingGroup(
				new StandardOpenstackClient());
		CloudAdapter adapter = new BaseCloudAdapter(openstackScalingGroup);

		// set up configuration
		int instanceHourMargin = 0;
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig(size, image, keyPair,
				securityGroups, bootScript);
		ScaleDownConfig scaleDownConfig = new BaseCloudAdapterConfig.ScaleDownConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		LivenessConfig liveness = new LivenessConfig(22, loginUser,
				loginKeyPath, new BootTimeLivenessCheck(livenessTestCommand,
						30, 15), new RunTimeLivenessCheck(livenessTestCommand,
						30, 3, 10));
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] scaling group alert for openstack-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 40;
		BaseCloudAdapterConfig config = new BaseCloudAdapterConfig(
				new ScalingGroupConfig("openstack-cluster",
						openstackScalingGroupConfig()), scaleUpConfig,
				scaleDownConfig, liveness, alertsConfig, poolUpdatePeriod);
		adapter.configure(JsonUtils.toJson(config).getAsJsonObject());

		MachinePoolPrinter poolPrinterTask = new MachinePoolPrinter(adapter);
		executorService.scheduleWithFixedDelay(poolPrinterTask, 30, 30,
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

	private static JsonObject openstackScalingGroupConfig() {
		OpenStackScalingGroupConfig clientConfig = new OpenStackScalingGroupConfig(
				getKeystoneEndpoint(), getRegionName(), getTenantName(),
				getUserName(), getPassword(), assignFloatingIp);
		return JsonUtils.toJson(clientConfig).getAsJsonObject();
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
