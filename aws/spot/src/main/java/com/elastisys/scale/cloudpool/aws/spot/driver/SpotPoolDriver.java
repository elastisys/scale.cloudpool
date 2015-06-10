package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.amazonaws.services.ec2.model.InstanceStateName.Pending;
import static com.amazonaws.services.ec2.model.InstanceStateName.Running;
import static com.amazonaws.services.ec2.model.SpotInstanceState.Active;
import static com.amazonaws.services.ec2.model.SpotInstanceState.Cancelled;
import static com.amazonaws.services.ec2.model.SpotInstanceState.Closed;
import static com.amazonaws.services.ec2.model.SpotInstanceState.Open;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingFilters.CLOUD_POOL_TAG_FILTER;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingFilters.INSTANCE_STATE_FILTER;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingFilters.SPOT_REQUEST_ID_FILTER;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingFilters.SPOT_REQUEST_STATE_FILTER;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingTags.MEMBERSHIP_STATUS_TAG;
import static com.elastisys.scale.cloudpool.aws.commons.ScalingTags.SERVICE_STATE_TAG;
import static com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions.toSpotRequestId;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifier;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingFilters;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.spot.functions.InstancePairedSpotRequestToMachine;
import com.elastisys.scale.cloudpool.aws.spot.metadata.InstancePairedSpotRequest;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPoolDriver} that provides a management interface towards a pool
 * of AWS EC2 spot instances. For a detailed description of spot instances,
 * refer to the <a href="EC2 user's guide">http://docs.aws
 * .amazon.com/AWSEC2/latest/UserGuide/using-spot-bid-specifications.html</a>.
 *
 * The {@link SpotPoolDriver} operates according to the {@link CloudPoolDriver}
 * contract. Some details on how the {@link SpotPoolDriver} satisfies the
 * contract are summarized below.
 *
 * <h3>Configuration</h3>
 *
 * When {@link #configure(BaseCloudPoolConfig)} is called, the
 * {@link SpotPoolDriver} expects a {@link BaseCloudPoolConfig} that contains a
 * {@code driverConfig} that contains a {@link SpotPoolDriverConfig}. Refer to
 * the {@link #CONFIG_SCHEMA} for a description of the driver's configuration
 * parameters.
 * <p/>
 *
 * <pre>
 *         {
 *             "cloudPool": {
 *                 "name": "MyScalingPool",
 *                 "driverConfig": {
 *                     "awsAccessKeyId": "ABC...XYZ",
 *                     "awsSecretAccessKey": "abc...123",
 *                     "region": "us-east-1",
 *                     "bidPrice": 0.007,
 *                     "bidReplacementPeriod": 120,
 *                     "danglingInstanceCleanupPeriod": 120
 *                 }
 *             },
 *             ... rest of BaseCloudPool configuration
 *         }
 * </pre>
 *
 * <h3>Identifying pool members</h3>
 * The {@link SpotPoolDriver} tracks members of the spot pool via a
 * {@link ScalingTags#CLOUD_POOL_TAG} tag. All spot requests marked with the tag
 * (and their instances, if fulfilled) are considered pool members.
 *
 * <h4>Converting spot instance requests to {@link Machine} instances</h4>: At
 * any time, some spot instance requests may be <i>fulfilled</i> (assigned an
 * instance) whereas others may be <i>unfulfilled</i> (waiting for an instance
 * to become available at the right price).
 * <p/>
 * Converting fulfilled spot instance requests to the {@link Machine} type is
 * straightforward -- the instance's metadata is simply translated to populate a
 * {@link Machine} object.
 * <p/>
 * The case is a little different for unfulfilled spot instance requests. These
 * are reported as {@link Machine} instances with a {@link MachineState} of
 * {@code REQUESTED} and no {@code launchtime} set.
 *
 * <h4>Starting machines</h4>
 * When asked to start new {@link Machine}s, one spot instance request is placed
 * for each requested machine. All spot requests that belong to the cloud pool
 * are tagged with a {@link ScalingTags#CLOUD_POOL_TAG} whose value is taken
 * from the {@code cloudPool/name} configuration key. Persistent spot requests
 * are used to make sure that the request goes back to being open when an
 * assigned spot instance is terminated.
 *
 * <h4>Terminating machines</h4>
 * When asked to terminate a {@link Machine}, the spot instance request in
 * question will be canceled and any associated instance is terminated.
 *
 * <h3>Handling configuration updates that changes the bid price</h3>
 * The new price is considered to be the bid price that will be used <i>from
 * this point on</i>. That is, any placed but still unfulfilled spot requests
 * are re-submitted with the new bid price and any future spot requests are
 * placed with the new bid price. Already fulfilled spot requests (with running
 * instances) are left running at the old bid price. If the user wishes to
 * discard them, this needs to be done manually.
 *
 * <h3>Periodical tasks</h3>: The {@link SpotPoolDriver} executes background
 * tasks in order to:
 * <ul>
 * <li>Replace unfulfilled spot requests with a bid price that isn't up-to-date
 * with the configured bid price.</li>
 * <li>Clean up dangling instances, whose spot requests have been canceled.
 * Normally the instance will be terminated when canceling its spot request,
 * however there is a time-window when the instance may be assigned after we
 * decide to cancel the request, which will leave a dangling spot instance
 * without an active spot request.</li>
 * <li>Clean up instances that have started running even though the
 * corresponding spot request has been canceled.</li>
 * </ul>
 *
 * @see BaseCloudPool
 */
public class SpotPoolDriver implements CloudPoolDriver {
	private static Logger LOG = LoggerFactory.getLogger(SpotPoolDriver.class);

	/** Maximum number of threads to run in {@link #threadPool}. */
	private static final int MAX_THREADS = 5;

	/** Logical name of the managed machine pool. */
	private final AtomicReference<String> poolName;
	/** Full {@link CloudPool} configuration, including driver configuration. */
	private final AtomicReference<BaseCloudPoolConfig> poolConfig;
	/** Driver configuration. */
	private final AtomicReference<SpotPoolDriverConfig> driverConfig;

	private final ScheduledExecutorService threadPool;

	/** The client used to communicate with the EC2 API. */
	private final SpotClient client;

	/**
	 * Supported API versions by this implementation.
	 */
	private final static List<String> supportedApiVersions = Arrays
			.asList(ApiVersion.LATEST);
	/**
	 * Cloud pool metadata for this implementation.
	 */
	private final static CloudPoolMetadata cloudPoolMetadata = new CloudPoolMetadata(
			PoolIdentifier.AWS_SPOT_INSTANCES, supportedApiVersions);

	public SpotPoolDriver(SpotClient client) {
		this.client = client;
		this.poolName = Atomics.newReference();
		this.poolConfig = Atomics.newReference();
		this.driverConfig = Atomics.newReference();
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setDaemon(true).setNameFormat("spotdriver-tasks-%d").build();
		this.threadPool = Executors.newScheduledThreadPool(MAX_THREADS,
				threadFactory);
	}

	@Override
	public void configure(BaseCloudPoolConfig configuration)
			throws CloudPoolDriverException {
		CloudPoolConfig cloudPoolConfig = configuration.getCloudPool();
		checkArgument(cloudPoolConfig != null, "missing cloudPool config");
		JsonObject driverConfig = cloudPoolConfig.getDriverConfig();
		checkArgument(driverConfig != null, "missing driverConfig");

		try {
			SpotPoolDriverConfig newDriverConfig = JsonUtils.toObject(
					driverConfig, SpotPoolDriverConfig.class);
			newDriverConfig.validate();

			this.poolName.set(cloudPoolConfig.getName());
			this.poolConfig.set(configuration);
			this.driverConfig.set(newDriverConfig);
			this.client.configure(newDriverConfig.getAwsAccessKeyId(),
					newDriverConfig.getAwsSecretAccessKey(),
					newDriverConfig.getRegion());
			start();
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to apply configuration: %s", e.getMessage()), e);
		}
	}

	/**
	 * Starts periodical cleanup tasks.
	 */
	private void start() {
		LOG.info("starting periodical execution of cleanup tasks");
		long period = driverConfig().getDanglingInstanceCleanupPeriod();
		this.threadPool.scheduleWithFixedDelay(new DanglingInstanceCleaner(),
				period, period, TimeUnit.SECONDS);

		long bidReplacePeriod = driverConfig().getBidReplacementPeriod();
		this.threadPool.scheduleWithFixedDelay(
				new WrongBidPriceRequestReplacer(), bidReplacePeriod,
				bidReplacePeriod, TimeUnit.SECONDS);
	}

	@Override
	public List<Machine> listMachines() throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			List<InstancePairedSpotRequest> requestInstancePairs = getAlivePoolSpotRequests();
			return Lists.transform(requestInstancePairs,
					new InstancePairedSpotRequestToMachine());
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to retrieve machines in cloud pool \"%s\": %s",
					getPoolName(), e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> startMachines(int count, ScaleOutConfig scaleOutConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured driver");
		List<Machine> startedMachines = Lists.newArrayList();
		try {
			for (int i = 0; i < count; i++) {
				SpotInstanceRequest newSpotRequest = this.client
						.placeSpotRequest(driverConfig().getBidPrice(),
								scaleOutConfig);
				LOG.info("placed spot request {}",
						newSpotRequest.getSpotInstanceRequestId());
				InstancePairedSpotRequest pairedSpotRequest = new InstancePairedSpotRequest(
						newSpotRequest, null);
				startedMachines.add(InstancePairedSpotRequestToMachine
						.convert(pairedSpotRequest));

				// set pool membership tag to make sure spot request is
				// recognized as a pool member
				setPoolMembershipTag(newSpotRequest);
			}
		} catch (Exception e) {
			throw new StartMachinesException(count, startedMachines, e);
		}

		return startedMachines;
	}

	@Override
	public void terminateMachine(String spotRequestId)
			throws NotFoundException, CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		LOG.debug("terminating spot request {}", spotRequestId);
		try {
			verifyPoolMember(spotRequestId);

			InstancePairedSpotRequest instancePairedSpotRequest = getSpotRequestWithInstance(spotRequestId);
			SpotInstanceRequest request = instancePairedSpotRequest
					.getRequest();
			// cancel spot request
			this.client.cancelSpotRequest(request.getSpotInstanceRequestId());
			if (instancePairedSpotRequest.hasInstance()) {
				// terminate spot instance (if spot request is fulfilled)
				String instanceId = instancePairedSpotRequest.getInstance()
						.getInstanceId();
				LOG.debug("terminating {}'s spot instance {}", spotRequestId,
						instanceId);
				this.client.terminateInstance(instanceId);
			}
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			String message = format(
					"failed to terminate spot request \"%s\": %s",
					spotRequestId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void attachMachine(String spotRequestId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			SpotInstanceRequest spotRequest = verifySpotRequestExistance(spotRequestId);

			setPoolMembershipTag(spotRequest);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to attach '%s' to cloud pool: %s", spotRequestId,
					e.getMessage()), e);
		}
	}

	@Override
	public void detachMachine(String spotRequestId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			verifyPoolMember(spotRequestId);

			this.client.untagResource(spotRequestId,
					asList(poolMembershipTag()));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to attach '%s' to cloud pool: %s", spotRequestId,
					e.getMessage()), e);
		}
	}

	@Override
	public void setServiceState(String spotRequestId, ServiceState serviceState)
			throws NotFoundException, CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			verifyPoolMember(spotRequestId);

			Tag serviceStateTag = new Tag().withKey(SERVICE_STATE_TAG)
					.withValue(serviceState.name());
			this.client.tagResource(spotRequestId, asList(serviceStateTag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to set service state for %s: %s", spotRequestId,
					e.getMessage()), e);
		}
	}

	@Override
	public void setMembershipStatus(String spotRequestId,
			MembershipStatus membershipStatus) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			verifyPoolMember(spotRequestId);

			Tag membershipStatusTag = new Tag().withKey(MEMBERSHIP_STATUS_TAG)
					.withValue(membershipStatus.toString());
			this.client.tagResource(spotRequestId, asList(membershipStatusTag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to set membership status for %s: %s",
					spotRequestId, e.getMessage()), e);
		}
	}

	@Override
	public String getPoolName() {
		checkState(isConfigured(), "attempt to use unconfigured driver");
		return this.poolName.get();
	}

	@Override
	public CloudPoolMetadata getMetadata() {
		return cloudPoolMetadata;
	}

	private boolean isConfigured() {
		return this.poolConfig.get() != null;
	}

	/**
	 * Returns all {@code open} or {@code active} spot requests in the managed
	 * pool.
	 *
	 * @return The {@link SpotInstanceRequest}s paired with their
	 *         {@link Instance} (if fulfilled).
	 */
	private List<InstancePairedSpotRequest> getAlivePoolSpotRequests() {
		return getPoolSpotRequests(Arrays.asList(Open.toString(),
				Active.toString()));
	}

	/**
	 * Returns all {@link SpotInstanceRequest}s in the pool that are in any of a
	 * given set of states.
	 *
	 * @param inStates
	 *            The spot request states of interest.
	 * @return The {@link SpotInstanceRequest}s paired with their
	 *         {@link Instance}.
	 */
	private List<InstancePairedSpotRequest> getPoolSpotRequests(
			List<String> states) {
		// only include spot requests with cloud pool tag
		Filter poolFilter = new Filter().withName(
				ScalingFilters.CLOUD_POOL_TAG_FILTER).withValues(getPoolName());
		// only include spot requests in any of the given states
		Filter stateFilter = new Filter().withName(
				ScalingFilters.SPOT_REQUEST_STATE_FILTER).withValues(states);

		List<SpotInstanceRequest> spotRequests = this.client
				.getSpotInstanceRequests(asList(poolFilter, stateFilter));
		List<InstancePairedSpotRequest> requestInstancePairs = pairUpWithInstances(spotRequests);
		return requestInstancePairs;
	}

	/**
	 * Pairs up each fulfilled {@link SpotInstanceRequest} with its assigned
	 * {@link Instance} in a {@link InstancePairedSpotRequest}. Unfulfilled
	 * {@link SpotInstanceRequest}s are returned without a paired
	 * {@link Instance}.
	 *
	 * @param spotRequests
	 * @return
	 */
	private List<InstancePairedSpotRequest> pairUpWithInstances(
			List<SpotInstanceRequest> spotRequests) {
		List<InstancePairedSpotRequest> pairs = Lists.newArrayList();

		for (SpotInstanceRequest spotRequest : spotRequests) {
			String assignedInstanceId = spotRequest.getInstanceId();
			Instance spotInstance = null;
			if (assignedInstanceId != null) {
				spotInstance = this.client
						.getInstanceMetadata(assignedInstanceId);
			}
			pairs.add(new InstancePairedSpotRequest(spotRequest, spotInstance));
		}
		return pairs;
	}

	/**
	 * Retrieves a {@link SpotInstanceRequest} and, if fulfilled, its associated
	 * {@link Instance}.
	 *
	 * @param spotRequestId
	 * @return
	 */
	private InstancePairedSpotRequest getSpotRequestWithInstance(
			String spotRequestId) {
		SpotInstanceRequest request = this.client
				.getSpotInstanceRequest(spotRequestId);
		Instance instance = null;
		if (request.getInstanceId() != null) {
			instance = this.client.getInstanceMetadata(request.getInstanceId());
		}
		return new InstancePairedSpotRequest(request, instance);
	}

	/**
	 * Sets the pool membership tag ({@link ScalingTags#CLOUD_POOL_TAG}) on a
	 * {@link SpotInstanceRequest}.
	 *
	 * @param spotInstanceRequest
	 */
	private void setPoolMembershipTag(SpotInstanceRequest spotInstanceRequest) {
		this.client.tagResource(spotInstanceRequest.getSpotInstanceRequestId(),
				asList(poolMembershipTag()));
	}

	private Tag poolMembershipTag() {
		return new Tag().withKey(ScalingTags.CLOUD_POOL_TAG).withValue(
				getPoolName());
	}

	SpotClient client() {
		return this.client;
	}

	BaseCloudPoolConfig poolConfig() {
		return this.poolConfig.get();
	}

	SpotPoolDriverConfig driverConfig() {
		return this.driverConfig.get();
	}

	/**
	 * Verifies that a particular {@link SpotInstanceRequest} exists and is a
	 * member of the pool. If it is not tagged with the pool membership tag a
	 * {@link NotFoundException} is thrown.
	 *
	 * @param spotRequestId
	 * @throws NotFoundException
	 * @throws AmazonClientException
	 */
	private void verifyPoolMember(String spotRequestId)
			throws NotFoundException, AmazonClientException {
		Filter idFilter = new Filter(SPOT_REQUEST_ID_FILTER,
				asList(spotRequestId));
		Filter poolFilter = new Filter(CLOUD_POOL_TAG_FILTER,
				asList(getPoolName()));
		List<SpotInstanceRequest> matchingRequests = this.client
				.getSpotInstanceRequests(Arrays.asList(idFilter, poolFilter));
		if (matchingRequests.isEmpty()) {
			throw new NotFoundException(String.format(
					"spot instance request %s is not a member of the pool",
					spotRequestId));
		}
	}

	/**
	 * Verifies that a particular {@link SpotInstanceRequest} exists at all and,
	 * if so, returns it. If not, a {@link NotFoundException} is thrown.
	 *
	 * @param spotRequestId
	 * @return The {@link SpotInstanceRequest}, if it exists.
	 * @throws NotFoundException
	 * @throws AmazonClientException
	 */
	private SpotInstanceRequest verifySpotRequestExistance(String spotRequestId)
			throws NotFoundException, AmazonClientException {
		Filter idFilter = new Filter(SPOT_REQUEST_ID_FILTER,
				asList(spotRequestId));
		List<SpotInstanceRequest> matchingRequests = this.client
				.getSpotInstanceRequests(Arrays.asList(idFilter));
		if (matchingRequests.isEmpty()) {
			throw new NotFoundException(String.format(
					"spot instance request %s does not exist", spotRequestId));
		}
		return Iterables.getOnlyElement(matchingRequests);
	}

	/**
	 * Cleans up any dangling {@link Instance}s (instances whose spot request
	 * has been cancelled).
	 *
	 * @return All {@link Instance}s that were terminated.
	 */
	List<Instance> cleanupDanglingInstances() {
		LOG.info("cleaning up dangling instances (whose spot requests "
				+ "are cancelled) ...");
		// get all dead spot requests (canceled/closed/failed) spot requests
		// belonging to the pool
		Filter poolFilter = new Filter().withName(CLOUD_POOL_TAG_FILTER)
				.withValues(getPoolName());
		// only include spot requests in state
		Filter spotStateFilter = new Filter().withName(
				SPOT_REQUEST_STATE_FILTER).withValues(Cancelled.toString(),
				Closed.toString());
		List<SpotInstanceRequest> deadRequests = client()
				.getSpotInstanceRequests(asList(poolFilter, spotStateFilter));
		List<String> deadRequestIds = transform(deadRequests, toSpotRequestId());

		// get all pending/running instances with a spot instance id equal
		// to any of the dead spot requests
		Filter stateFilter = new Filter().withName(INSTANCE_STATE_FILTER)
				.withValues(Pending.toString(), Running.toString());
		Filter spotRequestFilter = new Filter().withName(
				ScalingFilters.SPOT_REQUEST_ID_FILTER).withValues(
				deadRequestIds);

		List<Instance> danglingInstances = client().getInstances(
				asList(stateFilter, spotRequestFilter));
		for (Instance danglingInstance : danglingInstances) {
			LOG.info("terminating dangling instance {} belonging "
					+ "to dead spot request {}",
					danglingInstance.getInstanceId(),
					danglingInstance.getSpotInstanceRequestId());
			client().terminateInstance(danglingInstance.getInstanceId());
		}
		return danglingInstances;
	}

	/**
	 * Check bid prices for all placed spot requests and replace ones that are
	 * not up-to-date with the currently configured price.
	 *
	 * @return Returns the (possibly empty) list of new spot requests that were
	 *         placed.
	 */
	List<Machine> replaceBids() {
		double currentBidPrice = driverConfig().getBidPrice();
		LOG.info("replacing unfulfilled spot requests with bidprice "
				+ "other than {} ...", currentBidPrice);
		List<InstancePairedSpotRequest> unfulfilledRequests = getPoolSpotRequests(asList(Open
				.toString()));

		List<Machine> placedRequests = Lists.newArrayList();
		for (InstancePairedSpotRequest unfulfilledRequest : unfulfilledRequests) {
			SpotInstanceRequest request = unfulfilledRequest.getRequest();
			double spotPrice = Double.valueOf(request.getSpotPrice());
			// wrong bid price. replace.
			if (spotPrice != currentBidPrice) {
				String requestId = request.getSpotInstanceRequestId();
				LOG.info(
						"replacing unfulfilled spot request {} with wrong bid "
								+ "price {} ", requestId, spotPrice);
				// cancel
				terminateMachine(requestId);
				// replace
				placedRequests.addAll(startMachines(1, poolConfig()
						.getScaleOutConfig()));
			}
		}
		return placedRequests;
	}

	/**
	 * Periodical task that, when run, terminates any spot instances whose spot
	 * request is no longer alive.
	 */
	private class DanglingInstanceCleaner implements Runnable {

		@Override
		public void run() {
			try {
				cleanupDanglingInstances();
			} catch (Exception e) {
				// need to catch exceptions since periodic exeuction will stop
				// on uncaught exceptions
				LOG.error("failed to clean up dangling instances: {}\n{}",
						e.getMessage(), Throwables.getStackTraceAsString(e));
			}
		}
	}

	/**
	 * Periodical task that, when run, finds spot requests in the pool that have
	 * been placed with a bid price different from the currently configured one.
	 * Any such spot requests, that haven't yet been fulfilled, are replaced
	 * with a new spot request with the right bid price.
	 */
	private class WrongBidPriceRequestReplacer implements Runnable {

		@Override
		public void run() {
			try {
				replaceBids();
			} catch (Exception e) {
				// need to catch exceptions since periodic exeuction will stop
				// on uncaught exceptions
				LOG.error("failed to replace wrong bid price requests: {}\n{}",
						e.getMessage(), Throwables.getStackTraceAsString(e));
			}
		}
	}
}
