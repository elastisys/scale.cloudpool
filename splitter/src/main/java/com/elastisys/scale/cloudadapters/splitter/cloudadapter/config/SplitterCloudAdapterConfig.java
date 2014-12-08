package com.elastisys.scale.cloudadapters.splitter.cloudadapter.config;

import java.util.Objects;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.SplitterCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators.PoolSizeCalculationStrategy;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * <p>
 * The configuration for the Splitter cloud adapter. It consists of some
 * Splitter-specific configuration, and the configuration sets for each
 * individual adapter it should split the machine pool between.
 * </p>
 * <p>
 * Adapter configurations are prioritized [0, 100]. See the adapter's
 * documentation for details on how it chooses which adapter to use.
 * </p>
 *
 * <p>
 * This class is thread-safe by virtue of being immutable.
 * </p>
 *
 * @see SplitterCloudAdapter
 */
public class SplitterCloudAdapterConfig {

	/** The remote cloud adapters */
	private final ImmutableList<PrioritizedRemoteCloudAdapterConfig> adapters;

	/** The pool size calculator */
	private final PoolSizeCalculator poolSizeCalculator;

	/**
	 * Creates a new instance of the configuration object.
	 *
	 * @param adapters
	 *            A list of prioritized remote cloud adapter configuration
	 *            objects.
	 */
	public SplitterCloudAdapterConfig(
			ImmutableList<PrioritizedRemoteCloudAdapterConfig> adapters,
			PoolSizeCalculator calculator) {
		Preconditions.checkNotNull(adapters, "Adapters cannot be null");
		Preconditions.checkNotNull(calculator,
				"Pool size calculator cannot be null");

		this.adapters = adapters;
		this.poolSizeCalculator = calculator;
	}

	/**
	 * Validates the configuration.
	 *
	 * @throws ConfigurationException
	 *             Thrown if there is a configuration error, see message.
	 */
	public void validate() throws ConfigurationException {
		if (this.adapters == null) {
			// FIXME JSON parsing can lead to this being null
			throw new ConfigurationException("No cloud adapters configured!");
		}
		int sum = 0;
		for (PrioritizedRemoteCloudAdapterConfig adapterConfig : this.adapters) {
			adapterConfig.validate();
			sum += adapterConfig.getPriority();
		}
		if (sum != 100) {
			throw new ConfigurationException(
					"Sums of priorities does not equal 100");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.poolSizeCalculator, this.adapters);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SplitterCloudAdapterConfig other = (SplitterCloudAdapterConfig) obj;
		return Objects
				.equals(this.poolSizeCalculator, other.poolSizeCalculator)
				&& Objects.equals(this.adapters, other.adapters);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SplitterCloudAdapterConfig [adapters="
				+ Objects.toString(this.adapters) + ", poolSizeCalculator="
				+ Objects.toString(this.poolSizeCalculator) + "]";
	}

	/**
	 * @return The configured adapters.
	 */
	public ImmutableList<PrioritizedRemoteCloudAdapterConfig> getAdapterConfigurations() {
		return this.adapters;
	}

	/**
	 * @return The configured pool size calculation strategy.
	 */
	public PoolSizeCalculationStrategy getPoolSizeCalculatorStrategy() {
		return this.poolSizeCalculator.getCalculationStrategy();
	}
}