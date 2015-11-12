package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Exercises the {@link TimeInterval} class.
 */
public class TestTimeInterval {

	@Test
	public void basicSanity() {
		// same value, different constructors
		TimeInterval interval1 = new TimeInterval(10L, "seconds");
		TimeInterval interval2 = new TimeInterval(10L, TimeUnit.SECONDS);

		assertThat(interval1.getTime(), is(10L));
		assertThat(interval1.getUnit(), is(TimeUnit.SECONDS));
		assertThat(interval1, is(interval2));
	}

	/**
	 * Should be allowed to give zero as duration.
	 */
	@Test
	public void withZeroDuration() {
		new TimeInterval(0L, TimeUnit.MINUTES);
	}

	/**
	 * Try out all recognized time units.
	 */
	@Test
	public void withDifferentUnits() {
		assertThat(new TimeInterval(10L, "nanoseconds").getUnit(),
				is(TimeUnit.NANOSECONDS));

		assertThat(new TimeInterval(10L, "microseconds").getUnit(),
				is(TimeUnit.MICROSECONDS));
		assertThat(new TimeInterval(10L, "milliseconds").getUnit(),
				is(TimeUnit.MILLISECONDS));
		assertThat(new TimeInterval(10L, "seconds").getUnit(),
				is(TimeUnit.SECONDS));
		assertThat(new TimeInterval(10L, "minutes").getUnit(),
				is(TimeUnit.MINUTES));
		assertThat(new TimeInterval(10L, "hours").getUnit(),
				is(TimeUnit.HOURS));
		assertThat(new TimeInterval(10L, "days").getUnit(), is(TimeUnit.DAYS));
	}

	/**
	 * Time unit should be case insensitive.
	 */
	@Test
	public void caseInsensitive() {
		assertThat(new TimeInterval(10L, "SeConDs").getUnit(),
				is(TimeUnit.SECONDS));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullTime() {
		new TimeInterval(null, TimeUnit.SECONDS);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullTimeUnit() {
		new TimeInterval(10L, (TimeUnit) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullUnitAsString() {
		new TimeInterval(10L, (String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithUnrecognizedUnit() {
		new TimeInterval(10L, "months");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNegativeDuration() {
		new TimeInterval(-1L, TimeUnit.SECONDS);
	}

}
