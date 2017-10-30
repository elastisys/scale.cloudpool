package com.elastisys.scale.cloudpool.openstack.driver;

import static com.elastisys.scale.cloudpool.openstack.driver.MachinesMatcher.machines;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openstack4j.api.exceptions.ServerResponseException;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.openstack.compute.domain.NovaFlavor;
import org.openstack4j.openstack.compute.domain.NovaServer;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.openstack.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV3Credentials;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

/**
 * Verifies the operational behavior of the {@link OpenStackPoolDriver}.
 */
@SuppressWarnings("unchecked")
public class TestOpenStackPoolDriverOperation {

    private static final String POOL_NAME = "openstack-pool";
    private static final String CLOUD_PROVIDER = "CityCloud";

    private final OpenstackClient mockClient = mock(OpenstackClient.class);

    /** Object under test. */
    private OpenStackPoolDriver driver;

    @Before
    public void onSetup() throws CloudPoolDriverException {
        this.driver = new OpenStackPoolDriver(this.mockClient, CLOUD_PROVIDER);
        this.driver.configure(driverConfig());
        reset(this.mockClient);
    }

    /**
     * Verifies proper handling of {@link CloudPoolDriver#listMachines()} calls.
     * Delegate to {@link OpenstackClient} and convert response to
     * {@link Machine}s.
     */
    @Test
    public void listMachines() throws CloudPoolDriverException {
        // on empty pool
        setUpMockedPoolClient(POOL_NAME, servers());
        assertThat(this.driver.listMachines(), is(machines()));
        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);

