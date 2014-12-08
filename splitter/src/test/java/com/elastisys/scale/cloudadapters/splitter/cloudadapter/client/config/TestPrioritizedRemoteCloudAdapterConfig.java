package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;

public class TestPrioritizedRemoteCloudAdapterConfig {

	private final BasicCredentials correctBasicCredentials = new BasicCredentials(
			"username", "password");
	private final CertificateCredentials correctCertificateCredentials = new CertificateCredentials(
			KeyStoreType.PKCS12, "/proc/cpuinfo", "somepassword");
	private final String correctHost = "localhost";
	private final int correctPort = 1234;
	private final int correctPriority = 100;

	@Test(expected = ConfigurationException.class)
	public void testMissingCredentials() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				null, 1234, 100, null, null);
		config.validate();
	}

	@Test
	public void testMissingBasicCredentials() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, 1234, 100, null,
				this.correctCertificateCredentials);
		config.validate();
	}

	@Test
	public void testMissingCertificateCredentials()
			throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, 1234, 100, this.correctBasicCredentials, null);
		config.validate();
	}

	@Test
	public void testBothCredentialsGiven() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, 1234, 100, this.correctBasicCredentials,
				this.correctCertificateCredentials);
		config.validate();
	}

	@Test(expected = ConfigurationException.class)
	public void testMissingBasicUsername() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = JsonUtils
				.toObject(
						JsonUtils
						.parseJsonResource("cloudadapter/missing-username.json"),
						PrioritizedRemoteCloudAdapterConfig.class);
		config.validate();
	}

	@Test(expected = ConfigurationException.class)
	public void testIncorrectPort() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, -1, this.correctPriority,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test
	public void testCorrectConfig() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, this.correctPort, this.correctPriority,
				this.correctBasicCredentials, null);
		config.validate();

		config = new PrioritizedRemoteCloudAdapterConfig(this.correctHost,
				this.correctPort, this.correctPriority, null,
				this.correctCertificateCredentials);
		config.validate();

		config = new PrioritizedRemoteCloudAdapterConfig(this.correctHost,
				this.correctPort, this.correctPriority,
				this.correctBasicCredentials,
				this.correctCertificateCredentials);
		config.validate();
	}

	@Test
	public void testEquality() {
		PrioritizedRemoteCloudAdapterConfig a = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, this.correctPort, this.correctPriority,
				this.correctBasicCredentials, null);
		PrioritizedRemoteCloudAdapterConfig b = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, this.correctPort, this.correctPriority,
				this.correctBasicCredentials, null);
		PrioritizedRemoteCloudAdapterConfig c = new PrioritizedRemoteCloudAdapterConfig(
				this.correctHost, this.correctPort + 1, this.correctPriority,
				this.correctBasicCredentials, null);
		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(b, a);
		assertFalse(a.equals(c));
		assertFalse(a.equals(null));
		assertFalse(a.equals(""));
		assertEquals(a.hashCode(), a.hashCode());
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.hashCode() == c.hashCode());
	}
}
