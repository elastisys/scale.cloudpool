package com.elastisys.scale.cloudpool.gce.driver.client;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.gce.driver.GcePoolDriver;
import com.elastisys.scale.cloudpool.gce.driver.config.CloudApiSettings;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;

/**
 * Google Compute Engine client, which encapsulates a subset of the Google
 * Compute Engine API required by the {@link GcePoolDriver}.
 * <p/>
 * The {@link GceClient} provides access to the instance group APIs also. Note
 * that depending on the type of instance group (single-zone/multi-zon)
 * different API endpoints are used. See {@link #singleZoneInstance()} and
 * {@link #multiZoneInstanceGroup()}.
 * <p/>
 * Call {@link #configure(CloudApiSettings)} before first use.
 */
public interface GceClient {

    /**
     * Configures the {@link GceClient}.
     *
     * @param config
     *            GCE API credentials and settings.
     * @throws IllegalArgumentException
     * @throws CloudPoolDriverException
     */
    void configure(CloudApiSettings config) throws IllegalArgumentException, CloudPoolDriverException;

    /**
     * Retrieves metadata about a particular {@link Instance}.
     *
     * @param project
     *            The project under which the instance was created.
     * @param zone
     *            The zone in which the {@link Instance} is located. For
     *            example, {@code europe-west-1d}.
     * @param instanceName
     *            The short name of the {@link Instance}. For example,
     *            {@code mygroup-rxjc}.
     * @return
     * @throws NotFoundException
     *             If the instance could not be found.
     * @throws CloudPoolDriverException
     */
    Instance getInstance(String project, String zone, String instanceName)
            throws NotFoundException, CloudPoolDriverException;

    /**
     * Returns an instance template.
     *
     * @param project
     *            The project under which the instance template was created.
     * @param instanceTemplateName
     *            The short name of an instance template. For example,
     *            {@code my-instance-template}.
     * @return
     * @throws NotFoundException
     *             If the instance template could not be found.
     * @throws CloudPoolDriverException
     */
    InstanceTemplate getInstanceTemplate(String project, String instanceTemplateName)
            throws NotFoundException, CloudPoolDriverException;

    /**
     * Sets a new collection of {@link Metadata} key-value pairs for a given
     * instance.
     *
     * @param project
     *            The project under which the instance was created.
     * @param zone
     *            The zone in which the {@link Instance} is located. For
     *            example, {@code europe-west-1d}.
     * @param instanceName
     *            The short name of the {@link Instance}. For example,
     *            {@code mygroup-rxjc}.
     * @param metadata
     *            The new {@link Metadata} collection to set.
     * @return
     * @throws CloudPoolDriverException
     */
    Operation setMetadata(String project, String zone, String instanceName, Metadata metadata)
            throws CloudPoolDriverException;

    /**
     * Returns a {@link MultiZoneInstanceGroupClient} which can be used to
     * access API methods for a particular multi-zone instance groups.
     *
     * @param project
     *            The project under which the instance group was created.
     *
     * @param region
     *            The name of the region where the instance group is located.
     *            For example, "europe-west1".
     * @param instanceGroup
     *            The name of the instance group.
     * @return
     */
    InstanceGroupClient multiZoneInstanceGroup(String project, String region, String instanceGroup);

    /**
     * Returns a {@link SingleZoneInstanceGroupClient} which can be used to
     * access API methods for a particular single-zone instance groups.
     *
     * @param project
     *            The project under which the instance group was created.
     * @param zone
     *            The name of the zone where the instance group is located. For
     *            example, "europe-west1-d".
     * @param instanceGroup
     *            The name of the instance group.
     * @return
     */
    InstanceGroupClient singleZoneInstanceGroup(String project, String zone, String instanceGroup);

}
