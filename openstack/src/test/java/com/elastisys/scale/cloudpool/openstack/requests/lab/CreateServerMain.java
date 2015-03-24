package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.requests.CreateServerRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class CreateServerMain {
	private static Logger LOG = LoggerFactory.getLogger(CreateServerMain.class);

	public static void main(String[] args) throws Exception {
		Map<String, String> metadata = ImmutableMap.of("elastisys:cloudPool",
				"cluster");
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update", "sudo apt-get install -y apache2");
		String userData = Joiner.on("\n").join(bootScript);

		HttpLoggingFilter.toggleLogging(true);
		CreateServerRequest request = new CreateServerRequest(
				DriverConfigLoader.loadDefault(), "server1", "m1.small",
				"Ubuntu Server 14.04 64 bit", "instancekey",
				Arrays.asList("web"), Optional.of(userData), metadata);
		Server createdServer = request.call();
		LOG.info("created server: {}", createdServer);
	}
}
