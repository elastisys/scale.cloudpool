package com.elastisys.scale.cloudpool.aws.spot.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link SpotPoolDriverConfig} class.
 */
public class TestSpotPoolDriverConfig {

	/**
	 * Test applying a configuration that provides values for all fields
	 * (mandatory and optional).
	 */
	@Test
	public void completeConfig() {
		double bidPrice = 0.0050;
		long bidReplacementPeriod = 120L;
		long danglingInstanceCleanupPeriod = 60L;
		int connectionTimeout = 7000;
		int socketTimeout = 5000;
		SpotPoolDriverConfig config = new SpotPoolDriverConfig("awsAccessKeyId",
				"awsSecretAccessKey", "us-east-1", bidPrice,
				bidReplacementPeriod, danglingInstanceCleanupPeriod,
				connectionTimeout, socketTimeout);
		config.validate();

		assertThat(config.getAwsAccessKeyId(), is("awsAccessKeyId"));
		assertThat(config.getAwsSecretAccessKey(), is("awsSecretAccessKey"));
		assertThat(config.getRegion(), is("us-east-1"));
		assertThat(config.getBidPrice(), is(0.005000));
		assertThat(config.getBidReplacementPeriod(), is(bidReplacementPeriod));
		assertThat(config.getDanglingInstanceCleanupPeriod(),
				is(danglingInstanceCleanupPeriod));
		assertThat(config.getConnectionTimeout(), is(connectionTimeout));
		assertThat(config.getSocketTimeout(), is(socketTimeout));
	}

	/**
	 * Test applying a configuration that leaves out optional values.
	 */
	@Test
	public void withDefaults() {
		SpotPoolDriverConfig config = new SpotPoolDriverConfig("awsAccessKeyId",
				"awsSecretAccessKey", "us-east-1", 0.0050, null, null);
		config.validate();

		assertThat(config.getBidReplacementPeriod(),
				is(SpotPoolDriverConfig.DEFAULT_BID_REPLACEMENT_PERIOD));
		assertThat(config.getDanglingInstanceCleanupPeriod(), is(
				SpotPoolDriverConfig.DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD));
		assertThat(config.getConnectionTimeout(),
				is(SpotPoolDriverConfig.DEFAULT_CONNECTION_TIMEOUT));
		assertThat(config.getSocketTimeout(),
				is(SpotPoolDriverConfig.DEFAULT_SOCKET_TIMEOUT));
	}

	/**
	 * Amazon rounds off their bid prices to six decimals.
	 */
	@Test
	public void limitBidPricePrecision() {
		assertThat(configWithPrice(0.0123456789).getBidPrice(), is(0.012346));
		assertThat(configWithPrice(0.123456789).getBidPrice(), is(0.123457));
		assertThat(configWithPrice(0.123456789).getBidPrice(), is(0.123457));
		assertThat(configWithPrice(0.0015103472457080748).getBidPrice(),
				is(0.001510));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingAwsAccessKeyId() {
		new SpotPoolDriverConfig(null, "awsSecretAccessKey", "us-east-1",
				0.0070, null, null).validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingAwsSecretAccessKey() {
		new SpotPoolDriverConfig("awsAccessKeyId", null, "us-east-1", 0.0070,
				null, null).validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingRegion() {
		new SpotPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey", null,
				0.0070, null, null).validate();
	}

	/**
	 * If bid price is set too low, and gets rounded off to 0, validation must
	 * fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tooLowBidPrice() {
		configWithPrice(0.0000001).validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalConnectionTimeout() {
		new SpotPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey",
				"us-east-1", 0.0070, null, null, 0, 5000).validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalSocketTimeout() {
		new SpotPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey",
				"us-east-1", 0.0070, null, null, 5000, 0).validate();
	}

	private SpotPoolDriverConfig configWithPrice(double bidPrice) {
		return new SpotPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey",
				"us-east-1", bidPrice, 120L, 60L);

	}
}
