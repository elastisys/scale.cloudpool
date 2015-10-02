package com.elastisys.scale.cloudpool.aws.spot.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises the {@link SpotPoolDriverConfig} class.
 */
public class TestSpotPoolDriverConfig {

	@Test
	public void basicSanity() {
		SpotPoolDriverConfig config = new SpotPoolDriverConfig(
				"awsAccessKeyId", "awsSecretAccessKey", "us-east-1", 0.0050,
				120L, 60L);
		config.validate();

		assertThat(config.getAwsAccessKeyId(), is("awsAccessKeyId"));
		assertThat(config.getAwsSecretAccessKey(), is("awsSecretAccessKey"));
		assertThat(config.getRegion(), is("us-east-1"));
		assertThat(config.getBidPrice(), is(0.005000));
		assertThat(config.getBidReplacementPeriod(), is(120L));
		assertThat(config.getDanglingInstanceCleanupPeriod(), is(60L));
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

	/**
	 * If bid price is set too low, and gets rounded off to 0, validation must
	 * fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tooLowBidPrice() {
		configWithPrice(0.0000001).validate();
	}

	private SpotPoolDriverConfig configWithPrice(double bidPrice) {
		return new SpotPoolDriverConfig("awsAccessKeyId", "awsSecretAccessKey",
				"us-east-1", bidPrice, 120L, 60L);

	}
}
