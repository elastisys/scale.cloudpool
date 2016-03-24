package com.elastisys.scale.cloudpool.kubernetes.client.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.kubernetes.client.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.client.KubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.http.HttpApiClient;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link KubernetesClient} that speaks to the Kubernetes apiserver according
 * to the settings specified in a {@link KubernetesCloudPoolConfig}.
 *
 */
public class StandardKubernetesClient implements KubernetesClient {
	private static final Logger LOG = LoggerFactory
			.getLogger(StandardKubernetesClient.class);

	/**
	 * The client that will be used to communicate with the Kubernetes
	 * apiserver.
	 */
	private final HttpApiClient httpClient;

	/**
	 * The currently set configuration, which describes how to connect to the
	 * Kubernetes apiserver.
	 */
	private KubernetesCloudPoolConfig config;

	/**
	 * Creates a new {@link StandardKubernetesClient} which will communicate
	 * with the Kubernetes apiserver using the provided {@link HttpApiClient}.
	 *
	 * @param httpClient
	 */
	public StandardKubernetesClient(HttpApiClient httpClient) {
		this.httpClient = httpClient;
		this.config = null;

	}

	@Override
	public void configure(KubernetesCloudPoolConfig config) {
		this.config = config;
		this.httpClient.configure(config.getAuth());
	}

	private void ensureConfigured() throws IllegalStateException {
		if (this.config == null) {
			throw new IllegalStateException(
					"kubernetes apiserver client not configured yet");
		}
	}

	/**
	 * Verifies and returns the configured ReplicationController if it exists
	 * and is reachable. If not, a {@link KubernetesApiException} is thrown.
	 *
	 * @return The metadata and status of the configured ReplicationController.
	 * @throws KubernetesApiException
	 */
	private JsonObject ensureReplicationController()
			throws KubernetesApiException {
		try {
			return getReplicationController();
		} catch (Exception e) {
			throw new KubernetesApiException(
					String.format(
							"cannot complete API request: the "
									+ "configured replication controller "
									+ "could not be reached: %s",
							e.getMessage()),
					e);
		}
	}

	@Override
	public PoolSizeSummary getPoolSize() throws KubernetesApiException {
		ensureConfigured();
		JsonObject rc = ensureReplicationController();

		try {
			JsonObject spec = rc.get("spec").getAsJsonObject();

			int desired = spec.get("replicas").getAsInt();
			JsonObject status = rc.get("status").getAsJsonObject();
			int running = status.get("replicas").getAsInt();
			return new PoolSizeSummary(desired, running, running);
		} catch (Exception e) {
			throw new KubernetesApiException(String.format(
					"failed to retrieve pod pool size: %s", e.getMessage()), e);
		}
	}

	@Override
	public MachinePool getMachinePool() throws KubernetesApiException {
		ensureConfigured();
		JsonObject rc = ensureReplicationController();

		List<Machine> machines = new ArrayList<>();
		try {
			Map<String, String> podSelector = getPodSelector(rc);
			JsonObject pods = getPods(podSelector);
			JsonArray podItems = pods.get("items").getAsJsonArray();
			Iterator<JsonElement> podIterator = podItems.iterator();
			while (podIterator.hasNext()) {
				JsonObject podItem = podIterator.next().getAsJsonObject();
				machines.add(new PodToMachine().apply(podItem));
			}
		} catch (Exception e) {
			throw new KubernetesApiException(
					String.format("failed to retrieve pod pool members: %s",
							e.getMessage()),
					e);
		}
		return new MachinePool(machines, UtcTime.now());
	}

	@Override
	public void setDesiredSize(int desiredSize) throws KubernetesApiException {
		ensureConfigured();
		ensureReplicationController();

		try {
			JsonObject update = JsonUtils
					.parseJsonString(String.format(
							"{\"spec\": {\"replicas\": %d}}", desiredSize))
					.getAsJsonObject();
			JsonObject response = this.httpClient
					.patch(replicationControllerUrl(), update);
			LOG.debug("response: {}", JsonUtils.toPrettyString(response));
		} catch (Exception e) {
			throw new KubernetesApiException(String.format(
					"failed to update pod pool size: %s", e.getMessage()), e);
		}

	}

