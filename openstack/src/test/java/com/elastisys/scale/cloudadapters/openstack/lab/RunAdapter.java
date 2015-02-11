package com.elastisys.scale.cloudadapters.openstack.lab;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.MailServerSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudadapters.commons.util.cli.CloudadapterCommandLineDriver;
import com.elastisys.scale.cloudadapters.openstack.requests.lab.AbstractClient;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroup;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.StandardOpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the OpenStack {@link CloudAdapter} via
 * commands read from {@code stdin}.
 */
public class RunAdapter extends AbstractClient {
	static Logger LOG = LoggerFactory.getLogger(RunAdapter.class);

	// TODO: set launch configuration parameters for new group instances
	private static final String size = "m1.small";
	private static final String image = "Ubuntu Server 14.04 64 bit";
	private static final List<String> bootScript = Arrays.asList(
			"sudo apt-get update", "sudo apt-get install -y apache2");
	private static final String keyPair = System.getenv("OS_INSTANCE_KEYPAIR");
	private static final List<String> securityGroups = Arrays.asList("web");
	private static final boolean assignFloatingIp = true;

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
		AlertSettings alertsConfig = new AlertSettings(
				"[elastisys:scale] scaling group alert for openstack-cluster",
				Arrays.asList(emailRecipient), "noreply@elastisys.com",
				"INFO|NOTICE|WARN|ERROR|FATAL", new MailServerSettings(
						emailServer, 25, null, false));

		int poolUpdatePeriod = 40;
		BaseCloudAdapterConfig config = new BaseCloudAdapterConfig(
				new ScalingGroupConfig("openstack-cluster",
						openstackScalingGroupConfig()), scaleUpConfig,
				scaleDownConfig, alertsConfig, poolUpdatePeriod);
		JsonObject jsonConfig = JsonUtils.toJson(config).getAsJsonObject();
		LOG.info("setting config: {}", jsonConfig);
		adapter.configure(jsonConfig);

		new CloudadapterCommandLineDriver(adapter).start();

		executorService.shutdownNow();
	}

	private static JsonObject openstackScalingGroupConfig() {
		OpenStackScalingGroupConfig clientConfig = new OpenStackScalingGroupConfig(
				getKeystoneEndpoint(), getRegionName(), getTenantName(),
				getUserName(), getPassword(), assignFloatingIp);
		return JsonUtils.toJson(clientConfig).getAsJsonObject();
	}

}
