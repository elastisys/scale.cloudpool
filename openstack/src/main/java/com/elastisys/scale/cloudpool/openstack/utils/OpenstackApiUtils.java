package com.elastisys.scale.cloudpool.openstack.utils;

import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;

import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriverConfig;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

public class OpenstackApiUtils {

	public static NovaApi getNativeApi(
			OpenStackPoolDriverConfig account) {
		Properties overrides = buildOverrideProperties();
		// Openstack login identity becomes: "tentantId:username"
		String identity = account.getTenantName() + ":" + account.getUserName();
		String password = account.getPassword();

		ImmutableSet<Module> modules = ImmutableSet
				.<Module> of(new SLF4JLoggingModule());
		return ContextBuilder.newBuilder(new NovaApiMetadata())
				.endpoint(account.getKeystoneEndpoint()).modules(modules)
				.credentials(identity, password).overrides(overrides)
				.buildApi(NovaApi.class);
	}

	/**
	 * Returns override properties for the {@code openstack-nova} cloud provider
	 * to pass to the jclouds {@link ComputeServiceContext} .
	 * 
	 * @return Override properties
	 */
	private static Properties buildOverrideProperties() {
		Properties properties = new Properties();
		// Naively trust all server certificates
		properties.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
		// Login using user/password credentials
		properties.setProperty(KeystoneProperties.CREDENTIAL_TYPE,
				"passwordCredentials");
		// Timeout (in milliseconds) on establishing SSH connections
		properties.setProperty(ComputeServiceProperties.TIMEOUT_PORT_OPEN,
				"20000");
		return properties;
	}
}
