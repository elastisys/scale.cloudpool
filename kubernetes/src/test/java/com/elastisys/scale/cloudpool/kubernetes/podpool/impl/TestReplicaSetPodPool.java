package com.elastisys.scale.cloudpool.kubernetes.podpool.impl;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPoolSize;
import com.elastisys.scale.cloudpool.kubernetes.testutils.FakeKubeErrors;
import com.elastisys.scale.cloudpool.kubernetes.testutils.ReplicaSetBuilder;
import com.elastisys.scale.cloudpool.kubernetes.types.LabelSelectorRequirement;
import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodList;
import com.elastisys.scale.cloudpool.kubernetes.types.PodSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.PodStatus;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSet;
import com.elastisys.scale.cloudpool.kubernetes.types.ReplicaSetSpec;
import com.elastisys.scale.cloudpool.kubernetes.types.Status;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link ReplicaSetPodPool} to make sure that it makes proper API
 * calls against the Kubernetes API server.
 */
public class TestReplicaSetPodPool {
    /** namespace where Deployment exists. */
    private static final String NAMESPACE = "my-ns";
    private static final String NAMESPACE2 = "my-ns2";
    /** Deployment name. */
    private static final String RS_NAME = "nginx-rs";
    private static final String RS_NAME2 = "nginx-rs2";

    private ApiServerClient mockApiServer = mock(ApiServerClient.class);

    /** Object under test. */
    private PodPool podPool;

    @Before
    public void beforeTestMethod() {
        this.podPool = new ReplicaSetPodPool().configure(this.mockApiServer, NAMESPACE, RS_NAME);
    }

