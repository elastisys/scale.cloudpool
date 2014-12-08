package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import org.junit.Test;

import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.google.common.base.Optional;

public class TestAuthenticatedHttpClient {

	private final BasicCredentials correctBasicCredentials = new BasicCredentials(
			"admin", "adminpassword");
	private final CertificateCredentials correctCertificateCredentials = new CertificateCredentials(
			KeyStoreType.PKCS12, "/proc/cpuinfo", "somepassword");

	@Test
	public void testCorrectConstruction() {
		new AuthenticatedHttpClient(Optional.of(this.correctBasicCredentials),
				Optional.of(this.correctCertificateCredentials));
	}

	@Test
	public void testMissingCertificateCredentials() {
		new AuthenticatedHttpClient(Optional.of(this.correctBasicCredentials),
				Optional.<CertificateCredentials> absent());
	}

	@Test
	public void testMissingBasicCredentials() {
		new AuthenticatedHttpClient(Optional.<BasicCredentials> absent(),
				Optional.of(this.correctCertificateCredentials));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingBothCredentialTypes() {
		new AuthenticatedHttpClient(Optional.<BasicCredentials> absent(),
				Optional.<CertificateCredentials> absent());
	}
}
