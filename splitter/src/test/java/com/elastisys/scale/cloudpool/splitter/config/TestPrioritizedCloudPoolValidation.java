package com.elastisys.scale.cloudpool.splitter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;

/**
 * Tests validation of {@link PrioritizedCloudPool}s created from different
 * combinations of parameters.
 */
public class TestPrioritizedCloudPoolValidation {

	private final BasicCredentials correctBasicCredentials = new BasicCredentials(
			"username", "password");
	private final CertificateCredentials correctCertificateCredentials = new CertificateCredentials(
			KeyStoreType.PKCS12, "/proc/cpuinfo", "somepassword");
	private final String correctHost = "localhost";
	private final int correctPort = 1234;
	private final int correctPriority = 100;

	/**
	 * No authentication.
	 */
	@Test
	public void testNoAuth() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort, null,
				null);
		config.validate();
	}

	/**
	 * Certificate authentication.
	 */
	@Test
	public void testCertAuth() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort, null,
				this.correctCertificateCredentials);
		config.validate();
	}

	/**
	 * Basic authentication.
	 */
	@Test
	public void testBasicAuth() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	/**
	 * Basic and certificate authentication.
	 */
	@Test
	public void testBasicAndCertAuth() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials,
				this.correctCertificateCredentials);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPriorityTooLow() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(-1,
				this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPriorityTooHigh() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(101,
				this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncorrectPort() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, -1,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingHost() {
		PrioritizedCloudPool config = new PrioritizedCloudPool(
				this.correctPriority, null, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test
	public void testEquality() {
		PrioritizedCloudPool a = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		PrioritizedCloudPool b = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		PrioritizedCloudPool c = new PrioritizedCloudPool(
				this.correctPriority, this.correctHost, this.correctPort + 1,
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
