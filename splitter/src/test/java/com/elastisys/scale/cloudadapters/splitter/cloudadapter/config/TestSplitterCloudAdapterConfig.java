package com.elastisys.scale.cloudadapters.splitter.cloudadapter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators.StrictPoolSizeCalculationStrategy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.google.common.collect.ImmutableList;

public class TestSplitterCloudAdapterConfig {

	@Test(expected = NullPointerException.class)
	public void testConstructorMissingAdapters() {
		new SplitterCloudAdapterConfig(null, PoolSizeCalculator.STRICT);
	}

	@Test(expected = NullPointerException.class)
	public void testConstructorMissingCalculator() {
		new SplitterCloudAdapterConfig(
				ImmutableList.of(new PrioritizedRemoteCloudAdapterConfig(null,
						0, 0, null, null)), null);
	}

	@Test
	public void testConstructorWorking() {
		new SplitterCloudAdapterConfig(
				ImmutableList.of(new PrioritizedRemoteCloudAdapterConfig(null,
						0, 0, null, null)), PoolSizeCalculator.STRICT);
	}

	@Test
	public void testCorrectConfig() throws ConfigurationException {
		SplitterCloudAdapterConfig config = loadCloudAdapterConfig("splitterconfig/correct-config.json");

		config.validate();

		assertEquals(StrictPoolSizeCalculationStrategy.INSTANCE,
				config.getPoolSizeCalculatorStrategy());

		assertEquals(4, config.getAdapterConfigurations().size());
		ImmutableList<PrioritizedRemoteCloudAdapterConfig> adapterConfigurations = config
				.getAdapterConfigurations();

		assertEquals(40, adapterConfigurations.get(0).getPriority());
		assertEquals(10, adapterConfigurations.get(0).getCloudAdapterPort());
		assertEquals("localhost0", adapterConfigurations.get(0)
				.getCloudAdapterHost());
		assertEquals(new BasicCredentials("testuser0", "testpassword0"),
				adapterConfigurations.get(0).getBasicCredentials().get());
		assertNull(adapterConfigurations.get(0).getCertificateCredentials()
				.orNull());

		assertEquals(20, adapterConfigurations.get(1).getPriority());
		assertEquals(11, adapterConfigurations.get(1).getCloudAdapterPort());
		assertEquals("localhost1", adapterConfigurations.get(1)
				.getCloudAdapterHost());
		assertEquals(new BasicCredentials("testuser1", "testpassword1"),
				adapterConfigurations.get(1).getBasicCredentials().get());
		assertNull(adapterConfigurations.get(1).getCertificateCredentials()
				.orNull());

		assertEquals(20, adapterConfigurations.get(2).getPriority());
		assertEquals(12, adapterConfigurations.get(2).getCloudAdapterPort());
		assertEquals("localhost2", adapterConfigurations.get(2)
				.getCloudAdapterHost());
		assertNull(adapterConfigurations.get(2).getBasicCredentials().orNull());
		assertEquals(new CertificateCredentials(KeyStoreType.PKCS12,
				"/proc/cpuinfo", "somekeystorepassword2"),
				adapterConfigurations.get(2).getCertificateCredentials().get());

		assertEquals(20, adapterConfigurations.get(3).getPriority());
		assertEquals(13, adapterConfigurations.get(3).getCloudAdapterPort());
		assertEquals("localhost3", adapterConfigurations.get(3)
				.getCloudAdapterHost());
		assertEquals(new BasicCredentials("testuser3", "testpassword3"),
				adapterConfigurations.get(3).getBasicCredentials().get());
		assertEquals(new CertificateCredentials(KeyStoreType.PKCS12,
				"/proc/cpuinfo", "somekeystorepassword3", "somekeypassword3"),
				adapterConfigurations.get(3).getCertificateCredentials().get());
	}

	@Test(expected = ConfigurationException.class)
	public void testWrongPrioritySum() throws ConfigurationException {
		SplitterCloudAdapterConfig config = loadCloudAdapterConfig("splitterconfig/wrong-sum.json");
		config.validate();
	}

	@Test(expected = ConfigurationException.class)
	public void testNoAdapters() throws ConfigurationException {
		SplitterCloudAdapterConfig config = loadCloudAdapterConfig("splitterconfig/no-adapters.json");
		config.validate();
	}

	@Test
	public void testEquality() {
		SplitterCloudAdapterConfig a = loadCloudAdapterConfig("splitterconfig/correct-config.json");
		SplitterCloudAdapterConfig b = loadCloudAdapterConfig("splitterconfig/correct-config.json");
		SplitterCloudAdapterConfig c = loadCloudAdapterConfig("splitterconfig/no-adapters.json");
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

	private SplitterCloudAdapterConfig loadCloudAdapterConfig(
			String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				SplitterCloudAdapterConfig.class);
	}
}