	/**
	 * Extracts the pod selector(s) from a given ReplicationController metadata
	 * object (as returned by {@link #getReplicationController()}). The pod
	 * selector is a map of key:value pairs assigned to the set of pods that
	 * this replication controller is responsible for managing.
	 *
	 * @param replicationController
	 *            A replication controller metadata JSON object.
	 * @return
	 */
	private Map<String, String> getPodSelector(
			JsonObject replicationController) {
		Map<String, String> podSelector = new HashMap<>();
		JsonObject spec = replicationController.get("spec").getAsJsonObject();
		JsonObject selectors = spec.get("selector").getAsJsonObject();
		for (Entry<String, JsonElement> selector : selectors.entrySet()) {
			podSelector.put(selector.getKey(),
					selector.getValue().getAsString());
		}

		return podSelector;
	}

	/**
	 * Retrieves metadata and status for the replication controller as described
	 * <a href=
	 * "http://kubernetes.io/docs/api-reference/v1/operations/#_read_the_specified_replicationcontroller">
	 * in the API docs</a>. This call is analogous to
	 *
	 * <pre>
	 * curl [options] https://kubernetes:443/api/v1/namespaces/default/replicationcontrollers/nginx
	 * </pre>
	 *
	 * or
	 *
	 * <pre>
	 * kubectl get rc nginx --output=json
	 * </pre>
	 *
	 * @return JSON metadata for the replication controller.
	 * @throws HttpResponseException
	 * @throws IOException
	 */
	private JsonObject getReplicationController()
			throws HttpResponseException, IOException {
		return this.httpClient.get(replicationControllerUrl());
	}

	private String replicationControllerUrl() {
		String baseUrl = nsQualifiedBaseUrl();
		String url = String.format("%s/replicationcontrollers/%s", baseUrl,
				this.config.getPodPool().getReplicationController());
		return url;
	}

	/**
	 * Retrieves metadata about all pods with the given {@code labelSelector},
	 * as described <a href=
	 * "http://kubernetes.io/docs/api-reference/v1/operations/#_list_or_watch_objects_of_kind_pod">
	 * in the API docs</a>. This call is analogous to
	 *
	 * <pre>
	 * curl [options] https://kubernetes:443/api/v1/namespaces/default/pods?labelSelector=app%3Dnginx
	 * </pre>
	 *
	 * or
	 *
	 * <pre>
	 * kubectl get pods --selector="app=nginx" --output=json
	 * </pre>
	 *
	 * @param labelSelector
	 *            The
	 *            <a href="http://kubernetes.io/docs/user-guide/labels/">label
	 *            selector</a> used to filter the set of returned pods.
	 * @return JSON metadata for the pods.
	 * @throws HttpResponseException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private JsonObject getPods(Map<String, String> labelSelector)
			throws HttpResponseException, IOException, URISyntaxException {
		String baseUrl = nsQualifiedBaseUrl();
		String podSelector = Joiner.on(",").join(labelSelector.entrySet());
		String url = String.format("%s/pods?labelSelector=%s", baseUrl,
				URLEncoder.encode(podSelector, Charsets.UTF_8.name()));
		LOG.debug("getting pods: {}", url);
		return this.httpClient.get(url);
	}

	/**
	 * The namespace-qualified base URL to the configured Kubernetes apiserver.
	 *
	 * @return
	 */
	private String nsQualifiedBaseUrl() {
		return String.format("https://%s:%d/api/v1/namespaces/%s",
				this.config.getApiServer().getHost(),
				this.config.getApiServer().getPort(),
				this.config.getPodPool().getNamespace());
	}

}
