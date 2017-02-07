package com.elastisys.scale.cloudpool.google.commons.api.compute;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;

/**
 * A Google Compute Engine client, which encapsulates a subset of the Google
 * Compute Engine API.
 * <p/>
 * The {@link ComputeClient} provides access to the instance group APIs also.
 * Note that depending on the type of instance group (single-zone/multi-zon)
 * different API endpoints are used. See {@link #singleZoneInstance()} and
 * {@link #multiZoneInstanceGroup()}.
 * <p/>
 * Call {@link #configure(CloudApiSettings)} before first use.
 */
public interface ComputeClient {

    /**
     * Configures the {@link ComputeClient}.
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
     * @param instanceUrl
     *            The URL of the instance. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0}.
     * @return
     * @throws NotFoundException
     *             If the instance could not be found.
     * @throws CloudPoolDriverException
     */
    Instance getInstance(String instanceUrl) throws NotFoundException, CloudPoolDriverException;

    /**
     * Returns an instance template.
     *
     * @param instanceTemplateUrl
     *            The instance template URL. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/webserver-template}.
     * @return
     * @throws NotFoundException
     *             If the instance template could not be found.
     * @throws CloudPoolDriverException
     */
    InstanceTemplate getInstanceTemplate(String instanceTemplateUrl) throws NotFoundException, CloudPoolDriverException;

    /**
     * Sets a new collection of {@link Metadata} key-value pairs for a given
     * instance.
     *
     * @param instanceUrl
     *            The URL of the instance. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0}.
     * @param metadata
     *            The new {@link Metadata} collection to set.
     * @return
     * @throws CloudPoolDriverException
     */
    Operation setMetadata(String instanceUrl, Metadata metadata) throws CloudPoolDriverException;

    /**
     * Returns a {@link InstanceGroupClient} which can be used to access a
     * particular multi-zone instance group.
     *
     * @param instanceGroupUrl
     *            The URL of the instance group. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/regions/europe-west1/instanceGroupManagers/my-instance-group}
     * @return
     */
    InstanceGroupClient multiZoneInstanceGroup(String instanceGroupUrl);

    /**
     * Returns a {@link InstanceGroupClient} which can be used to access API a
     * particular single-zone instance group.
     *
     * @param instanceGroupUrl
     *            The URL of the instance group. For example,
     *            {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-c/instanceGroupManagers/my-instance-group}.
     * @return
     */
    InstanceGroupClient singleZoneInstanceGroup(String instanceGroupUrl);

}
