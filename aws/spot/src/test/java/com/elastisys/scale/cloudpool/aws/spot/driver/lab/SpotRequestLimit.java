package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;

import jersey.repackaged.com.google.common.collect.Lists;

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
		client.configure(awsAccessKeyId, awsSecretAccessKey, region,
				new ClientConfiguration());

		LOG.info("Testing spot request limit in region {}", region);
		BaseCloudPoolConfig config = JsonUtils.toObject(
				JsonUtils.parseJsonFile(new File("myconfig.json")),
				BaseCloudPoolConfig.class);
		LOG.info("config: {}", config.getScaleOutConfig());
		List<SpotInstanceRequest> placedRequests = Lists.newArrayList();
		int MAX = 200;
		try {
			placedRequests.addAll(
					client.placeSpotRequests(0.001, config.getScaleOutConfig(),
							MAX, Arrays.asList(new Tag("key", "value"))));
		} catch (Exception e) {
			LOG.error("failed: {}", e.getMessage(), e);
		} finally {
			LOG.info("cancelling all placed requests ...");
			for (SpotInstanceRequest request : placedRequests) {
				client.cancelSpotRequests(
						Arrays.asList(request.getSpotInstanceRequestId()));
			}
			LOG.info("cancelled all requests.");
		}
	}
}
