package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;

/**
 * Exercise {@link ScaleOutConfig}.
 */
public class TestScaleOutConfig {

	@Test
	public void basicSanity() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update");
		List<String> securityGroups = Arrays.asList("webserver");
		ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567",
				"mykeypair", securityGroups, bootScript);
		config.validate();

		assertThat(config.getSize(), is("m1.small"));
		assertThat(config.getImage(), is("ami-124567"));
		assertThat(config.getKeyPair(), is("mykeypair"));
		assertThat(config.getSecurityGroups(), is(securityGroups));
		assertThat(config.getBootScript(), is(bootScript));

	}

	@Test(expected = CloudPoolException.class)
	public void missingSize() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update");
		List<String> securityGroups = Arrays.asList("webserver");
		ScaleOutConfig config = new ScaleOutConfig(null, "ami-124567",
				"mykeypair", securityGroups, bootScript);
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingImage() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update");
		List<String> securityGroups = Arrays.asList("webserver");
		ScaleOutConfig config = new ScaleOutConfig("m1.small", null,
				"mykeypair", securityGroups, bootScript);
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingKeypair() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update");
		List<String> securityGroups = Arrays.asList("webserver");
		ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567",
				null, securityGroups, bootScript);
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingSecurityGroups() {
		List<String> bootScript = Arrays.asList("#!/bin/bash",
				"sudo apt-get update");
		List<String> securityGroups = null;
		ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567",
				"mykeypair", securityGroups, bootScript);
		config.validate();
	}

	@Test(expected = CloudPoolException.class)
	public void missingBootscript() {
		List<String> bootScript = null;
		List<String> securityGroups = Arrays.asList("webserver");
		ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567",
				"mykeypair", securityGroups, bootScript);
		config.validate();
	}

}
