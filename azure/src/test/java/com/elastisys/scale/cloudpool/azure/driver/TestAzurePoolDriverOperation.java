package com.elastisys.scale.cloudpool.azure.driver;

import static com.elastisys.scale.cloudpool.azure.driver.MachinesMatcher.machines;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureClient;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureException;
import com.elastisys.scale.cloudpool.azure.driver.client.VmSpec;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureApiAccess;
import com.elastisys.scale.cloudpool.azure.driver.config.AzureAuth;
import com.elastisys.scale.cloudpool.azure.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.CustomScriptExtension;
import com.elastisys.scale.cloudpool.azure.driver.config.LinuxSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.NetworkSettings;
import com.elastisys.scale.cloudpool.azure.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * Verifies the operational behavior of the {@link AzurePoolDriver}.
 */
@SuppressWarnings("unchecked")
public class TestAzurePoolDriverOperation {

    private static final String POOL_NAME = "azurepool";
    private static final VirtualMachineSizeTypes VM_SIZE = VirtualMachineSizeTypes.STANDARD_DS1_V2;
    private static final String VM_IMAGE = "Canonical:UbuntuServer:16.04.0-LTS:latest";
    private static final String VM_NAME_PREFIX = "vm";
    private static final String RESOURCE_GROUP = "my-rg";

