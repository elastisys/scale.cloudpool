package com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.google.common.util.concurrent.Atomics;

public class MockPrioritizedRemoteCloudAdapter implements
		PrioritizedRemoteCloudAdapter {

	private static final AtomicInteger idGenerator = new AtomicInteger();

	protected AtomicReference<PrioritizedRemoteCloudAdapterConfig> configuration;
	protected AtomicReference<MachinePool> machinePool;
	protected final int id;

	public MockPrioritizedRemoteCloudAdapter() {
		this.id = idGenerator.incrementAndGet();
		this.configuration = Atomics.newReference();
		this.machinePool = Atomics.newReference();
	}

	public MockPrioritizedRemoteCloudAdapter(
			PrioritizedRemoteCloudAdapterConfig configuration,
			MachinePool machinePool) {
		this();
		this.configuration = Atomics.newReference(configuration);
		this.machinePool = Atomics.newReference(machinePool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		return result;
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
		MockPrioritizedRemoteCloudAdapter other = (MockPrioritizedRemoteCloudAdapter) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(PrioritizedRemoteCloudAdapter o) {
		return -1 * Integer.compare(getPriority(), o.getPriority());
	}

	@Override
	public void configure(PrioritizedRemoteCloudAdapterConfig configuration)
			throws ConfigurationException {
		this.configuration.set(configuration);
	}

	@Override
	public int getPriority() {
		return this.configuration.get().getPriority();
	}

	@Override
	public MachinePool getMachinePool() throws CloudAdapterException {
		return this.machinePool.get();
	}

	@Override
	public void resizeMachinePool(long desiredCapacity)
			throws IllegalArgumentException, CloudAdapterException {
		this.machinePool.set(generateMachinePool(desiredCapacity));
	}

	public static MachinePool generateMachinePool(final long size) {
		List<Machine> machines = new ArrayList<Machine>((int) size);
		for (int i = 0; i < size; i++) {
			machines.add(new Machine("i-" + i, MachineState.RUNNING,
					new DateTime(), null, null, null));
		}
		return new MachinePool(machines, new DateTime());
	}

	/**
	 * @return the configuration
	 */
	public PrioritizedRemoteCloudAdapterConfig getConfiguration() {
		return this.configuration.get();
	}

	/**
	 * @param configuration
	 *            the configuration to set
	 */
	public void setConfiguration(
			PrioritizedRemoteCloudAdapterConfig configuration) {
		this.configuration.set(configuration);
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @param machinePool
	 *            the machinePool to set
	 */
	public void setMachinePool(MachinePool machinePool) {
		this.machinePool.set(machinePool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MockPrioritizedRemoteCloudAdapter [id=" + this.id
				+ ", poolSize=" + this.machinePool.get().getMachines().size()
				+ "]";
	}
}
