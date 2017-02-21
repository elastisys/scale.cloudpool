package com.elastisys.scale.cloudpool.multipool.impl;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;

/**
 * Creation interface used by a {@link DiskBackedMultiCloudPool} when a new
 * {@link CloudPool} instance needs to be created.
 *
 * @see DiskBackedMultiCloudPool
 */
public interface CloudPoolFactory {

    /**
     * Creates a {@link CloudPool} instance.
     * <p/>
     * A {@link ThreadFactory} is passed which is <b>highly recommended to
     * use</b> when the created {@link CloudPool} instance needs to spawn
     * {@link Thread}s/{@link Executor}s. The primary reason being that the
     * {@link ThreadFactory} has been set up with a
     * <a href="http://logback.qos.ch/manual/mdc.html">logging context</a> that
     * gives distinct log output for different {@link CloudPool} instances.
     * <p/>
     * More specifically, the {@link DiskBackedMultiCloudPool} sets up a
     * {@code cloudpool} MDC property which, for example, can be used in a
     * layout pattern via <code>%X{cloudpool}</code> to show which
     * {@link CloudPool} instance produced a given log entry.
     *
     * @param threadFactory
     *            A {@link ThreadFactory} which is <b>highly recommended to
     *            use</b> when the created {@link CloudPool} instance needs to
     *            spawn {@link Thread}s/{@link Executor}s.
     * @param stateDir
     *            A file system path where where the {@link CloudPool} is
     *            allowed to store any runtime state.
     *
     * @return
     * @throws CloudPoolException
     */
    CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException;
}
