package com.elastisys.scale.cloudpool.gce.driver;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.gce.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.InstanceProperties;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.ManagedInstance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;

/**
 * Captures a (fake) snapshot of a multi-zone (regional) instance group.
 */
public class FakeMultiZoneInstanceGroup {

    private final DriverConfig driverConfig;

    public static final String BASE_INSTANCE_NAME = "webserver";
    public static final String MACHINE_TYPE_NAME = "n1-standard-1";

    /** Sample instance template short name. */
    public static final String INSTANCE_TEMPLATE_NAME = "webserver-template";
    private final int targetSize;
    private final int numRunning;

    public FakeMultiZoneInstanceGroup(DriverConfig driverConfig, int targetSize, int numRunning) {
        this.driverConfig = driverConfig;
        this.targetSize = targetSize;
        this.numRunning = numRunning;

        checkArgument(!provisioningTemplate().isSingleZoneGroup(), "provisioningTemplate must specify a region");
    }

    /**
     * Returns (fake) metadata about the instance group (manager).
     * <p/>
     * Note: only a subset of the group attributes are set (the ones needed by
     * the {@link GcePoolDriver}.
     *
     * @return
     */
    public InstanceGroupManager instanceGroupManager() {
        String instanceTemplateUrl = String.format(
                "https://www.googleapis.com/compute/v1/projects/%s/global/instanceTemplates/%s", project(),
                INSTANCE_TEMPLATE_NAME);
        String instanceGroupUrl = String.format(
                "https://www.googleapis.com/compute/v1/projects/%s/global/instanceGroups/%s", project(),
                BASE_INSTANCE_NAME);
        return new InstanceGroupManager().setName(provisioningTemplate().getInstanceGroup())
                .setBaseInstanceName(BASE_INSTANCE_NAME).setTargetSize(this.targetSize)
                .setInstanceGroup(instanceGroupUrl).setInstanceTemplate(instanceTemplateUrl);
    }

    /**
     * Returns the {@link ManagedInstance} representations of the
     * {@link Instance}s in the group.
     * <p/>
     * Note: only a subset of the instance attributes are set (the ones needed
     * by the {@link GcePoolDriver}.
     *
     * @return
     */
    public List<ManagedInstance> managedInstances() {
        List<ManagedInstance> managedInstances = new ArrayList<>(this.numRunning);
        for (Instance instance : instances()) {
            managedInstances.add(
                    new ManagedInstance().setInstance(instance.getSelfLink()).setInstanceStatus(instance.getStatus()));
        }
        return managedInstances;
    }

    /**
     * Returns the started {@link Instance}s in the (fake) instance group.
     *
     * @return
     */
    public List<Instance> instances() {
        List<Instance> memberInstances = new ArrayList<>();
        for (int i = 1; i <= this.numRunning; i++) {
            String privateIp = "10.0.0." + i;
            String publicIp = "192.100.100." + i;
            memberInstances.add(instance(BASE_INSTANCE_NAME + "-" + i, "RUNNING", privateIp, publicIp));
        }
        return memberInstances;
    }

    /**
     * Returns the (fake) {@link InstanceTemplate} from which the group members
     * are created.
     * <p/>
     * Note: only a subset of the template attributes are set (the ones needed
     * by the {@link GcePoolDriver}.
     *
     * @return
     */
    public InstanceTemplate instanceTemplate() {
        return new InstanceTemplate().setName(INSTANCE_TEMPLATE_NAME)
                .setProperties(new InstanceProperties().setMachineType(MACHINE_TYPE_NAME));
    }

    public String instanceTemplateName() {
        return INSTANCE_TEMPLATE_NAME;
    }

    private Instance instance(String name, String status, String privateIp, String publicIp) {
        String zone = region() + "-" + zoneQualfier();
        List<NetworkInterface> networkInterfaces = Arrays.asList(new NetworkInterface().setName("nic0")
                .setNetworkIP(privateIp).setAccessConfigs(Arrays.asList(new AccessConfig().setNatIP(publicIp))));
        return new Instance().setName(name).setStatus(status).setZone(zoneUrl(zone))
                .setCreationTimestamp(UtcTime.parse("2017-01-01T12:00:00.000Z").toString()).setKind("compute#instance")
                .setMachineType(machineTypeUrl("n1-standard-1", zone)).setNetworkInterfaces(networkInterfaces)
                .setSelfLink(instanceUrl(name, zone))//
                .setMetadata(new Metadata().setItems(new ArrayList<>()));
    }

    /**
     * @param machineTypeName
     * @param zoneName
     *            Zone name. For example, eu-west1-d.
     * @return
     */
    private String machineTypeUrl(String machineTypeName, String zoneName) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s", project(),
                zoneName, machineTypeName);
    }

    private String zoneUrl(String zoneName) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s", project(), zoneName);
    }

    private String instanceUrl(String instanceShortName, String zoneName) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s", project(),
                zoneName, instanceShortName);
    }

    private String zoneQualfier() {
        return "a";
    }

    private String project() {
        return provisioningTemplate().getProject();
    }

    private String region() {
        return provisioningTemplate().getRegion();
    }

    private ProvisioningTemplate provisioningTemplate() {
        return this.driverConfig.parseProvisioningTemplate(ProvisioningTemplate.class);
    }

}
