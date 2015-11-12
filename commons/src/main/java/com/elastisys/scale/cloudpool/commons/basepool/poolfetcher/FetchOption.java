package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher;

/**
 * Options that can be passed to a {@link PoolFetcher} call to indicate a
 * certain pool fetch behavior.
 */
public enum FetchOption {

	/**
	 * Force the {@link PoolFetcher} to refresh its view of the pool members
	 * (and not return cached data).
	 */
	FORCE_REFRESH;
}
