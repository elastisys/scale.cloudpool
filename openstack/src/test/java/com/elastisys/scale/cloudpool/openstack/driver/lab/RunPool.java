package com.elastisys.scale.cloudpool.openstack.driver.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.MailServerSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.StandardOpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.cloudpool.openstack.requests.lab.DriverConfigLoader;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the OpenStack {@link CloudPool} via
 * commands read from {@code stdin}.
 */
public class RunPool {
	static Logger LOG = LoggerFactory.getLogger(RunPool.class);

	// TODO: set launch configuration parameters for new group instances
	private static final String size = "m1.small";
	private static final String image = "Ubuntu Server 14.04 64 bit";
	private static final List<String> bootScript = Arrays.asList("#!/bin/bash",
			"sudo apt-get update", "sudo apt-get install -y apache2");
	private static final String keyPair = System.getenv("OS_INSTANCE_KEYPAIR");
	private static final List<String> securityGroups = Arrays.asList("web");
	// TODO: set to reveiver of alert emails
	private static final String emailRecipient = System.getenv("EMAIL_ADDRESS");
	// TODO: set to SMTP server to use for sending alert emails
	private static final String emailServer = System.getenv("EMAIL_SERVER");

	private static final ScheduledExecutorService executorService = Executors
			.newScheduledThreadPool(10);

	public static void main(String[] args) throws Exception {
		CloudPoolDriver openstackScalingGroup = new OpenStackPoolDriver(
				new StandardOpenstackClient());
		CloudPool pool = new BaseCloudPool(openstackScalingGroup);

		// set up configuration
		int instanceHourMargin = 0;
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig(size, image, keyPair,
				securityGroups, bootScript);
		ScaleInConfig scaleDownConfig = new BaseCloudPoolConfig.ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR,
				instanceHourMargin);
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] cloud pool alert for openstack-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 15;

		CloudPoolConfig cloudPoolConfig = cloudPoolConfig("openstack-cluster",
				DriverConfigLoader.loadDefault());
		BaseCloudPoolConfig config = new BaseCloudPoolConfig(cloudPoolConfig,
				scaleUpConfig, scaleDownConfig, alertsConfig, poolUpdatePeriod);
		JsonObject jsonConfig = JsonUtils.toJson(config).getAsJsonObject();
		pool.configure(jsonConfig);

		new CloudPoolCommandLineDriver(pool).start();

		executorService.shutdownNow();
	}

	private static CloudPoolConfig cloudPoolConfig(String cloudPoolName,
			OpenStackPoolDriverConfig driverConfig) {
		return new CloudPoolConfig(cloudPoolName, JsonUtils
				.toJson(driverConfig).getAsJsonObject());
	}
}
