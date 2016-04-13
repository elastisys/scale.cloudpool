package com.elastisys.scale.cloudpool.juju.client;

import java.io.IOException;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.juju.config.JujuCloudPoolConfig;

/**
 * Interface for the {@link CloudPool} operations that we support in the Juju
 * environment. Since Juju does not have metadata support that allows us to tag
 * certain units and/or machines, we cannot support the full
 * <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/" target="_blank">
 * cloud pool API specification</a>.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 *
 */
public interface JujuClient {

	/**
	 * Configures the client according to the given configuration specification.
	 *
	 * @param config
	 *            The configuration specification.
	 * @throws IOException
	 *             Thrown if the configuration cannot be applied.
	 */
	public void configure(JujuCloudPoolConfig config) throws IOException;

	/**
	 * Sets the desired size of the Juju deployment.
	 *
	 * @param desiredSize
	 *            The desired size of the deployment.
	 */
	public void setDesiredSize(Integer desiredSize);

	/**
	 * @return The size of the deployment.
	 * @throws IOException
	 *             Thrown if there is an error getting the pool size.
	 */
	public PoolSizeSummary getPoolSize() throws IOException;

	/**
	 * @return The machine pool representing this deployment.
	 */
	public MachinePool getMachinePool();

}
