package com.elastisys.scale.cloudpool.splitter.config;

import com.elastisys.scale.cloudpool.splitter.poolcalculators.PoolSizeCalculationStrategy;
import com.elastisys.scale.cloudpool.splitter.poolcalculators.StrictPoolSizeCalculationStrategy;

/**
 * The valid pool size calculation strategies recognized in the configuration.
 * 
 * @see SplitterConfig
 */
public enum PoolSizeCalculator {
	STRICT(StrictPoolSizeCalculationStrategy.INSTANCE);

	/** The calculation strategy to use for this {@link PoolSizeCalculator}. */
	private final PoolSizeCalculationStrategy calculationStrategy;

	private PoolSizeCalculator(PoolSizeCalculationStrategy calculationStrategy) {
		this.calculationStrategy = calculationStrategy;
	}

	/**
	 * @return The configured pool size calculation strategy.
	 */
	public PoolSizeCalculationStrategy getCalculationStrategy() {
		return this.calculationStrategy;
	}
}
