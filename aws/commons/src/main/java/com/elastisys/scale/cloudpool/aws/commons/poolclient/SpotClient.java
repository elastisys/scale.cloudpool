package com.elastisys.scale.cloudpool.aws.commons.poolclient;

import java.util.Collection;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;

/**
 * An AWS client interface that extends the {@link Ec2Client} with methods for
 * dealing with spot requests.
 */
public interface SpotClient extends Ec2Client {

    /**
     * Retrieves meta data about a particular {@link SpotInstanceRequest}.
     *
     * @param spotRequestId
     *            The id of the spot request.
     * @return
     * @throws AmazonClientException
     */
    SpotInstanceRequest getSpotInstanceRequest(String spotRequestId) throws AmazonClientException;

    /**
     * Returns all {@link SpotInstanceRequest}s that satisfy a number of
     * {@link Filter}s.
     *
     * @param filters
     *            The {@link Filter}s that need to be satisfied by returned
     *            {@link SpotInstanceRequest}s.
     * @return
     * @throws AmazonClientException
     */
    List<SpotInstanceRequest> getSpotInstanceRequests(Collection<Filter> filters) throws AmazonClientException;

    /**
     * Places a number of new {@link SpotInstanceRequest}s and (optionally) tags
     * the spot instance requests with a given set of {@link Tag}s.
     *
     * @param bidPrice
     *            The bid price to set for the {@link SpotInstanceRequest}.
     * @param instanceTemplate
     *            A description of the desired spot {@link Instance}.
     * @param count
     *            The number of spot instances to request.
     * @param tags
     *            Tags to set on the created spot instance requests. May be
     *            empty.
     * @return The placed {@link SpotInstanceRequest}s.
     * @throws AmazonClientException
     */
    public List<SpotInstanceRequest> placeSpotRequests(double bidPrice, Ec2ProvisioningTemplate instanceTemplate,
            int count, List<Tag> tags) throws AmazonClientException;

    /**
     * Cancels a collection of {@link SpotInstanceRequest}s.
     *
     * @param spotInstanceRequestIds
     *            The identifiers of all {@link SpotInstanceRequest}s to cancel.
     * @return The result of the cancellation request.
     * @throws AmazonClientException
     */
    CancelSpotInstanceRequestsResult cancelSpotRequests(List<String> spotInstanceRequestIds)
            throws AmazonClientException;

}
