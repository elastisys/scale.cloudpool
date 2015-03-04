package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.scriptbuilder.domain.OsFamily;

import com.elastisys.scale.cloudpool.openstack.requests.CreateServerRequest;
import com.elastisys.scale.cloudpool.openstack.utils.jclouds.ScriptUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class CreateServerMain extends AbstractClient {

	public static void main(String[] args) throws Exception {
		Map<String, String> metadata = ImmutableMap.of("scaling-group",
				"cluster");
		List<String> bootScript = Arrays.asList("sudo apt-get update",
				"sudo apt-get install -y apache2");
		String userData = ScriptUtils.renderScript(
				Joiner.on("\n").join(bootScript), OsFamily.UNIX);

		CreateServerRequest request = new CreateServerRequest(
				getAccountConfig(), "server1", "m1.small",
				"Ubuntu Server 12.04", "openstack_p11", Arrays.asList("web"),
				Optional.of(userData), metadata);
		Server createdServer = request.call();
		logger.info("created server: {}", createdServer);
	}
}
