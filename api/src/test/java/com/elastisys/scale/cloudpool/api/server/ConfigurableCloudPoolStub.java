package com.elastisys.scale.cloudpool.api.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} stub that only supports setting/getting the configuration
 * and starting/stopping.
 */
class ConfigurableCloudPoolStub implements CloudPool {

    private JsonObject config = null;
    private boolean started = false;

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        this.config = configuration;
    }

    @Override
    public Optional<JsonObject> getConfiguration() {
        return Optional.fromNullable(this.config);
    }

    @Override
    public void start() throws IllegalStateException {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public CloudPoolStatus getStatus() {
        boolean configured = this.config != null;
        return new CloudPoolStatus(this.started, configured);
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setDesiredSize(int desiredSize) throws IllegalArgumentException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException {
        throw new UnsupportedOperationException("not implemented");
    }
}