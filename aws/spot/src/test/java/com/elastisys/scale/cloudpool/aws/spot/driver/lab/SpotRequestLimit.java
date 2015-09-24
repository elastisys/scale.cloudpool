package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

import java.io.File;
import java.util.List;

import jersey.repackaged.com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A client that places spot requests in a given zone until no more requests are
 * accepted in order to determine the spot request limit set by Amazon. On the
 * last attempt, the limit was set at 50 spot requests. On the next request the
 * following error was encountered:
 *
 * <pre>
 * com.amazonaws.AmazonServiceException: Max spot instance count exceeded (Service: AmazonEC2; Status Code: 400; Error Code: MaxSpotInstanceCountExceeded; Request ID: 7f0dd7fa-7f94-4a4e-8be7-2ff82c7134bd)
 * </pre>
 */
public class SpotRequestLimit extends BaseClient {

	private static final Logger LOG = LoggerFactory
			.getLogger(SpotRequestLimit.class);

	public static void main(String[] args) {
		AwsSpotClient client = new AwsSpotClient();
		client.configure(awsAccessKeyId, awsSecretAccessKey, region);

		LOG.info("Testing spot request limit in region {}", region);
		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonFile(new File("myconfig.json")),
				BaseCloudPoolConfig.class);
		LOG.info("config: {}", config.getScaleOutConfig());
		List<SpotInstanceRequest> placedRequests = Lists.newArrayList();
		int completedRequests = 0;
		int MAX = 200;
		try {
			while (completedRequests < MAX) {
				placedRequests.add(client.placeSpotRequest(0.001,
						config.getScaleOutConfig()));
				completedRequests++;
				LOG.info("completed request {}", completedRequests);
			}
		} catch (Exception e) {
			LOG.error("failed after {} request(s): {}", completedRequests,
					e.getMessage(), e);
		} finally {
			LOG.info("cancelling all placed requests ...");
			for (SpotInstanceRequest request : placedRequests) {
				client.cancelSpotRequest(request.getSpotInstanceRequestId());
			}
			LOG.info("cancelled all requests.");
		}
	}
}
