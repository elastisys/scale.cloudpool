package com.elastisys.scale.cloudadapters.splitter.cloudadapter.config;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators.PoolSizeCalculationStrategy;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators.StrictPoolSizeCalculationStrategy;

/**
 * The valid pool size calculation strategies recognized in the configuration.
 */
public enum PoolSizeCalculator {
	/** @see StrictPoolSizeCalculationStrategy */
	STRICT(StrictPoolSizeCalculationStrategy.INSTANCE);

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
