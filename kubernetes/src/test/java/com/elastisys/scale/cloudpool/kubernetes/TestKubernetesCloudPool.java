package com.elastisys.scale.cloudpool.kubernetes;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.PodPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.testutils.DeploymentBuilder;
import com.elastisys.scale.cloudpool.kubernetes.testutils.FakeKubeErrors;
import com.elastisys.scale.cloudpool.kubernetes.testutils.ReplicaSetBuilder;
import com.elastisys.scale.cloudpool.kubernetes.testutils.ReplicationControllerBuilder;
import com.elastisys.scale.cloudpool.kubernetes.types.Deployment;
import com.elastisys.scale.cloudpool.kubernetes.types.DeploymentSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.cloudpool.kubernetes.types.PodSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.PodStatus;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSet;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSetSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationController;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicationControllerSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.Status;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link KubernetesCloudPool}.
 */
public class TestKubernetesCloudPool {

    private static final Logger LOG = LoggerFactory.getLogger(TestKubernetesCloudPool.class);

    /** Sample Kubernetes namespace. */
    private static final String NAMESPACE = "my-ns";
    /** Sample ReplicationController name. */
    private static final String RC_NAME = "nginx-rc";
    /** Sample ReplicaSet name. */
    private static final String RS_NAME = "nginx-rs";
    /** Sample Deployment name. */
    private static final String DEPLOYMENT_NAME = "nginx-deployment";

    /** Path to client cert. */
    private static final String CLIENT_CERT_PATH = "src/test/resources/ssl/admin.pem";
    /** Path to client key. */
    private static final String CLIENT_KEY_PATH = "src/test/resources/ssl/admin-key.pem";

    private static final String API_SERVER_URL = "https://kube.api:443";
    /** Sample client auth. */
    private static final AuthConfig CERT_AUTH = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
            .build();

    /** Sample {@link PodPoolConfig} for a ReplicationController. */
    private static final PodPoolConfig RC_POD_POOL = new PodPoolConfig(NAMESPACE, RC_NAME, null, null);
    /** Sample {@link PodPoolConfig} for a ReplicaSet. */
    private static final PodPoolConfig RS_POD_POOL = new PodPoolConfig(NAMESPACE, null, RS_NAME, null);
    /** Sample {@link PodPoolConfig} for a Deployment. */
    private static final PodPoolConfig DEPLOYMENT_POD_POOL = new PodPoolConfig(NAMESPACE, null, null, DEPLOYMENT_NAME);
    /** Sample update interval. */
    private static final TimeInterval UPDATE_INTERVAL = TimeInterval.seconds(10);

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    private ApiServerClient mockApiServer = mock(ApiServerClient.class);

