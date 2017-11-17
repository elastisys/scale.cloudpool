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
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingFilters;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.aws.spot.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.aws.spot.functions.InstancePairedSpotRequestToMachine;
import com.elastisys.scale.cloudpool.aws.spot.metadata.InstancePairedSpotRequest;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;

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
 * <h3>Identifying pool members</h3> The {@link SpotPoolDriver} tracks members
 * of the spot pool via a {@link ScalingTags#CLOUD_POOL_TAG} tag. All spot
 * requests marked with the tag (and their instances, if fulfilled) are
 * considered pool members.
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
 * <h4>Starting machines</h4> When asked to start new {@link Machine}s, one spot
 * instance request is placed for each requested machine. All spot requests that
 * belong to the cloud pool are tagged with a {@link ScalingTags#CLOUD_POOL_TAG}
 * whose value is taken from the {@code cloudPool/name} configuration key.
 * Persistent spot requests are used to make sure that the request goes back to
 * being open when an assigned spot instance is terminated.
 *
 * <h4>Terminating machines</h4> When asked to terminate a {@link Machine}, the
 * spot instance request in question will be canceled and any associated
 * instance is terminated.
 *
 * <h3>Handling configuration updates that changes the bid price</h3> The new
 * price is considered to be the bid price that will be used <i>from this point
 * on</i>. That is, any placed but still unfulfilled spot requests are
 * re-submitted with the new bid price and any future spot requests are placed
 * with the new bid price. Already fulfilled spot requests (with running
 * instances) are left running at the old bid price. If the user wishes to
 * discard them, this needs to be done manually.
 *
 * <h3>Periodical tasks</h3>: The {@link SpotPoolDriver} executes background
 * tasks in order to:
 * <ul>
 * <li>Cancel unfulfilled spot requests with a bid price that isn't up-to-date
 * with the configured bid price. These are eventually replaced when the
 * wrapping {@link BaseCloudPool} detects that the pool is short of requests.
 * </li>
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

    /** The current driver configuration. */
    private DriverConfig config;

    /** Used to run periodical clean-up tasks. */
    private final ScheduledExecutorService executor;

    /** The client used to communicate with the EC2 API. */
    private final SpotClient client;

    /**
     * Used to post {@link Alert}s that are to notify webhook/email recipients
     * configured for the cloud pool (if any).
     */
    private final EventBus eventBus;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    /** Task that periodically runs {@link DanglingInstanceCleaner}. */
    private ScheduledFuture<?> danglingInstanceCleanupTask;

    /** Task that periodically runs {@link WrongPricedRequestCanceller}. */
    private ScheduledFuture<?> wrongPricedRequestCancellerTask;

    /**
     * Creates a new {@link SpotPoolDriver}.
     *
     * @param client
     *            The client used to communicate with the EC2 API.
     * @param executor
     *            Used to run periodical clean-up tasks.
     * @param eventBus
     *            Used to post {@link Alert}s that are to notify webhook/email
     *            recipients configured for the cloud pool (if any).
     */
    public SpotPoolDriver(SpotClient client, ScheduledExecutorService executor, EventBus eventBus) {
        this.client = client;
        this.executor = executor;
        this.eventBus = eventBus;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {
        synchronized (this.lock) {
            // parse and validate openstack-specific cloudApiSettings
            CloudApiSettings cloudApiSettings = configuration.parseCloudApiSettings(CloudApiSettings.class);
            cloudApiSettings.validate();

            // parse and validate openstack-specific provisioningTemplate
            Ec2ProvisioningTemplate provisioningTemplate = configuration
                    .parseProvisioningTemplate(Ec2ProvisioningTemplate.class);
            provisioningTemplate.validate();

            this.config = configuration;
            ClientConfiguration clientConfig = new ClientConfiguration()
                    .withConnectionTimeout(cloudApiSettings.getConnectionTimeout())
                    .withSocketTimeout(cloudApiSettings.getSocketTimeout());
            this.client.configure(cloudApiSettings.getAwsAccessKeyId(), cloudApiSettings.getAwsSecretAccessKey(),
                    cloudApiSettings.getRegion(), clientConfig);
            start();
        }
    }

    /**
     * Starts periodical cleanup tasks.
     */
    private void start() {
        // cancel any prior running tasks
        if (this.danglingInstanceCleanupTask != null) {
            this.danglingInstanceCleanupTask.cancel(true);
        }
        if (this.wrongPricedRequestCancellerTask != null) {
            this.wrongPricedRequestCancellerTask.cancel(true);
        }

        LOG.info("starting periodical execution of cleanup tasks");
        TimeInterval period = cloudApiSettings().getDanglingInstanceCleanupPeriod();
        this.danglingInstanceCleanupTask = this.executor.scheduleWithFixedDelay(new DanglingInstanceCleaner(),
                period.getTime(), period.getTime(), period.getUnit());

        TimeInterval bidReplacePeriod = cloudApiSettings().getBidReplacementPeriod();
        this.wrongPricedRequestCancellerTask = this.executor.scheduleWithFixedDelay(new WrongPricedRequestCanceller(),
                bidReplacePeriod.getTime(), bidReplacePeriod.getTime(), bidReplacePeriod.getUnit());
    }

    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        List<InstancePairedSpotRequest> requestInstancePairs = getAlivePoolSpotRequests();
        return requestInstancePairs.stream().map(new InstancePairedSpotRequestToMachine()).collect(Collectors.toList());
    }

    @Override
    public List<Machine> startMachines(int count) throws StartMachinesException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");
        List<Machine> startedMachines = new ArrayList<>();
        try {
            Ec2ProvisioningTemplate template = provisioningTemplate();
            // add pool tag to recognize spot requests as pool members
            template = template.withTag(ScalingTags.CLOUD_POOL_TAG, getPoolName());

            List<SpotInstanceRequest> spotRequests = this.client.placeSpotRequests(cloudApiSettings().getBidPrice(),
                    template, count);
            List<String> spotIds = spotRequests.stream().map(SpotInstanceRequest::getSpotInstanceRequestId)
                    .collect(Collectors.toList());
            LOG.info("placed spot requests: {}", spotIds);
            for (SpotInstanceRequest spotRequest : spotRequests) {
                InstancePairedSpotRequest pairedSpotRequest = new InstancePairedSpotRequest(spotRequest, null);
                startedMachines.add(InstancePairedSpotRequestToMachine.convert(pairedSpotRequest));
            }
        } catch (Exception e) {
            throw new StartMachinesException(count, startedMachines, e);
        }

        return startedMachines;
    }

    @Override
    public void terminateMachines(List<String> spotRequestIds)
            throws TerminateMachinesException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");
        // defensive copy
        final List<String> victimIds = new ArrayList<>(spotRequestIds);

        LOG.info("request to terminate spot instances: {}", victimIds);

        // track errors
        Map<String, Throwable> failures = new HashMap<>();

        List<InstancePairedSpotRequest> poolSpotRequests = getAlivePoolSpotRequests();

        // only terminate pool members (error mark other requests)
        nonPoolMembers(spotRequestIds, poolSpotRequests).stream().forEach(spotId -> {
            failures.put(spotId, new NotFoundException(
                    String.format("spot instance request %s is not a member of the pool", spotId)));
            victimIds.remove(spotId);
        });

        // none of the machine ids were pool members
        if (victimIds.isEmpty()) {
            throw new TerminateMachinesException(Collections.emptyList(), failures);
        }

        try {
            // cancel spot requests
            LOG.info("cancelling spot requests: {}", victimIds);
            this.client.cancelSpotRequests(victimIds);

            // terminate spot instances (for fulfilled requests)
            List<String> instanceIds = poolSpotRequests.stream() //
                    .filter(r -> victimIds.contains(r.getId())) //
                    .filter(r -> r.hasInstance()) //
                    .map(r -> r.getInstance().getInstanceId()) //
                    .collect(Collectors.toList());
            LOG.info("terminating spot instances: {}", instanceIds);
            this.client.terminateInstances(instanceIds);
        } catch (Exception e) {
            String message = format("failed to terminate spot instances %s: %s", victimIds, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }

        if (!failures.isEmpty()) {
            throw new TerminateMachinesException(victimIds, failures);
        }
    }

    @Override
    public void attachMachine(String spotRequestId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            SpotInstanceRequest spotRequest = verifySpotRequestExistance(spotRequestId);

            setPoolMembershipTag(spotRequest);
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to attach '%s' to cloud pool: %s", spotRequestId, e.getMessage()), e);
        }
    }

    @Override
    public void detachMachine(String spotRequestId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            verifyPoolMember(spotRequestId);

            this.client.untagResource(spotRequestId, asList(poolMembershipTag()));
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to attach '%s' to cloud pool: %s", spotRequestId, e.getMessage()), e);
        }
    }

    @Override
    public void setServiceState(String spotRequestId, ServiceState serviceState)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            verifyPoolMember(spotRequestId);

            Tag serviceStateTag = new Tag().withKey(SERVICE_STATE_TAG).withValue(serviceState.name());
            this.client.tagResource(spotRequestId, asList(serviceStateTag));
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to set service state for %s: %s", spotRequestId, e.getMessage()), e);
        }
    }

    @Override
    public void setMembershipStatus(String spotRequestId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            verifyPoolMember(spotRequestId);

            Tag membershipStatusTag = new Tag().withKey(MEMBERSHIP_STATUS_TAG).withValue(membershipStatus.toString());
            this.client.tagResource(spotRequestId, asList(membershipStatusTag));
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to set membership status for %s: %s", spotRequestId, e.getMessage()), e);
        }
    }

    @Override
    public String getPoolName() {
        checkState(isConfigured(), "attempt to use unconfigured driver");
        return config().getPoolName();
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    DriverConfig config() {
        return this.config;
    }

    SpotClient client() {
        return this.client;
    }

    CloudApiSettings cloudApiSettings() {
        return config().parseCloudApiSettings(CloudApiSettings.class);
    }

    Ec2ProvisioningTemplate provisioningTemplate() {
        return config().parseProvisioningTemplate(Ec2ProvisioningTemplate.class);
    }

    /**
     * Returns all {@code open} or {@code active} spot requests in the managed
     * pool.
     *
     * @return The {@link SpotInstanceRequest}s paired with their
     *         {@link Instance} (if fulfilled).
     * @throws CloudPoolDriverException
     */
    private List<InstancePairedSpotRequest> getAlivePoolSpotRequests() throws CloudPoolDriverException {
        return getPoolSpotRequests(Arrays.asList(Open.toString(), Active.toString()));
    }

    /**
     * Returns all {@link SpotInstanceRequest}s in the pool that are in any of a
     * given set of states.
     *
     * @param inStates
     *            The spot request states of interest.
     * @return The {@link SpotInstanceRequest}s paired with their
     *         {@link Instance}.
     * @throws CloudPoolDriverException
     */
    private List<InstancePairedSpotRequest> getPoolSpotRequests(List<String> states) throws CloudPoolDriverException {
        try {
            // only include spot requests with cloud pool tag
            Filter poolFilter = new Filter().withName(ScalingFilters.CLOUD_POOL_TAG_FILTER).withValues(getPoolName());
            // only include spot requests in any of the given states
            Filter stateFilter = new Filter().withName(ScalingFilters.SPOT_REQUEST_STATE_FILTER).withValues(states);

            List<SpotInstanceRequest> spotRequests = this.client
                    .getSpotInstanceRequests(asList(poolFilter, stateFilter));
            List<InstancePairedSpotRequest> requestInstancePairs = pairUpWithInstances(spotRequests);
            return requestInstancePairs;
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\": %s", getPoolName(), e.getMessage()), e);
        }
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
    private List<InstancePairedSpotRequest> pairUpWithInstances(List<SpotInstanceRequest> spotRequests) {
        List<InstancePairedSpotRequest> pairs = new ArrayList<>();

        for (SpotInstanceRequest spotRequest : spotRequests) {
            String assignedInstanceId = spotRequest.getInstanceId();
            Instance spotInstance = null;
            if (assignedInstanceId != null) {
                spotInstance = this.client.getInstanceMetadata(assignedInstanceId);
            }
            pairs.add(new InstancePairedSpotRequest(spotRequest, spotInstance));
        }
        return pairs;
    }

    /**
     * Sets the pool membership tag ({@link ScalingTags#CLOUD_POOL_TAG}) on a
     * {@link SpotInstanceRequest}.
     *
     * @param spotInstanceRequest
     */
    private void setPoolMembershipTag(SpotInstanceRequest spotInstanceRequest) {
        this.client.tagResource(spotInstanceRequest.getSpotInstanceRequestId(), asList(poolMembershipTag()));
    }

    private Tag poolMembershipTag() {
        return new Tag().withKey(ScalingTags.CLOUD_POOL_TAG).withValue(getPoolName());
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
    private void verifyPoolMember(String spotRequestId) throws NotFoundException, AmazonClientException {
        Filter idFilter = new Filter(SPOT_REQUEST_ID_FILTER, asList(spotRequestId));
        Filter poolFilter = new Filter(CLOUD_POOL_TAG_FILTER, asList(getPoolName()));
        List<SpotInstanceRequest> matchingRequests = this.client
                .getSpotInstanceRequests(Arrays.asList(idFilter, poolFilter));
        if (matchingRequests.isEmpty()) {
            throw new NotFoundException(
                    String.format("spot instance request %s is not a member of the pool", spotRequestId));
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
        Filter idFilter = new Filter(SPOT_REQUEST_ID_FILTER, asList(spotRequestId));
        List<SpotInstanceRequest> matchingRequests = this.client.getSpotInstanceRequests(Arrays.asList(idFilter));
        if (matchingRequests.isEmpty()) {
            throw new NotFoundException(String.format("spot instance request %s does not exist", spotRequestId));
        }
        return matchingRequests.get(0);
    }

    /**
     * Cleans up any dangling {@link Instance}s (instances whose spot request
     * has been cancelled).
     *
     * @return All {@link Instance}s that were terminated.
     */
    List<Instance> cleanupDanglingInstances() {
        LOG.info("cleaning up dangling instances (whose spot requests " + "are cancelled) ...");
        // get all dead spot requests (canceled/closed/failed) spot requests
        // belonging to the pool
        Filter poolFilter = new Filter().withName(CLOUD_POOL_TAG_FILTER).withValues(getPoolName());
        // only include spot requests in state
        Filter spotStateFilter = new Filter().withName(SPOT_REQUEST_STATE_FILTER).withValues(Cancelled.toString(),
                Closed.toString());
        List<SpotInstanceRequest> deadRequests = client().getSpotInstanceRequests(asList(poolFilter, spotStateFilter));
        List<String> deadRequestIds = deadRequests.stream().map(SpotInstanceRequest::getSpotInstanceRequestId)
                .collect(Collectors.toList());

        // get all pending/running instances with a spot instance id equal
        // to any of the dead spot requests
        Filter stateFilter = new Filter().withName(INSTANCE_STATE_FILTER).withValues(Pending.toString(),
                Running.toString());
        Filter spotRequestFilter = new Filter().withName(ScalingFilters.SPOT_REQUEST_ID_FILTER)
                .withValues(deadRequestIds);

        List<Instance> danglingInstances = client().getInstances(asList(stateFilter, spotRequestFilter));
        for (Instance danglingInstance : danglingInstances) {
            LOG.info("terminating dangling instance {} belonging " + "to dead spot request {}",
                    danglingInstance.getInstanceId(), danglingInstance.getSpotInstanceRequestId());
            client().terminateInstances(asList(danglingInstance.getInstanceId()));
        }
        return danglingInstances;
    }

    /**
     * Check bid prices for all unfulfilled spot requests and cancel ones that
     * are not up-to-date with the currently configured bid price. These are to
     * eventually be replaced with a new spot request with the right bid price,
     * as soon as the {@link BaseCloudPool} detects that the pool is short on
     * spot requests.
     *
     * @return Returns the list of wrong-priced spot request identifiers that
     *         were cancelled.
     */
    List<String> cancelWrongPricedRequests() {
        double currentBidPrice = cloudApiSettings().getBidPrice();
        LOG.info("cancelling unfulfilled spot requests with bidprice " + "other than {} ...", currentBidPrice);
        List<InstancePairedSpotRequest> unfulfilledRequests = getPoolSpotRequests(asList(Open.toString()));
        List<String> wrongPricedSpotIds = new ArrayList<>();
        for (InstancePairedSpotRequest unfulfilledRequest : unfulfilledRequests) {
            SpotInstanceRequest request = unfulfilledRequest.getRequest();
            double spotPrice = Double.valueOf(request.getSpotPrice());
            if (spotPrice != currentBidPrice) {
                wrongPricedSpotIds.add(request.getSpotInstanceRequestId());
            }
        }
        if (wrongPricedSpotIds.isEmpty()) {
            return Collections.emptyList();
        }

        LOG.info("cancelling unfulfilled spot requests with wrong bid " + "price: {}", wrongPricedSpotIds);
        try {
            // Note: there is a possibility that a wrong-priced spot request has
            // been fulfilled after we decided to cancel it. If so, it will
            // become a dangling instance that gets cleaned up eventually.
            this.client.cancelSpotRequests(wrongPricedSpotIds);
        } catch (Exception e) {
            postCancellationFailureAlert(wrongPricedSpotIds, e);
        }

        postCancellationAlert(wrongPricedSpotIds);
        return wrongPricedSpotIds;
    }

    /**
     * Posts a spot request cancellation failure {@link Alert} on the
     * {@link EventBus}.
     *
     * @param spotRequestIds
     *            The spot requests that could not be cancelled.
     * @param error
     *            The error that occurred.
     */
    private void postCancellationFailureAlert(List<String> spotRequestIds, Exception error) {
        String message = String.format("failed to cancel wrong-priced spot requests %s: %s", spotRequestIds,
                error.getMessage());
        LOG.error("{}", message, error);
        this.eventBus.post(new Alert(AlertTopics.SPOT_REQUEST_CANCELLATION.name(), AlertSeverity.WARN, UtcTime.now(),
                message, null));
    }

    /**
     * Posts a spot request cancellation {@link Alert} on the {@link EventBus}.
     *
     * @param cancelledRequests
     *            The spot requests that were cancelled.
     */
    private void postCancellationAlert(List<String> cancelledRequests) {
        if (cancelledRequests.isEmpty()) {
            return;
        }

        String message = String.format(
                "cancelled %d unfulfilled spot instance request(s) " + "with an out-dated bid price",
                cancelledRequests.size());
        Map<String, JsonElement> metadata = ImmutableMap.of("cancelledRequests", JsonUtils.toJson(cancelledRequests));
        this.eventBus.post(new Alert(AlertTopics.SPOT_REQUEST_CANCELLATION.name(), AlertSeverity.INFO, UtcTime.now(),
                message, null, metadata));
    }

    /**
     * Returns the list of machine ids (from a given list of machine ids) that
     * are *not* members of the given pool.
     *
     * @param machineIds
     * @param pool
     * @return
     */
    private static List<String> nonPoolMembers(List<String> spotIds, List<InstancePairedSpotRequest> pool) {
        return spotIds.stream().filter(spotId -> !member(spotId, pool)).collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if the given spot request id is found in the
     * given pool of {@link InstancePairedSpotRequest}s.
     *
     * @param spotId
     * @param pool
     * @return
     */
    private static boolean member(String spotId, List<InstancePairedSpotRequest> pool) {
        return pool.stream().anyMatch(spotReq -> spotReq.getId().equals(spotId));
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
                LOG.error("failed to clean up dangling instances: {}\n{}", e.getMessage(),
                        Throwables.getStackTraceAsString(e));
            }
        }
    }

    /**
     * Periodical task that, when run, finds spot requests in the pool that have
     * been placed with a bid price different from the currently configured one.
     * Any such spot requests, that haven't yet been fulfilled, are cancelled
     * (to eventually be replaced with a new spot request with the right bid
     * price, as soon as the {@link BaseCloudPool} detects that the pool is
     * short on spot requests).
     */
    private class WrongPricedRequestCanceller implements Runnable {

        @Override
        public void run() {
            try {
                cancelWrongPricedRequests();
            } catch (Exception e) {
                // need to catch exceptions since periodic exeuction will stop
                // on uncaught exceptions
                LOG.error("failed to replace wrong bid price requests: {}\n{}", e.getMessage(),
                        Throwables.getStackTraceAsString(e));
            }
        }
    }
}
