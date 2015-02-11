package com.elastisys.scale.cloudadapters.splitter;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.splitter.config.PoolSizeCalculator;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.config.SplitterConfig;
import com.elastisys.scale.cloudadapters.splitter.requests.RequestFactory;
import com.elastisys.scale.cloudadapters.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

public class TestConfiguringSplitter {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestConfiguringSplitter.class);

	private static final String COMPLETE_CONFIG = "splitterconfig/complete-config.json";
	private static final String DUPLICATE_ADAPTER_CONFIG = "splitterconfig/duplicate-adapters-config.json";
	private static final String MINIMAL_CONFIG = "splitterconfig/minimal-config.json";
	private static final String NO_CALCULATOR_CONFIG = "splitterconfig/no-calculator.json";
	private static final String NO_ADAPTERS_CONFIG = "splitterconfig/no-adapters.json";
	private static final String WRONG_SUM_CONFIG = "splitterconfig/wrong-sum.json";
	private static final String BASICAUTH_NO_PASS_CONFIG = "splitterconfig/basicauth-no-password.json";
	private static final String BASICAUTH_NO_USER_CONFIG = "splitterconfig/basicauth-no-username.json";
	private static final String CERTAUTH_NO_KEYSTORE_CONFIG = "splitterconfig/certauth-no-keystore.json";
	private static final String CERTAUTH_NO_KEYSTOREPASS_CONFIG = "splitterconfig/certauth-no-keystorepassword.json";

	/** object under test. */
	private Splitter splitter;

	@Before
	public void beforeTestMethod() {
		RequestFactory requestFactoryMock = mock(RequestFactory.class);
		this.splitter = new Splitter(requestFactoryMock);
	}

	/**
	 * Verify that by default, the {@link Splitter} uses a
	 * {@link HttpRequestFactory} to create cloud adapter requests.
	 */
	@Test
	public void defaultRequestFactory() {
		assertThat(new Splitter().getRequestFactory(),
				is(instanceOf(HttpRequestFactory.class)));
	}

	@Test
	public void getConfigSchema() {
		Optional<JsonObject> schema = this.splitter.getConfigurationSchema();
		assertTrue(schema.isPresent());
		assertThat(schema.get(), is(Splitter.CONFIG_SCHEMA));
	}

	@Test
	public void getConfigBeforeBeingSet() {
		Optional<JsonObject> absent = Optional.absent();
		assertThat(this.splitter.getConfiguration(), is(absent));
	}

	@Test
	public void completeConfig() {
		JsonObject configuration = JsonUtils.parseJsonResource(COMPLETE_CONFIG);
		this.splitter.configure(configuration);

		assertThat(this.splitter.getConfiguration().get(),
				is(JsonUtils.parseJsonResource(COMPLETE_CONFIG)));

		// verify contents of configuration
		SplitterConfig config = this.splitter.config();
		assertThat(config.getPoolSizeCalculator(),
				is(PoolSizeCalculator.STRICT));
		assertThat(config.getPoolUpdatePeriod(), is(30L));

		assertThat(config.getAdapters().size(), is(4));
		assertThat(config.getAdapters().get(0), is(new PrioritizedCloudAdapter(
				40, "localhost0", 10, null, null)));
		assertThat(config.getAdapters().get(1), is(new PrioritizedCloudAdapter(
				20, "localhost1", 11, new BasicCredentials("testuser1",
						"testpassword1"), null)));
		assertThat(config.getAdapters().get(2), is(new PrioritizedCloudAdapter(
				30, "localhost2", 12, null, new CertificateCredentials(
						KeyStoreType.PKCS12, "/proc/cpuinfo",
						"somekeystorepassword2"))));
		assertThat(config.getAdapters().get(3), is(new PrioritizedCloudAdapter(
				10, "localhost3", 13, new BasicCredentials("testuser3",
						"testpassword3"), new CertificateCredentials(
						KeyStoreType.PKCS12, "/proc/cpuinfo",
						"somekeystorepassword3", "somekeypassword3"))));
	}

	@Test
	public void reconfigure() {
		Optional<JsonObject> absent = Optional.absent();
		assertThat(this.splitter.getConfiguration(), is(absent));
		// configure
		JsonObject config = JsonUtils.parseJsonResource(COMPLETE_CONFIG);
		this.splitter.configure(config);
		assertThat(this.splitter.getConfiguration().get(),
				is(JsonUtils.parseJsonResource(COMPLETE_CONFIG)));
		// reconfigure
		JsonObject newConfig = JsonUtils.parseJsonResource(MINIMAL_CONFIG);
		this.splitter.configure(newConfig);
		assertThat(this.splitter.getConfiguration().get(),
				is(JsonUtils.parseJsonResource(MINIMAL_CONFIG)));
	}

	/**
	 * Verify default values for left out configuration fields.
	 */
	@Test
	public void minimalConfig() {
		JsonObject config = JsonUtils.parseJsonResource(MINIMAL_CONFIG);
		this.splitter.configure(config);

		assertThat(this.splitter.config().getPoolUpdatePeriod(),
				is(SplitterConfig.DEFAULT_POOL_UPDATE_PERIOD));
	}

	/**
	 * Priority calculation strategy is a mandatory field.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void noCalculatorConfig() {
		JsonObject config = JsonUtils.parseJsonResource(NO_CALCULATOR_CONFIG);
		this.splitter.configure(config);

	}

	/**
	 * Config must contain at least one adapter.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void noAdaptersConfig() {
		JsonObject config = JsonUtils.parseJsonResource(NO_ADAPTERS_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * Adapter priorities must add up to 100.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void wrongAdapterPrioritySum() {
		JsonObject config = JsonUtils.parseJsonResource(WRONG_SUM_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * Basic auth must specify username.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void basicAuthMissingUsername() {
		JsonObject config = JsonUtils
				.parseJsonResource(BASICAUTH_NO_USER_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * Basic auth must specify password.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void basicAuthMissingPassword() {
		JsonObject config = JsonUtils
				.parseJsonResource(BASICAUTH_NO_PASS_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * Cert auth must specify keystore.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void certAuthMissingKeystorePath() {
		JsonObject config = JsonUtils
				.parseJsonResource(CERTAUTH_NO_KEYSTORE_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * Cert auth must specify keystore pssword.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void certAuthMissingKeystorePassword() {
		JsonObject config = JsonUtils
				.parseJsonResource(CERTAUTH_NO_KEYSTOREPASS_CONFIG);
		this.splitter.configure(config);
	}

	/**
	 * A given adapter host and port combination is only allowed once.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void duplicateAdapters() {
		JsonObject config = JsonUtils
				.parseJsonResource(DUPLICATE_ADAPTER_CONFIG);
		this.splitter.configure(config);
	}

}