    /** Object under test. */
    private KubernetesCloudPool cloudpool;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
        this.executor.setRemoveOnCancelPolicy(true);
        this.cloudpool = new KubernetesCloudPool(this.mockApiServer, this.executor);
    }

    /**
     * Should not be possible to start before the cloud pool has been
     * configured.
     */
    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() {
        this.cloudpool.start();
    }

    /**
     * Start and stop should be properly implemented and idempotent.
     */
    @Test
    public void startAndStop() {
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.UNCONFIGURED_STOPPED));
        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));

        this.cloudpool.start();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STARTED));
        assertThat(this.executor.getQueue().size(), is(1));
        // start should be idempotent
        this.cloudpool.start();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STARTED));
        // no new periodical tasks should have been added
        assertThat(this.executor.getQueue().size(), is(1));

        this.cloudpool.stop();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));
        assertThat(this.executor.getQueue().size(), is(0));
        // stop should be idempotent
        this.cloudpool.stop();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));
        assertThat(this.executor.getQueue().size(), is(0));

        // should be possible to restart
        this.cloudpool.start();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STARTED));
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * The started state should be kept when a new config is set.
     */
    @Test
    public void configureStarted() {
        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        this.cloudpool.start();
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STARTED));
        assertThat(this.executor.getQueue().size(), is(1));

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        // should still be in a started state
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STARTED));
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * The started state should be kept when a new config is set.
     */
    @Test
    public void configureStopped() {
        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));
        assertThat(this.executor.getQueue().size(), is(0));

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        // should still be in a stopped state
        assertThat(this.cloudpool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));
        assertThat(this.executor.getQueue().size(), is(0));
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicationController.
     */
    @Test
    public void getMachinePoolOnReplicationController() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicationController rc = ReplicationControllerBuilder.create().namespace(NAMESPACE).name(RC_NAME)
                .addTemplateLabel("app", "nginx").desiredReplicas(2).replicas(2).build();
        prepareMockApiServer(rc);

        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        this.cloudpool.start();

        MachinePool machinePool = this.cloudpool.getMachinePool();
        assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
        assertThat(machinePool.getMachines().size(), is(2));
        assertThat(machinePool.getMachines().get(0).getId(), is("nginx-rc-0"));
        assertThat(machinePool.getMachines().get(1).getId(), is("nginx-rc-1"));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(replicationControllerPath(NAMESPACE, RC_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, "app=nginx"));
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * Deployment.
     */
    @Test
    public void getMachinePoolOnDeployment() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        Deployment deployment = DeploymentBuilder.create().namespace(NAMESPACE).name(DEPLOYMENT_NAME)
                .addTemplateLabel("app", "nginx").desiredReplicas(2).replicas(2).build();
        prepareMockApiServer(deployment);

        this.cloudpool.configure(asJson(config(DEPLOYMENT_POD_POOL)));
        this.cloudpool.start();

        MachinePool machinePool = this.cloudpool.getMachinePool();
        assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
        assertThat(machinePool.getMachines().size(), is(2));
        assertThat(machinePool.getMachines().get(0).getId(), is("nginx-deployment-0"));
        assertThat(machinePool.getMachines().get(1).getId(), is("nginx-deployment-1"));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(deploymentPath(NAMESPACE, DEPLOYMENT_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, "app=nginx"));
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicaSet.
     */
    @Test
    public void getMachinePoolOnReplicaSet() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME).addTemplateLabel("app", "nginx")
                .desiredReplicas(2).replicas(2).build();
        prepareMockApiServer(rs);

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        MachinePool machinePool = this.cloudpool.getMachinePool();
        assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
        assertThat(machinePool.getMachines().size(), is(2));
        assertThat(machinePool.getMachines().get(0).getId(), is("nginx-rs-0"));
        assertThat(machinePool.getMachines().get(1).getId(), is("nginx-rs-1"));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, "app=nginx"));
    }

    /**
     * On failure to fetch the machine pool, a {@link CloudPoolException} should
     * be thrown.
     */
    @Test(expected = CloudPoolException.class)
    public void getMachinePoolOnError() throws Exception {
        Status status = FakeKubeErrors.errorStatus(404, String.format("replicasets \"%s\" not found", RS_NAME));
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME)))
                .thenThrow(new HttpResponseException(404, status.toString()));

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        this.cloudpool.getMachinePool();
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicationController.
     */
    @Test
    public void getPoolSizeOnReplicationController() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicationController rc = ReplicationControllerBuilder.create().namespace(NAMESPACE).name(RC_NAME)
                .addTemplateLabel("app", "nginx").desiredReplicas(3).replicas(2).build();
        prepareMockApiServer(rc);

        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        this.cloudpool.start();

        PoolSizeSummary poolSize = this.cloudpool.getPoolSize();
        assertThat(poolSize, is(new PoolSizeSummary(3, 2, 2)));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(replicationControllerPath(NAMESPACE, RC_NAME));
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * Deployment.
     */
    @Test
    public void getPoolSizeOnDeployment() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        Deployment deployment = DeploymentBuilder.create().namespace(NAMESPACE).name(DEPLOYMENT_NAME)
                .addTemplateLabel("app", "nginx").desiredReplicas(3).replicas(2).build();
        prepareMockApiServer(deployment);

        this.cloudpool.configure(asJson(config(DEPLOYMENT_POD_POOL)));
        this.cloudpool.start();

        PoolSizeSummary poolSize = this.cloudpool.getPoolSize();
        assertThat(poolSize, is(new PoolSizeSummary(3, 2, 2)));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(deploymentPath(NAMESPACE, DEPLOYMENT_NAME));
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicaSet.
     */
    @Test
    public void getPoolSizeOnReplicaSet() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME).addTemplateLabel("app", "nginx")
                .desiredReplicas(3).replicas(2).build();
        prepareMockApiServer(rs);

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        PoolSizeSummary poolSize = this.cloudpool.getPoolSize();
        assertThat(poolSize, is(new PoolSizeSummary(3, 2, 2)));

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
    }

    /**
     * On failure to fetch the pool size, a {@link CloudPoolException} should be
     * thrown.
     */
    @Test(expected = CloudPoolException.class)
    public void getPoolSizeOnError() throws Exception {
        Status status = FakeKubeErrors.errorStatus(404, String.format("replicasets \"%s\" not found", RS_NAME));
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME)))
                .thenThrow(new HttpResponseException(404, status.toString()));

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        this.cloudpool.getPoolSize();
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicationController.
     */
    @Test
    public void setDesiredSizeOnReplicationController() throws Exception {
        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        this.cloudpool.start();

        Future<?> update = this.cloudpool.setDesiredSize(10);

        // wait for update to complete
        update.get();

        // expected patch
        ReplicationController patch = new ReplicationController();
        patch.spec = new ReplicationControllerSpec();
        patch.spec.replicas = 10;
        JsonObject expectedUpdate = asJson(patch);

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).patch(replicationControllerPath(NAMESPACE, RC_NAME), expectedUpdate);
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * Deployment.
     */
    @Test
    public void setDesiredSizeOnDeployment() throws Exception {
        this.cloudpool.configure(asJson(config(DEPLOYMENT_POD_POOL)));
        this.cloudpool.start();

        Future<?> update = this.cloudpool.setDesiredSize(10);

        // wait for update to complete
        update.get();

        // expected patch
        Deployment patch = new Deployment();
        patch.spec = new DeploymentSpec();
        patch.spec.replicas = 10;
        JsonObject expectedUpdate = asJson(patch);

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).patch(deploymentPath(NAMESPACE, DEPLOYMENT_NAME), expectedUpdate);
    }

    /**
     * Make sure calls are made to the right URLs when set up to manage a
     * ReplicaSet.
     */
    @Test
    public void setDesiredSizeOnReplicaSet() throws Exception {
        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        Future<?> update = this.cloudpool.setDesiredSize(10);
        // wait for update to complete
        update.get();

        // expected patch
        ReplicaSet patch = new ReplicaSet();
        patch.spec = new ReplicaSetSpec();
        patch.spec.replicas = 10;
        JsonObject expectedUpdate = asJson(patch);

        // verify that a calls were made on the right URLs
        verify(this.mockApiServer).patch(replicaSetPath(NAMESPACE, RS_NAME), expectedUpdate);
    }

    /**
     * On failure to set the desired size, a {@link CloudPoolException} should
     * be thrown.
     */
    @Test
    public void setDesiredSizeOnError() throws Exception {
        Status status = FakeKubeErrors.errorStatus(404, String.format("replicasets \"%s\" not found", RS_NAME));
        when(this.mockApiServer.patch(argThat(is(replicaSetPath(NAMESPACE, RS_NAME))),
                argThat(is(any(JsonObject.class))))).thenThrow(new HttpResponseException(404, status.toString()));

        this.cloudpool.configure(asJson(config(RS_POD_POOL)));
        this.cloudpool.start();

        Future<?> update = this.cloudpool.setDesiredSize(10);

        try {
            // wait for update to complete
            update.get();
            fail("expected to fail");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(CloudPoolException.class));
        }
    }

    @Test(expected = NotStartedException.class)
    public void getMachinePoolBeforeStarted() {
        this.cloudpool.getMachinePool();
    }

    @Test(expected = NotStartedException.class)
    public void getPoolSizeBeforeStarted() {
        this.cloudpool.getPoolSize();
    }

    @Test(expected = NotStartedException.class)
    public void setDesiredSizeBeforeStarted() {
        this.cloudpool.setDesiredSize(1);
    }

    /**
     * Not all cloudpool REST API methods are supported.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void attachMachineNotImplemented() {
        this.cloudpool.attachMachine("nginx-123");
    }

    /**
     * Not all cloudpool REST API methods are supported.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void detachMachineNotImplemented() {
        this.cloudpool.detachMachine("nginx-123", true);
    }

    /**
     * Not all cloudpool REST API methods are supported.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void setMembershipStatusNotImplemented() {
        this.cloudpool.setMembershipStatus("nginx-123", MembershipStatus.blessed());
    }

    /**
     * Not all cloudpool REST API methods are supported.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void setServiceStateNotImplemented() {
        this.cloudpool.setServiceState("nginx-123", ServiceState.BOOTING);
    }

    /**
     * Not all cloudpool REST API methods are supported.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void terminateMachineNotImplemented() {
        this.cloudpool.terminateMachine("nginx-123", true);
    }

    private static JsonObject asJson(Object object) {
        return JsonUtils.toJson(object).getAsJsonObject();
    }

    private static String replicationControllerPath(String namespace, String name) {
        return MessageFormat.format("/api/v1/namespaces/{0}/replicationcontrollers/{1}", namespace, name);
    }

    private static String deploymentPath(String namespace, String name) {
        return MessageFormat.format("/apis/extensions/v1beta1/namespaces/{0}/deployments/{1}", namespace, name);
    }

    private static String replicaSetPath(String namespace, String name) {
        return MessageFormat.format("/apis/extensions/v1beta1/namespaces/{0}/replicasets/{1}", namespace, name);
    }

    private static String podsQueryPath(String namespace, String labelSelector) throws UnsupportedEncodingException {
        String encodedSelector = URLEncoder.encode(labelSelector, StandardCharsets.UTF_8.name());
        return MessageFormat.format("/api/v1/namespaces/{0}/pods?labelSelector={1}", namespace, encodedSelector);
    }

    /**
     * Prepares mock API server interactions for a given
     * {@link ReplicationController}. The API server is set up to return the
     * {@link ReplicationController} when called on the expected URL. Also, a
     * number of fake {@link Pod}s are set up when a {@link Pod} listing is
     * requested with the label selector of the {@link ReplicationController}.
     *
     * @param rc
     * @throws Exception
     */
    private void prepareMockApiServer(ReplicationController rc) throws Exception {
        when(this.mockApiServer.get(replicationControllerPath(rc.metadata.namespace, rc.metadata.name)))
                .thenReturn(asJson(rc));
        int numPods = rc.status.replicas;
        String labelSelector = rc.spec.getLabelSelectorExpression();
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, labelSelector)))
                .thenReturn(asJson(podList(numPods, rc.metadata.name)));
    }

    /**
     * Prepares mock API server interactions for a given {@link Deployment}. The
     * API server is set up to return the {@link Deployment} when called on the
     * expected URL. Also, a number of fake {@link Pod}s are set up when a
     * {@link Pod} listing is requested with the label selector of the
     * {@link Deployment}.
     *
     * @param deployment
     * @throws Exception
     */
    private void prepareMockApiServer(Deployment deployment) throws Exception {
        when(this.mockApiServer.get(deploymentPath(deployment.metadata.namespace, deployment.metadata.name)))
                .thenReturn(asJson(deployment));
        int numPods = deployment.status.replicas;
        String labelSelector = deployment.spec.selector.toLabelSelectorExpression();
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, labelSelector)))
                .thenReturn(asJson(podList(numPods, deployment.metadata.name)));
    }

    /**
     * Prepares mock API server interactions for a given {@link ReplicaSet}. The
     * API server is set up to return the {@link ReplicaSet} when called on the
     * expected URL. Also, a number of fake {@link Pod}s are set up when a
     * {@link Pod} listing is requested with the label selector of the
     * {@link ReplicaSet}.
     *
     * @param rs
     * @throws Exception
     */
    private void prepareMockApiServer(ReplicaSet rs) throws Exception {
        when(this.mockApiServer.get(replicaSetPath(rs.metadata.namespace, rs.metadata.name))).thenReturn(asJson(rs));
        int numPods = rs.status.replicas;
        String labelSelector = rs.spec.selector.toLabelSelectorExpression();
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, labelSelector)))
                .thenReturn(asJson(podList(numPods, rs.metadata.name)));
    }

    /**
     * Creates a {@link PodList} with a given number of pods. All pod names are
     * prefixed by the given prefix with a sequence number (0..) appended.
     *
     * @param numPods
     * @param podNamePrefix
     * @return
     */
    private static PodList podList(int numPods, String podNamePrefix) {
        PodList podList = new PodList();
        podList.items = new ArrayList<>();
        for (int i = 0; i < numPods; i++) {
            Pod pod = new Pod();
            pod.apiVersion = "v1";
            pod.kind = "Pod";
            pod.metadata = new ObjectMeta();
            pod.metadata.name = podNamePrefix + "-" + i;
            pod.spec = new PodSpec();
            pod.status = new PodStatus();
            pod.status.phase = "Running";
            pod.status.hostIP = "100.100.100." + i;
            pod.status.podIP = "10.0.0." + i;
            pod.status.startTime = UtcTime.now().minusMinutes(5);
            podList.items.add(pod);
        }

        return podList;
    }

    /**
     * Creates a {@link KubernetesCloudPoolConfig} with a given {@link PodPool}
     * type.
     *
     * @param podPoolConfig
     * @return
     */
    private static KubernetesCloudPoolConfig config(PodPoolConfig podPoolConfig) {
        return new KubernetesCloudPoolConfig(API_SERVER_URL, CERT_AUTH, podPoolConfig, UPDATE_INTERVAL, null);
    }
}
