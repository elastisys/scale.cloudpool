package com.elastisys.scale.cloudadapters.splitter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;

/**
 * Tests validation of {@link PrioritizedCloudAdapter}s created from different
 * combinations of parameters.
 */
public class TestPrioritizedCloudAdapterValidation {

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
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort, null,
				null);
		config.validate();
	}

	/**
	 * Certificate authentication.
	 */
	@Test
	public void testCertAuth() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort, null,
				this.correctCertificateCredentials);
		config.validate();
	}

	/**
	 * Basic authentication.
	 */
	@Test
	public void testBasicAuth() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	/**
	 * Basic and certificate authentication.
	 */
	@Test
	public void testBasicAndCertAuth() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials,
				this.correctCertificateCredentials);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPriorityTooLow() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(-1,
				this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPriorityTooHigh() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(101,
				this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncorrectPort() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, -1,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingHost() {
		PrioritizedCloudAdapter config = new PrioritizedCloudAdapter(
				this.correctPriority, null, this.correctPort,
				this.correctBasicCredentials, null);
		config.validate();
	}

	@Test
	public void testEquality() {
		PrioritizedCloudAdapter a = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		PrioritizedCloudAdapter b = new PrioritizedCloudAdapter(
				this.correctPriority, this.correctHost, this.correctPort,
				this.correctBasicCredentials, null);
		PrioritizedCloudAdapter c = new PrioritizedCloudAdapter(
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
