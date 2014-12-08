package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Atomics;

/**
 * A {@link CloudAdapter} that acts as a local proxy to a remotely located
 * {@link CloudAdapter}.
 * <p/>
 * The remote {@link CloudAdapter} is assumed to publish a REST API as described
 * in the documentation for the {@link CloudAdapterRestApi}.
 * <p/>
 * The class is thread-safe by virtue of relying on thread-safe components and
 * can be used without explicit client-side locking.
 */
public class StandardPrioritizedRemoteCloudAdapter implements
		PrioritizedRemoteCloudAdapter {

	private static final int UNSET_PREVIOUS_POOL_SIZE = -1;

	/** {@link Logger} instance. */
	private static final Logger logger = LoggerFactory
			.getLogger(StandardPrioritizedRemoteCloudAdapter.class);

	/** The current configuration set. */
	private final AtomicReference<PrioritizedRemoteCloudAdapterConfig> config;

	/** The last known size of the machine pool. */
	private final AtomicLong lastKnownPoolSize;

	/**
	 * Creates a new instance.
	 */
	public StandardPrioritizedRemoteCloudAdapter() {
		this.config = Atomics.newReference();
		this.lastKnownPoolSize = new AtomicLong(UNSET_PREVIOUS_POOL_SIZE);
	}

	/**
	 * Validates the configuration.
	 *
	 * @param configuration
	 *            The configuration to validate.
	 * @throws ConfigurationException
	 *             Thrown if the configuration cannot be validated, i.e., there
	 *             is something wrong with it (missing or unacceptable value).
	 */
	public static void validate(
			PrioritizedRemoteCloudAdapterConfig configuration)
			throws ConfigurationException {
		try {
			configuration.validate();
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, ConfigurationException.class);
			throw new ConfigurationException(
					"failed to validate cloud adapter configuration: "
							+ e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.elastisys.scale.cloudadapters.splitter.scalinggroup.client.
	 * PrioritizedCloudAdapter
	 * #configure(com.elastisys.scale.cloudadapters.splitter
	 * .scalinggroup.client.PrioritizedRemoteCloudAdapterConfig)
	 */
	@Override
	public void configure(PrioritizedRemoteCloudAdapterConfig configuration)
			throws ConfigurationException {
		validate(configuration);
		this.config.set(configuration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.elastisys.scale.cloudadapters.splitter.scalinggroup.client.
	 * PrioritizedCloudAdapter#getPriority()
	 */
	@Override
	public int getPriority() {
		return getOrThrowConfig().getPriority();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.elastisys.scale.cloudadapters.splitter.scalinggroup.client.
	 * PrioritizedCloudAdapter#getMachinePool()
	 */
	@Override
	public MachinePool getMachinePool() throws CloudAdapterException {
		final PrioritizedRemoteCloudAdapterConfig config = getOrThrowConfig();

		// send GET request to remote cloud adapter's REST API
		String url = format("%s%s", cloudAdapterBaseUrl(config), "/pool/");

		AuthenticatedHttpClient client = createClient(config);

		HttpRequestResponse response = null;
		try {
			response = client.execute(new HttpGet(url));
		} catch (Exception e) {
			throw new CloudAdapterException(format(
					"failed to get machine pool from remote "
							+ "cloud adapter at %s: %s", url, e.getMessage()),
					e);
		}

		String responseBody = response.getResponseBody();
		try {
			return MachinePool.fromJson(responseBody);
		} catch (Exception e) {
			throw new CloudAdapterException(format(
					"failed to parse machine pool from remote "
							+ "cloud adapter at %s: %s", url, e.getMessage()),
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.elastisys.scale.cloudadapters.splitter.scalinggroup.client.
	 * PrioritizedCloudAdapter#resizeMachinePool(int)
	 */
	@Override
	public void resizeMachinePool(final long desiredCapacity)
			throws IllegalArgumentException, CloudAdapterException {

		// Get a reference to the current config, so any updates to it do not
		// affect us
		final PrioritizedRemoteCloudAdapterConfig config = getOrThrowConfig();

		// Thread-safely check if we have not set the pool size yet, and if so,
		// set it to the current value
		this.lastKnownPoolSize.compareAndSet(UNSET_PREVIOUS_POOL_SIZE,
				getMachinePool().getMachines().size());

		// send POST request to remote cloud adapter's REST API
		String url = String.format("%s%s", cloudAdapterBaseUrl(config),
				"/pool/");

		AuthenticatedHttpClient client = createClient(config);

		try {
			HttpPost post = new HttpPost(url);
			StringEntity entity = new StringEntity(JsonUtils.toString(JsonUtils
					.toJson(new ResizeRequestMessage(desiredCapacity))),
					ContentType.APPLICATION_JSON);
			post.setEntity(entity);

			this.logger.info("Requesting desired capacity {} at {}",
					desiredCapacity, url);

			HttpRequestResponse response = client.execute(post);
			this.lastKnownPoolSize.set(desiredCapacity);
		} catch (Exception e) {
			final String message = format(
					"failed request to set machine pool size on remote "
							+ "cloud adapter at %s: %s", url, e.getMessage());
			logger.error(message);
			throw new CloudAdapterException(message, e);
		}

		this.logger.info("Done requesting desired capacity {} at {}.",
				desiredCapacity, url);
	}

	/**
	 * Compares the priorities of the prioritized cloud adapters so that higher
	 * priorities are sorted first, i.e., descending order is the most natural
	 * for these adapters.
	 */
	@Override
	public int compareTo(PrioritizedRemoteCloudAdapter o) {
		return -1 * Integer.compare(getPriority(), o.getPriority());
	}

	private PrioritizedRemoteCloudAdapterConfig getOrThrowConfig() {
		final PrioritizedRemoteCloudAdapterConfig config = this.config.get();
		if (config == null) {
			throw new IllegalStateException("Configuration not yet set!");
		} else {
			return config;
		}
	}

	/**
	 * Constructs the base URL for the remote cloud adapter, to be accessed over
	 * HTTPS.
	 *
	 * @return The base URL for the remote cloud adapter's HTTPS endpoint.
	 */
	private static String cloudAdapterBaseUrl(
			final PrioritizedRemoteCloudAdapterConfig config) {
		final String host = config.getCloudAdapterHost();
		final int port = config.getCloudAdapterPort();
		return format("https://%s:%d", host, port);
	}

	private static AuthenticatedHttpClient createClient(
			PrioritizedRemoteCloudAdapterConfig config) {
		AuthenticatedHttpClient client = new AuthenticatedHttpClient(logger,
				config.getBasicCredentials(),
				config.getCertificateCredentials());
		return client;
	}
}