    private final AzureClient mockClient = mock(AzureClient.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    /** Object under test. */
    private AzurePoolDriver driver;

    @Before
    public void onSetup() throws CloudPoolDriverException {
        this.driver = new AzurePoolDriver(this.mockClient, this.executor);
        this.driver.configure(driverConfig());
        reset(this.mockClient);

        FrozenTime.setFixed(UtcTime.parse("2017-11-01T12:00:00.000Z"));
    }

    /**
     * Verifies proper handling of {@link CloudPoolDriver#listMachines()} calls.
     * Delegate to {@link AzureClient} and convert response to {@link Machine}s.
     */
    @Test
    public void listMachines() throws CloudPoolDriverException {
        // on empty pool
        setUpMockedPoolClient(POOL_NAME, vms());
        assertThat(this.driver.listMachines(), is(machines()));
        verify(this.mockClient).listVms(poolMembershipTag());

        // on non-empty pool
        setUpMockedPoolClient(POOL_NAME, vms(memberVm("vm-1", PowerState.RUNNING)));
        assertThat(this.driver.listMachines(), is(machines("vm-1")));
        //
        // on pool with machines in different states
        List<VirtualMachine> members = vms(memberVm("vm-1", PowerState.RUNNING), memberVm("vm-2", PowerState.STARTING),
                memberVm("vm-3", PowerState.DEALLOCATED));
        setUpMockedPoolClient(POOL_NAME, members);
        List<Machine> machines = this.driver.listMachines();
        assertThat(machines, is(machines("vm-1", "vm-2", "vm-3")));
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown if listing pool
     * members fails.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void listMachinesOnClientError() throws CloudPoolDriverException {
        when(this.mockClient.listVms(poolMembershipTag())).thenThrow(new AzureException("socket timeout"));
        this.driver.listMachines();
    }

    /**
     * Verifies proper behavior when starting new servers. Make sure the
     * configured provisioning template is used and that pool membership is
     * marked with a vm tag.
     */
    @Test
    public void startMachines() throws Exception {
        ProvisioningTemplate vmTemplate = provisioningTemplate();
        DriverConfig config = new DriverConfig(POOL_NAME, cloudApiSettings(), toJson(vmTemplate));
        this.driver.configure(config);

        // prepare mock
        String vmName = expectedVmName(vmTemplate, 0);
        List<VirtualMachine> createdVms = vms(memberVm(vmName, PowerState.RUNNING));
        when(this.mockClient.launchVms(argThat(is(any(List.class))))).thenReturn(createdVms);

        List<Machine> machines = this.driver.startMachines(1);
        assertThat(machines.size(), is(1));

        // verify that the expected VmSpec was passed to client
        List<VmSpec> expectedVmSpecs = exepectedVmSpecs(1, vmTemplate);
        verify(this.mockClient).launchVms(expectedVmSpecs);
    }

    /**
     * When starting multiple machines, a unique name must be given to each VM.
     * This follows the pattern: {@code vmNamePrefix-timeMillis-index}.
     */
    @Test
    public void startMultipleMachines() throws Exception {
        ProvisioningTemplate vmTemplate = provisioningTemplate();
        DriverConfig config = new DriverConfig(POOL_NAME, cloudApiSettings(), toJson(vmTemplate));
        this.driver.configure(config);

        // prepare mock
        String vmName0 = expectedVmName(vmTemplate, 0);
        String vmName1 = expectedVmName(vmTemplate, 1);
        List<VirtualMachine> createdVms = vms(memberVm(vmName0, PowerState.STARTING),
                memberVm(vmName1, PowerState.STARTING));
        when(this.mockClient.launchVms(argThat(is(any(List.class))))).thenReturn(createdVms);

        List<Machine> machines = this.driver.startMachines(2);
        assertThat(machines.size(), is(2));

        // verify that the expected VmSpec was passed to client
        List<VmSpec> expectedVmSpecs = exepectedVmSpecs(2, vmTemplate);
        verify(this.mockClient).launchVms(expectedVmSpecs);
    }

    /**
     * On cloud API errors, a {@link StartMachinesException} should be thrown.
     */
    @Test
    public void startMachinesOnClientError() throws StartMachinesException {
        setUpMockedPoolClient(POOL_NAME, vms());
        when(this.mockClient.launchVms(argThat(is(any(List.class))))).thenThrow(new AzureException("socket timeout"));

        // should raise an exception
        try {
            this.driver.startMachines(1);
            fail("startMachines expected to fail");
        } catch (StartMachinesException e) {
            assertThat(e.getRequestedMachines(), is(1));
            assertThat(e.getStartedMachines().size(), is(0));
        }
    }

    /**
     * Verifies behavior when terminating a single pool member.
     */
    @Test
    public void terminateSingleInstance() throws Exception {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        this.driver.terminateMachines(asList(vm1.id()));

        verify(this.mockClient, atLeastOnce()).listVms(poolMembershipTag());
        verify(this.mockClient).deleteVms(asList(vm1.id()));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * Verifies behavior when terminating multiple pool members.
     */
    @Test
    public void terminateMultipleInstances() throws Exception {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        this.driver.terminateMachines(asList(vm1.id(), vm2.id()));

        verify(this.mockClient, atLeastOnce()).listVms(poolMembershipTag());
        verify(this.mockClient).deleteVms(asList(vm1.id(), vm2.id()));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * On client error, a {@link CloudPoolDriverException} should be raised.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void terminateOnClientError() throws Exception {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        String vm1Id = vm1.id();
        doThrow(new AzureException("socket timeout")).when(this.mockClient).deleteVms(asList(vm1Id));

        this.driver.terminateMachines(asList(vm1.id()));
    }

    /**
     * Trying to terminate a machine that is not recognized as a pool member
     * should result in a {@link TerminateMachinesException}.
     */
    @Test
    public void terminateOnNonGroupMember() {
        setUpMockedPoolClient(POOL_NAME,
                vms(memberVm("vm-1", PowerState.RUNNING), memberVm("vm-2", PowerState.STARTING)));

        try {
            this.driver.terminateMachines(asList("vm-X"));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            assertThat(e.getTerminatedMachines().size(), is(0));
            assertThat(e.getTerminationErrors().size(), is(1));
            assertThat(e.getTerminationErrors().get("vm-X"), instanceOf(NotFoundException.class));
        }
    }

    /**
     * When some terminations were successful and some failed, a
     * {@link TerminateMachinesException} should be thrown which indicates which
     * servers were terminated and which server terminations failed.
     */
    @Test
    public void terminateOnPartialFailure() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        // i-X is not a pool member and should fail
        try {
            this.driver.terminateMachines(asList("vm-X", vm1.id()));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            // terminating i-1 should succeed
            assertThat(e.getTerminatedMachines(), is(asList(vm1.id())));
            // terminating i-X should fail
            assertTrue(e.getTerminationErrors().keySet().contains("vm-X"));
            assertThat(e.getTerminationErrors().get("vm-X"), instanceOf(NotFoundException.class));
        }
    }

    /**
     * Exercise a scenario where a multi-vm termination fails (for different
     * reasons) for several VMs while others are successful.
     */
    @Test
    public void terminateOnPartialSuccessWithMultipleFailures() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        String vm1Id = vm1.id();
        String vm2Id = vm2.id();

        // when asked to delete vm1 and vm2, only vm1 should succeed
        Map<String, Throwable> terminationErrors = ImmutableMap.of(vm2Id, new AzureException("failed to delete nic"));
        doThrow(new TerminateMachinesException(asList(vm1Id), terminationErrors)).when(this.mockClient)
                .deleteVms(asList(vm1Id, vm2Id));

        try {
            this.driver.terminateMachines(asList("vm-X", vm1Id, vm2Id));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            // terminating vm-1 should succeed
            assertThat(e.getTerminatedMachines(), is(asList(vm1Id)));
            // terminating vm-X should fail
            assertTrue(e.getTerminationErrors().keySet().contains("vm-X"));
            assertThat(e.getTerminationErrors().get("vm-X"), instanceOf(NotFoundException.class));
            // terminating vm-2 should fail
            assertTrue(e.getTerminationErrors().keySet().contains(vm2Id));
            assertThat(e.getTerminationErrors().get(vm2Id), instanceOf(AzureException.class));
        }
    }

    /**
     * Verify that the pool membership tag is removed from the vm when detaching
     * a pool member.
     */
    @Test
    public void detach() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        VirtualMachine vm2 = memberVm("vm-2", PowerState.STARTING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1, vm2));

        this.driver.detachMachine(vm1.id());

        // should verify that machine is a pool member
        verify(this.mockClient).getVm(vm1.id());
        // should remove cloudpool membership tag
        verify(this.mockClient).untagVm(vm1, asList(Constants.CLOUD_POOL_TAG));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to detach a machine that is not recognized as a
     * pool member.
     */
    @Test(expected = NotFoundException.class)
    public void detachOnNonGroupMember() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));
        when(this.mockClient.getVm("vm-X")).thenThrow(new NotFoundException("uncrecognized"));

        this.driver.detachMachine("vm-X");
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to untag a
     * server that is to be detached from the pool.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void detachOnClientError() throws Exception {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        doThrow(new AzureException("socket timeout")).when(this.mockClient).untagVm(vm1,
                asList(Constants.CLOUD_POOL_TAG));

        this.driver.detachMachine(vm1.id());
    }

    /**
     * Verifies that a pool membership tag gets set on servers that are attached
     * to the pool.
     */
    @Test
    public void attach() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        // vm to be attached
        Map<String, String> tags = ImmutableMap.of("key", "value");
        VirtualMachine vm2 = vm("vm-2", PowerState.RUNNING, tags);

        when(this.mockClient.getVm(vm2.id())).thenReturn(vm2);

        this.driver.attachMachine(vm2.id());

        // should remove cloudpool membership tag
        verify(this.mockClient).getVm(vm2.id());
        verify(this.mockClient).tagVm(vm2, ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * An attempt to attach a non-existing server should result in
     * {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void attachNonExistingMachine() {
        setUpMockedPoolClient(POOL_NAME, vms());

        // server vm-X does not exist
        doThrow(new NotFoundException("vm-X does not exist")).when(this.mockClient).getVm("vm-X");

        this.driver.attachMachine("vm-X");
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag a
     * server that is to be attached to the pool.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void attachOnClientError() throws Exception {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        // vm to be attached
        Map<String, String> tags = ImmutableMap.of("key", "value");
        VirtualMachine vm2 = vm("vm-2", PowerState.RUNNING, tags);
        when(this.mockClient.getVm(vm2.id())).thenReturn(vm2);

        doThrow(new AzureException("socket timeout")).when(this.mockClient).tagVm(vm2, poolMembershipTag());

        this.driver.attachMachine(vm2.id());
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setServiceState(String, ServiceState)} stores the
     * state by setting a tag on the server.
     */
    @Test
    public void setServiceState() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        this.driver.setServiceState(vm1.id(), ServiceState.BOOTING);

        // should verify that machine is a pool member
        verify(this.mockClient).getVm(vm1.id());
        // should set service state metadata tag
        verify(this.mockClient).tagVm(vm1,
                ImmutableMap.of(Constants.SERVICE_STATE_TAG, ServiceState.BOOTING.toString()));

        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to set service state on a server that is not
     * recognized as a pool member.
     */
    @Test(expected = NotFoundException.class)
    public void setServiceStateOnNonGroupMember() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        when(this.mockClient.getVm("vm-X")).thenThrow(new NotFoundException("not found"));

        this.driver.setServiceState("vm-X", ServiceState.BOOTING);
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * service state of a pool server.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setServiceStateOnClientError() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        doThrow(new AzureException("socket timeout")).when(this.mockClient).tagVm(vm1,
                ImmutableMap.of(Constants.SERVICE_STATE_TAG, ServiceState.BOOTING.toString()));

        this.driver.setServiceState(vm1.id(), ServiceState.BOOTING);
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setMembershipStatus(String, MembershipStatus)}
     * stores the status by setting a tag on the server.
     */
    @Test
    public void setMembershipStatus() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        this.driver.setMembershipStatus(vm1.id(), MembershipStatus.blessed());

        // should verify that machine is a pool member
        verify(this.mockClient).getVm(vm1.id());
        // should set tag
        String expectedTagValue = JsonUtils.toString(JsonUtils.toJson(MembershipStatus.blessed()));
        verify(this.mockClient).tagVm(vm1, ImmutableMap.of(Constants.MEMBERSHIP_STATUS_TAG, expectedTagValue));

        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to set membership status on a server that is
     * not recognized as a pool member.
     */
    @Test(expected = NotFoundException.class)
    public void setMembershipStatusOnNonGroupMember() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        when(this.mockClient.getVm("vm-X")).thenThrow(new NotFoundException("not found"));

        this.driver.setMembershipStatus("vm-X", MembershipStatus.blessed());
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * membership status of a pool server.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setMembershipStatusOnClientError() {
        VirtualMachine vm1 = memberVm("vm-1", PowerState.RUNNING);
        setUpMockedPoolClient(POOL_NAME, vms(vm1));

        String expectedTagValue = JsonUtils.toString(JsonUtils.toJson(MembershipStatus.blessed()));
        doThrow(new AzureException("socket timeout")).when(this.mockClient).tagVm(vm1,
                ImmutableMap.of(Constants.MEMBERSHIP_STATUS_TAG, expectedTagValue));

        this.driver.setMembershipStatus(vm1.id(), MembershipStatus.blessed());

    }

    /**
     * Prepares the mock {@link AzureClient} to respond to queries as if it was
     * fronting a given pool of {@link VirtualMachine}s.
     *
     * @param poolName
     * @param servers
     */
    private void setUpMockedPoolClient(String poolName, List<VirtualMachine> vms) {
        // set up response to queries for pool members
        when(this.mockClient.listVms(poolMembershipTag())).thenReturn(vms);

        // set up response to queries for pool member meta data
        for (VirtualMachine vm : vms) {
            when(this.mockClient.getVm(vm.id())).thenReturn(vm);
        }
    }

    private static List<VirtualMachine> vms(VirtualMachine... vms) {
        return Arrays.asList(vms);
    }

    private static Map<String, String> poolMembershipTag() {
        return ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME);
    }

    private static DriverConfig driverConfig() {
        return new DriverConfig(POOL_NAME, cloudApiSettings(), toJson(provisioningTemplate()));
    }

    private static ProvisioningTemplate provisioningTemplate() {
        return ProvisioningTemplate.builder(VM_SIZE, VM_IMAGE, network()).linuxSettings(linuxSettings())
                .vmNamePrefix(VM_NAME_PREFIX).build();
    }

    private static JsonObject toJson(Object object) {
        return JsonUtils.toJson(object).getAsJsonObject();
    }

    private static LinuxSettings linuxSettings() {
        String adminUser = "ubuntu";
        String password = null;
        String publicSshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDOGYHyOtch3SS05svQbhXC7zJSPbiNSIr50rrviOiQfm+3forRUfTIJl6K+DhyDL77ORHVwgGm+9R3hQ8RUzEqbzdolzAFveDnGKqtISi36e5rp3jJJV8bSf3Z7BdBv/T9Aat3Acn38Nov9YcuyxNVPwR8yawRol0EEBHyHLjgaX0l2U5KhDfgdMKgTNeuNUBRqE+eRN23C1eWyghOyEADeBEkA2l9CGjS09L778O0jI3qVfzZ+Awbx6ibQDu0P00IHDGfuDaZmMff5raTg2JEPtLr4sAyCvlnLyN4wdufe+06CP6hAsjW+aJ8EPbDinT0rmGEWIV6ZZ9Bhp48WZf1 foo@bar";
        String customData = Base64Utils.toBase64("#!bin/bash\napt-get update -qy && apt-get install apache2 -qy");
        CustomScriptExtension customScript = null;
        return new LinuxSettings(adminUser, publicSshKey, password, customData, customScript);
    }

    private static NetworkSettings network() {
        String virtualNetwork = "vmnet";
        String subnetName = "vmsubnet";
        boolean assignPublicIp = true;
        List<String> securityGroups = Arrays.asList("web");
        return new NetworkSettings(virtualNetwork, subnetName, assignPublicIp, securityGroups);
    }

    private static JsonObject cloudApiSettings() {
        CloudApiSettings apiSettings = new CloudApiSettings(
                new AzureApiAccess("subscriptionId", new AzureAuth("clientId", "domain", "secret")), RESOURCE_GROUP,
                Region.EUROPE_NORTH.name());
        return JsonUtils.toJson(apiSettings).getAsJsonObject();
    }

    private static VirtualMachine memberVm(String name, PowerState powerState) {
        // make sure server is tagged as a cloudpool member
        Map<String, String> tags = ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME);
        return vm(name, powerState, tags);
    }

    private static VirtualMachine vm(String name, PowerState powerState, Map<String, String> tags) {
        if (tags == null) {
            tags = new HashMap<>();
        }
        return VmMockBuilder.with().name(name).resourceGroup(RESOURCE_GROUP).powerState(powerState).tags(tags).build();
    }

    /**
     * Returns the list of {@link VmSpec} that are expected to be used to
     * provision a certain number of VMs from a given
     * {@link ProvisioningTemplate}.
     *
     * @param numVms
     * @param vmTemplate
     * @return
     */
    private static List<VmSpec> exepectedVmSpecs(int numVms, ProvisioningTemplate vmTemplate) {
        List<VmSpec> expectedVmSpecs = new ArrayList<>();
        for (int i = 0; i < numVms; i++) {
            String expectedName = expectedVmName(vmTemplate, i);

            Map<String, String> expectedTags = new HashMap<>(vmTemplate.getTags());
            expectedTags.putAll(poolMembershipTag());

            expectedVmSpecs.add(new VmSpec(vmTemplate.getVmSize(), vmTemplate.getVmImage(), vmTemplate.getOsDiskType(),
                    expectedName, vmTemplate.getLinuxSettings(), vmTemplate.getWindowsSettings(),
                    vmTemplate.getNetwork(), vmTemplate.getAvailabilitySet().orElse(null), expectedTags));
        }
        return expectedVmSpecs;
    }

    /**
     * Expected VM name for VM created with a given index at a given point in
     * time. Indices start at 0.
     *
     * @param vmTemplate
     * @param index
     * @return
     */
    private static String expectedVmName(ProvisioningTemplate vmTemplate, int index) {
        String namePrefix = vmTemplate.getVmNamePrefix().isPresent() ? vmTemplate.getVmNamePrefix().get() : POOL_NAME;
        String expectedName = String.format("%s-%d-%s", namePrefix, UtcTime.now().getMillis(), index);
        return expectedName;
    }

}
