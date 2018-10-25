package com.elastisys.scale.cloudpool.kubernetes;

import static com.elastisys.scale.cloudpool.kubernetes.AlertMatcher.alertMessage;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.PodPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link KubernetesCloudPool} with respect to sending out
 * {@link Alert}s on notable events.
 */
public class TestKubernetesCloudPoolAlerting {

    /** Sample Kubernetes namespace. */
    private static final String NAMESPACE = "my-ns";
    /** Sample ReplicationController name. */
    private static final String RC_NAME = "nginx-rc";

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
    /** Sample update interval. */
    private static final TimeInterval UPDATE_INTERVAL = TimeInterval.seconds(10);

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    private ApiServerClient mockApiServer = mock(ApiServerClient.class);
    private EventBus mockEventBus = mock(EventBus.class);

    /** Object under test. */
    private KubernetesCloudPool cloudpool;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
        this.executor.setRemoveOnCancelPolicy(true);
        this.cloudpool = new KubernetesCloudPool(this.mockApiServer, this.executor, this.mockEventBus);

        this.cloudpool.configure(asJson(config(RC_POD_POOL)));
        this.cloudpool.start();

        reset(this.mockEventBus);
    }

    /**
     * An INFO-level {@link Alert} should be sent when a new pool size is set.
     */
    @Test
    public void alertOnSetDesiredSize() throws Exception {
        // doNothing().when(this.mockApiServer).patch(argThat(is(any(String.class))),
        // argThat(is(any(JsonObject.class))));

        this.cloudpool.setDesiredSize(10);

        verify(this.mockEventBus).post(argThat(is(alertMessage(AlertTopics.RESIZE, AlertSeverity.INFO))));
    }

    /**
     * A WARN-level {@link Alert} should be sent on failure to set desired size.
     */
    @Test
    public void alertOnSetDesiredSizeError() throws Exception {
        when(this.mockApiServer.patch(argThat(is(any(String.class))), argThat(is(any(JsonObject.class)))))
                .thenThrow(new KubernetesApiException("api error"));

        try {
            Future<?> update = this.cloudpool.setDesiredSize(10);
            // wait for update to complete
            update.get();

            fail("expected to fail");
        } catch (Exception e) {
            // expected
        }

        verify(this.mockEventBus).post(argThat(is(alertMessage(AlertTopics.RESIZE, AlertSeverity.WARN))));
    }

    /**
     * A WARN-level {@link Alert} should be sent on failure to get the machine
     * pool.
     */
    @Test
    public void alertOnGetMachinePoolError() throws Exception {
        when(this.mockApiServer.get(argThat(is(any(String.class))))).thenThrow(new KubernetesApiException("api error"));

        try {
            this.cloudpool.getMachinePool();
            fail("expected to fail");
        } catch (Exception e) {
            // expected
        }

        verify(this.mockEventBus).post(argThat(is(alertMessage(AlertTopics.POOL_FETCH, AlertSeverity.WARN))));
    }

    private static JsonObject asJson(Object object) {
        return JsonUtils.toJson(object).getAsJsonObject();
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
