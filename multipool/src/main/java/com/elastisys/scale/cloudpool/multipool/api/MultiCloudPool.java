package com.elastisys.scale.cloudpool.multipool.api;

import java.util.List;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotFoundException;

/**
 * A {@link MultiCloudPool} offers management (create/delete) and provides
 * access to a collection of {@link CloudPoolInstance}s.
 *
 * @see CloudPool
 */
public interface MultiCloudPool {

    /**
     * Creates a new {@link CloudPoolInstance} and adds it to the collection.
     * The created instance will be in an unconfigured and unstarted state.
     *
     * @param cloudPoolName
     *            A {@link CloudPool} name, such as {@code my-cloud-pool}. A
     *            {@link CloudPool} name may only contain the following
     *            characters: {@code [A-Za-z_0-9\\-\\.}.
     * @return
     * @throws CloudPoolCreateException
     * @throws IllegalArgumentException
     */
    CloudPoolInstance create(String cloudPoolName) throws IllegalArgumentException, CloudPoolCreateException;

    /**
     * Deletes a {@link CloudPoolInstance} from the collection.
     *
     * @param cloudPoolName
     *            A {@link CloudPool} name, such as {@code my-cloud-pool}.
     * @throws CloudPoolDeleteException
     * @throws NotFoundException
     *             If no instance with the given name exists.
     */
    void delete(String cloudPoolName) throws NotFoundException, CloudPoolDeleteException;

    /**
     * Lists the names of all {@link CloudPoolInstance}s in the collection.
     *
     * @return
     */
    List<String> list();

    /**
     * Returns a particular {@link CloudPool} instance from the collection.
     *
     * @param cloudPoolName
     *            A {@link CloudPool} name, such as {@code my-cloud-pool}.
     * @return
     * @throws NotFoundException
     *             If no instance with the given name exists.
     */
    CloudPoolInstance get(String cloudPoolName) throws NotFoundException;
}
