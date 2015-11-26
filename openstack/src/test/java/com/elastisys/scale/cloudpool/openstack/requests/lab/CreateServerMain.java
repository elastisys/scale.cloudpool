package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.Arrays;
import java.util.Map;

import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.requests.CreateServerRequest;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;

public class CreateServerMain {
	private static Logger LOG = LoggerFactory.getLogger(CreateServerMain.class);

	public static void main(String[] args) throws Exception {
		Map<String, String> metadata = ImmutableMap.of("elastisys:cloudPool",
				"cluster");
		String encodedUserData = Base64Utils.toBase64("#!/bin/bash",
				"sudo apt-get update -qy", "sudo apt-get install -qy apache2");

		HttpLoggingFilter.toggleLogging(true);
		CreateServerRequest request = new CreateServerRequest(
				new OSClientFactory(DriverConfigLoader.loadDefault()),
				"server1", "m1.small", "Ubuntu Server 14.04 64 bit",
				"instancekey", Arrays.asList("web"), encodedUserData, metadata);
		Server createdServer = request.call();
		LOG.info("created server: {}", createdServer);
	}
}
