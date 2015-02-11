package com.elastisys.scale.cloudadapers.api.types;

import static com.google.common.base.Preconditions.checkArgument;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Response message of the {@link CloudAdapter#getPoolSize()} operation that
 * returns information about the pool size -- both desired and actual size.
 */
public class PoolSizeSummary {

	/** The desired size of the machine pool. */
	private final int desiredSize;
	/**
	 * The number of allocated machines in the pool (see
	 * {@link Machine#isAllocated()}.
	 */
	private final int allocated;
	/**
	 * The number of machines in the pool that are marked
	 * {@link ServiceState#OUT_OF_SERVICE}.
	 */
	private final int outOfService;

	public PoolSizeSummary(int desiredSize, int allocated, int outOfService) {
		checkArgument(desiredSize >= 0, "desiredSize must be >= 0");
		checkArgument(allocated >= 0, "allocated must be >= 0");
		checkArgument(outOfService >= 0, "outOfService must be >= 0");

		checkArgument(allocated >= outOfService,
				"outOfService cannot be greater than allocated");

		this.desiredSize = desiredSize;
		this.allocated = allocated;
		this.outOfService = outOfService;
	}

	/**
	 * Returns the desired size of the machine pool.
	 *
	 * @return
	 */
	public int getDesiredSize() {
		return this.desiredSize;
	}

	/**
	 * Returns the number of allocated machines in the pool (see
	 * {@link Machine#isAllocated()}.
	 *
	 * @return
	 */
	public int getAllocated() {
		return this.allocated;
	}

	/**
	 * Returns the number of machines in the pool that are marked
	 * {@link ServiceState#OUT_OF_SERVICE}.
	 *
	 * @return
	 */
	public int getOutOfService() {
		return this.outOfService;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.desiredSize, this.allocated,
				this.outOfService);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PoolSizeSummary) {
			PoolSizeSummary that = (PoolSizeSummary) obj;
			return Objects.equal(this.desiredSize, that.desiredSize)
					&& Objects.equal(this.allocated, that.allocated)
					&& Objects.equal(this.outOfService, that.outOfService);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("desiredSize", this.desiredSize)
				.add("allocated", this.allocated)
				.add("outOfService", this.outOfService).toString();
	}
}