    /**
     * When a configuration with a new ReplicaSet is set, subsequent operations
     * should request data about the new ReplicaSet.
     */
    @Test
    public void reconfigure() throws Exception {
        ReplicaSet rs = ReplicaSetBuilder.create().addTemplateLabel("app", "nginx").build();
        when(this.mockApiServer.get(argThat(is(any(String.class))))).thenReturn(asJson(rs));

        // configure
        this.podPool = new ReplicaSetPodPool().configure(this.mockApiServer, NAMESPACE, RS_NAME);
        this.podPool.getSize();
        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));

        // reconfigure
        this.podPool.configure(this.mockApiServer, NAMESPACE2, RS_NAME2);
        this.podPool.getSize();
        // verify that call to get replication controller requested right path
        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE2, RS_NAME2));

    }

    /**
     * The {@link PodPoolSize}, should be determined from the desired
     * ({@code spec.replicas}) and actual number of replicas
     * ({@code status.replicas}).
     */
    @Test
    public void getSize() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME).addTemplateLabel("app", "nginx")
                .desiredReplicas(3).replicas(2).build();

        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME))).thenReturn(asJson(rs));

        PodPoolSize size = this.podPool.getSize();
        assertThat(size.getDesiredReplicas(), is(3));
        assertThat(size.getActualReplicas(), is(2));

        // verify expected call to api server
        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
    }

    /**
     * API error responses should be translated to a
     * {@link KubernetesApiException}.
     */
    @Test(expected = KubernetesApiException.class)
    public void onErrorResponse() throws Exception {
        Status status = FakeKubeErrors.errorStatus(404, String.format("replicasets \"%s\" not found", RS_NAME));
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME)))
                .thenThrow(new HttpResponseException(404, status.toString()));
        this.podPool.getSize();
    }

    /**
     * Make sure that a proper {@link Pod} listing URL is used for a
     * {@link ReplicaSet} that specifies an <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#equality-based-requirement">equality-based
     * label-selector</a> in the {@code spec.selector.matchLabels} field.
     */
    @Test
    public void getPodsWithEqualityBasedLabelSelector() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME)//
                .addTemplateLabel("app", "nginx").addTemplateLabel("version", "1.11.0")
                .addTemplateLabel("env", "production")//
                .addMatchLabel("app", "nginx").addMatchLabel("env", "production")//
                .build();
        String expectedSelector = "app=nginx,env=production";
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME))).thenReturn(asJson(rs));
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, expectedSelector))).thenReturn(asJson(podList(2)));

        List<Pod> pods = this.podPool.getPods();
        assertThat(pods, is(podList(2).items));

        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, expectedSelector));
    }

    /**
     * Make sure that a proper {@link Pod} listing URL is used for a
     * {@link ReplicaSet} that specifies a <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#set-based-requirement">set-based
     * label-selector</a> in the {@code spec.selector.matchLabels} field.
     */
    @Test
    public void getPodsWithSetBasedLabelSelector() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME)//
                .addTemplateLabel("app", "nginx").addTemplateLabel("version", "1.11.0")
                .addTemplateLabel("env", "production")//
                .addMatchExpression(LabelSelectorRequirement.in("app", "nginx")) //
                .addMatchExpression(LabelSelectorRequirement.in("version", "1.11.0", "1.11.1"))//
                .addMatchExpression(LabelSelectorRequirement.notin("env", "testing")).build();
        String expectedSelector = "app in (nginx),version in (1.11.0,1.11.1),env notin (testing)";
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME))).thenReturn(asJson(rs));
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, expectedSelector))).thenReturn(asJson(podList(2)));

        List<Pod> pods = this.podPool.getPods();
        assertThat(pods, is(podList(2).items));

        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, expectedSelector));
    }

    /**
     * Make sure that a proper {@link Pod} listing URL is used for a
     * {@link ReplicaSet} that specifies both a <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#equality-based-requirement">equality-based
     * label-selector</a> and a <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#set-based-requirement">set-based
     * label-selector</a> in the {@code spec.selector.matchLabels} field.
     */
    @Test
    public void getPodsWithBothEqualityAndSetBasedLabelSelector() throws Exception {
        // prepare a fake metadata object that the api server will respond with
        ReplicaSet rs = ReplicaSetBuilder.create().namespace(NAMESPACE).name(RS_NAME)//
                .addTemplateLabel("app", "nginx").addTemplateLabel("version", "1.11.0")
                .addTemplateLabel("env", "production")//
                .addMatchExpression(LabelSelectorRequirement.in("version", "1.11.0", "1.11.1")) //
                .addMatchExpression(LabelSelectorRequirement.notin("env", "testing")) //
                .addMatchLabel("app", "nginx").build();
        String expectedSelector = "version in (1.11.0,1.11.1),env notin (testing),app=nginx";
        when(this.mockApiServer.get(replicaSetPath(NAMESPACE, RS_NAME))).thenReturn(asJson(rs));
        when(this.mockApiServer.get(podsQueryPath(NAMESPACE, expectedSelector))).thenReturn(asJson(podList(2)));

        List<Pod> pods = this.podPool.getPods();
        assertThat(pods, is(podList(2).items));

        verify(this.mockApiServer).get(replicaSetPath(NAMESPACE, RS_NAME));
        verify(this.mockApiServer).get(podsQueryPath(NAMESPACE, expectedSelector));
    }

    /**
     * A new desired size is set by sending a {@code PATCH} request to the api
     * server.
     */
    @Test
    public void setDesiredSize() throws Exception {
        this.podPool.setDesiredSize(10);

        ReplicaSet expectedPatch = new ReplicaSet();
        expectedPatch.spec = new ReplicaSetSpec();
        expectedPatch.spec.replicas = 10;

        verify(this.mockApiServer).patch(replicaSetPath(NAMESPACE, RS_NAME), asJson(expectedPatch));
    }

    private static JsonObject asJson(Object replicationController) {
        return JsonUtils.toJson(replicationController).getAsJsonObject();
    }

    private static String replicaSetPath(String namespace, String name) {
        return MessageFormat.format("/apis/extensions/v1beta1/namespaces/{0}/replicasets/{1}", namespace, name);
    }

    private static String podsQueryPath(String namespace, String labelSelector) throws UnsupportedEncodingException {
        String encodedSelector = URLEncoder.encode(labelSelector, StandardCharsets.UTF_8.name());
        return MessageFormat.format("/api/v1/namespaces/{0}/pods?labelSelector={1}", namespace, encodedSelector);
    }

    private PodList podList(int numPods) {
        PodList podList = new PodList();
        podList.apiVersion = "v1";
        podList.kind = "PodList";
        podList.items = new ArrayList<>();
        for (int i = 0; i < numPods; i++) {
            Pod pod = new Pod();
            pod.metadata = new ObjectMeta();
            pod.spec = new PodSpec();
            pod.status = new PodStatus();
            pod.status.phase = "Running";
            pod.status.hostIP = "123.123.123.123";
            pod.status.podIP = "10.0.0.2";
            pod.status.startTime = UtcTime.parse("2017-01-01T12:00:00.000Z");
            podList.items.add(pod);
        }
        return podList;
    }
}