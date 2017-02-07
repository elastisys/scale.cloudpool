package com.elastisys.scale.cloudpool.google.compute.lab;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.google.commons.utils.InstanceUrl;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionInstanceGroupManagers.Get;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.RegionInstanceGroupManagersListInstancesResponse;

/**
 * Retrieves metadata about a <a href=
 * "https://cloud.google.com/compute/docs/instance-groups/#zonal_versus_regional_instance_groups">regional/multi-zone
 * instance group</a>.
 */
public class GetMultiZoneInstanceGroup extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetMultiZoneInstanceGroup.class);

    /** The project under which the instance group was created. */
    private static String project = System.getenv("GOOGLE_PROJECT");
    /**
     * TODO: the name of the region where the (multi-zone) instance group is
     * located.
     */
    private static String region = "europe-west1";

    /** TODO: set to the name of the instance group. */
    private static String instanceGroup = "webserver-farm";

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        Compute api = computeApiClient();

        Get request = api.regionInstanceGroupManagers().get(project, region, instanceGroup);
        InstanceGroupManager instanceGroupManager = request.execute();
        LOG.debug("multi-zone instance group: {}", instanceGroupManager.toPrettyString());

        String instanceTemplateUrl = instanceGroupManager.getInstanceTemplate();
        InstanceTemplate instanceTemplate = api.instanceTemplates().get(project, UrlUtils.basename(instanceTemplateUrl))
                .execute();
        LOG.debug("instance template: {}", instanceTemplate.toPrettyString());

        RegionInstanceGroupManagersListInstancesResponse listManagedInstances = api.regionInstanceGroupManagers()
                .listManagedInstances(project, region, instanceGroup).execute();
        List<ManagedInstance> groupInstances = listManagedInstances.getManagedInstances();
        if (groupInstances != null) {
            for (ManagedInstance member : groupInstances) {
                LOG.debug("member: {}", member);

                InstanceUrl instanceUrl = new InstanceUrl(member.getInstance());
                LOG.debug("instance URL: {}", instanceUrl);

                Instance instance = api.instances().get(project, instanceUrl.getZone(), instanceUrl.getName())
                        .execute();
                LOG.debug("instance: {}", instance.toPrettyString());
            }
        }
    }

}
