package com.elastisys.scale.cloudpool.gce.lab;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.Operation;

/**
 * Resizes a <a href=
 * "https://cloud.google.com/compute/docs/instance-groups/#zonal_versus_regional_instance_groups">regional/multi-zone
 * instance group</a>.
 */
public class ResizeMultiZoneInstanceGroup extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ResizeMultiZoneInstanceGroup.class);

    /** The project under which the instance group was created. */
    private static String project = System.getenv("GCE_PROJECT");
    /**
     * TODO: the name of the region where the (multi-zone) instance group is
     * located.
     */
    private static String region = "europe-west1";

    /** TODO: set to the name of the instance group. */
    private static String instanceGroup = "webserver-farm";

    /** TODO: set to desired size */
    private static int targetSize = 0;

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        Compute api = authenticatedApiClient();

        InstanceGroupManager group = api.regionInstanceGroupManagers().get(project, region, instanceGroup).execute();
        LOG.debug("instance group found: {}", group.getInstanceGroup());

        Operation operation = api.regionInstanceGroupManagers().resize(project, region, instanceGroup, targetSize)
                .execute();
        LOG.debug("requested resize: {}", JsonUtils.toPrettyString(JsonUtils.toJson(operation)));
    }
}