        // on non-empty pool
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));
        assertThat(this.driver.listMachines(), is(machines("i-1")));

        // on pool with machines in different states
        List<Server> members = servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD),
                memberServer("i-3", Status.DELETED));
        setUpMockedPoolClient(POOL_NAME, members);
        List<Machine> machines = this.driver.listMachines();
        assertThat(machines, is(machines("i-1", "i-2", "i-3")));
        // verify that cloud-specific metadata is included for each machine
        assertTrue(machines.get(0).getMetadata().getAsJsonObject().has("id"));
        assertTrue(machines.get(1).getMetadata().getAsJsonObject().has("id"));
        assertTrue(machines.get(2).getMetadata().getAsJsonObject().has("id"));
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown if listing pool
     * members fails.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void listMachinesOnClientError() throws CloudPoolDriverException {
        when(this.mockClient.getServers(Constants.CLOUD_POOL_TAG, POOL_NAME))
                .thenThrow(new ServerResponseException("internal error", 500));
        this.driver.listMachines();
    }

    /**
     * Verifies proper behavior when starting new servers. Make sure the
     * configured provisioning template is used and that pool membership is to
     * marked with a server metadata tag.
     */
    @Test
    public void startMachines() throws Exception {
        ProvisioningTemplate serverTemplate = ProvisioningTemplate.builder("small", "ubuntu").floatingIp(false).build();
        DriverConfig config = new DriverConfig(POOL_NAME, cloudApiSettings(), serverTemplate.toJson());
        this.driver.configure(config);

        // prepare mock
        when(this.mockClient.launchServer(anyString(), argThat(is(any(ProvisioningTemplate.class))),
                argThat(is(any(Map.class))))).thenReturn(memberServer("i-1", Status.BUILD));

        List<Machine> machines = this.driver.startMachines(1);
        assertThat(machines.size(), is(1));

        // verify that server was created with the expected template and was
        // tagged with cloupool membership tag
        ProvisioningTemplate expectedServerTemplate = this.driver.provisioningTemplate();
        Map<String, String> expectedMetadata = ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME);
        verify(this.mockClient).launchServer(anyString(), argThat(is(expectedServerTemplate)),
                argThat(is(expectedMetadata)));
    }

    /**
     * When the provisioning template specifies a floating IP, a floating IP
     * must be allocated and assigned to the {@link Server}.
     */
    @Test
    public void startMachinesWithFloatingIP() throws Exception {
        ProvisioningTemplate serverTemplate = ProvisioningTemplate.builder("small", "ubuntu").floatingIp(true).build();
        DriverConfig config = new DriverConfig(POOL_NAME, cloudApiSettings(), serverTemplate.toJson());
        this.driver.configure(config);

        // prepare mock
        when(this.mockClient.launchServer(anyString(), argThat(is(any(ProvisioningTemplate.class))),
                argThat(is(any(Map.class))))).thenReturn(memberServer("i-1", Status.BUILD));
        when(this.mockClient.assignFloatingIp("i-1")).thenReturn("100.200.100.200");

        List<Machine> machines = this.driver.startMachines(1);
        assertThat(machines.get(0).getPublicIps(), is(asList("100.200.100.200")));

        // verify that call was made to assign floating IP
        verify(this.mockClient).assignFloatingIp("i-1");
    }

    /**
     * On cloud API errors, a {@link StartMachinesException} should be thrown.
     */
    @Test
    public void startMachinesOnClientError() throws StartMachinesException {
        setUpMockedPoolClient(POOL_NAME, servers());
        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient).launchServer(anyString(),
                argThat(is(any(ProvisioningTemplate.class))), argThat(is(any(Map.class))));

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
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        this.driver.terminateMachines(asList("i-1"));

        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);
        verify(this.mockClient).terminateServer("i-1");
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * Verifies behavior when terminating multiple pool members.
     */
    @Test
    public void terminateMultipleInstances() throws Exception {
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        this.driver.terminateMachines(asList("i-1", "i-2"));

        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);
        verify(this.mockClient).terminateServer("i-1");
        verify(this.mockClient).terminateServer("i-2");
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * On client error, a {@link CloudPoolDriverException} should be raised.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void terminateOnClientError() throws Exception {
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient).terminateServer("i-1");

        this.driver.terminateMachines(asList("i-1"));
    }

    /**
     * Trying to terminate a machine that is not recognized as a pool member
     * should result in a {@link TerminateMachinesException}.
     */
    @Test
    public void terminateOnNonGroupMember() {
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        try {
            this.driver.terminateMachines(asList("i-X"));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            assertThat(e.getTerminatedMachines().size(), is(0));
            assertThat(e.getTerminationErrors().size(), is(1));
            assertThat(e.getTerminationErrors().get("i-X"), instanceOf(NotFoundException.class));
        }
    }

    /**
     * When some terminations were successful and some failed, a
     * {@link TerminateMachinesException} should be thrown which indicates which
     * servers were terminated and which server terminations failed.
     */
    @Test
    public void terminateOnPartialFailure() {
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        // i-X is not a pool member and should fail
        try {
            this.driver.terminateMachines(asList("i-X", "i-1"));
            fail("expected to fail");
        } catch (TerminateMachinesException e) {
            // terminating i-1 should succeed
            assertThat(e.getTerminatedMachines(), is(asList("i-1")));
            // terminating i-X should fail
            assertTrue(e.getTerminationErrors().keySet().contains("i-X"));
            assertThat(e.getTerminationErrors().get("i-X"), instanceOf(NotFoundException.class));
        }
    }

    /**
     * Verify that the pool membership tag is removed from the server when
     * detaching a pool member.
     */
    @Test
    public void detach() {
        setUpMockedPoolClient(POOL_NAME,
                servers(memberServer("i-1", Status.ACTIVE), memberServer("i-2", Status.BUILD)));

        this.driver.detachMachine("i-1");

        // should verify that machine is a pool member
        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);
        // should remove cloudpool membership tag
        verify(this.mockClient).untagServer("i-1", asList(Constants.CLOUD_POOL_TAG));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to detach a machine that is not recognized as a
     * pool member.
     */
    @Test(expected = NotFoundException.class)
    public void detachOnNonGroupMember() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.detachMachine("i-X");
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to untag a
     * server that is to be detached from the pool.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void detachOnClientError() throws Exception {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient).untagServer("i-1",
                asList(Constants.CLOUD_POOL_TAG));

        this.driver.detachMachine("i-1");
    }

    /**
     * Verifies that a pool membership tag gets set on servers that are attached
     * to the pool.
     */
    @Test
    public void attach() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.attachMachine("i-2");

        // should remove cloudpool membership tag
        verify(this.mockClient).tagServer("i-2", ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME));
        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * An attempt to attach a non-existing server should result in
     * {@link NotFoundException}.
     */
    @Test(expected = NotFoundException.class)
    public void attachNonExistingMachine() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        // server i-2 does not exist
        doThrow(new NotFoundException("i-2 does not exist")).when(this.mockClient).tagServer("i-2",
                ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME));

        this.driver.attachMachine("i-2");
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag a
     * server that is to be attached to the pool.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void attachOnClientError() throws Exception {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient).tagServer("i-2",
                ImmutableMap.of(Constants.CLOUD_POOL_TAG, POOL_NAME));

        this.driver.attachMachine("i-2");
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setServiceState(String, ServiceState)} stores the
     * state by setting a tag on the server.
     */
    @Test
    public void setServiceState() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.setServiceState("i-1", ServiceState.BOOTING);

        // should verify that machine is a pool member
        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);
        // should set service state metadata tag
        verify(this.mockClient).tagServer("i-1",
                ImmutableMap.of(Constants.SERVICE_STATE_TAG, ServiceState.BOOTING.toString()));

        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to set service state on a server that is not
     * recognized as a pool member.
     */
    @Test(expected = NotFoundException.class)
    public void setServiceStateOnNonGroupMember() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.setServiceState("i-X", ServiceState.BOOTING);
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * service state of a pool server.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setServiceStateOnClientError() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient)
                .tagServer(argThat(is(any(String.class))), argThat(is(any(Map.class))));

        this.driver.setServiceState("i-1", ServiceState.BOOTING);
    }

    /**
     * Verifies that a
     * {@link CloudPoolDriver#setMembershipStatus(String, MembershipStatus)}
     * stores the status by setting a tag on the server.
     */
    @Test
    public void setMembershipStatus() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.setMembershipStatus("i-1", MembershipStatus.blessed());

        // should verify that machine is a pool member
        verify(this.mockClient).getServers(Constants.CLOUD_POOL_TAG, POOL_NAME);
        // should set membership status metadata tag
        verify(this.mockClient).tagServer("i-1", ImmutableMap.of(Constants.MEMBERSHIP_STATUS_TAG,
                JsonUtils.toString(JsonUtils.toJson(MembershipStatus.blessed()))));

        verifyNoMoreInteractions(this.mockClient);
    }

    /**
     * It should not be possible to set membership status on a server that is
     * not recognized as a pool member.
     */
    @Test(expected = NotFoundException.class)
    public void setMembershipStatusOnNonGroupMember() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        this.driver.setMembershipStatus("i-X", MembershipStatus.blessed());
    }

    /**
     * A {@link CloudPoolDriverException} should be thrown on failure to tag the
     * membership status of a pool server.
     */
    @Test(expected = CloudPoolDriverException.class)
    public void setMembershipStatusOnClientError() {
        setUpMockedPoolClient(POOL_NAME, servers(memberServer("i-1", Status.ACTIVE)));

        doThrow(new ServerResponseException("internal error", 500)).when(this.mockClient)
                .tagServer(argThat(is(any(String.class))), argThat(is(any(Map.class))));

        this.driver.setMembershipStatus("i-1", MembershipStatus.blessed());
    }

    private static DriverConfig driverConfig() {
        ProvisioningTemplate serverTemplate = ProvisioningTemplate.builder("small", "ubuntu").floatingIp(true).build();
        return new DriverConfig(POOL_NAME, cloudApiSettings(), serverTemplate.toJson());
    }

    private static JsonObject cloudApiSettings() {
        String userId = null;
        String userName = "foo";
        String userDomainId = null;
        String userDomainName = "default";
        String password = "secret";
        String projectId = null;
        String projectName = "admin";
        String projectDomainName = "default";
        String projectDomainId = null;
        AuthV3Credentials v3Creds = new AuthV3Credentials(userId, userName, userDomainId, userDomainName, password,
                projectId, projectName, projectDomainName, projectDomainId);
        CloudApiSettings apiSettings = new CloudApiSettings(
                new AuthConfig("https://identity.mystack.com:5000/v3/", null, v3Creds), "RegionTwo");
        return JsonUtils.toJson(apiSettings).getAsJsonObject();
    }

    /**
     * Prepares the mock {@link OpenstackClient} to respond to queries as if it
     * was fronting a given pool of {@link Server}s.
     *
     * @param poolName
     * @param servers
     */
    private void setUpMockedPoolClient(String poolName, List<Server> servers) {
        // set up response to queries for pool members
        when(this.mockClient.getServers(Constants.CLOUD_POOL_TAG, poolName)).thenReturn(servers);

        // set up response to queries for pool member meta data
        for (Server server : servers) {
            when(this.mockClient.getServer(server.getId())).thenReturn(server);
        }
    }

    private static List<Server> servers(Server... servers) {
        return Arrays.asList(servers);
    }

    private static Server memberServer(String id, Server.Status status) {
        return server(id, status, null);
    }

    private static Server server(String id, Server.Status status, Map<String, String> metadata) {
        NovaServer server = new NovaServer();
        server.id = id;
        server.status = status;
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        // make sure server is tagged as a cloudpool member
        metadata.put(Constants.CLOUD_POOL_TAG, POOL_NAME);

        server.flavor = (NovaFlavor) NovaFlavor.builder().id("123").name("m1.small").build();
        server.metadata = metadata;
        return server;
    }
}
