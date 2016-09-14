package com.elastisys.scale.cloudpool.aws.spot.driver.alerts;

import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * A list of {@link Alert} message topics that can be emitted by a
 * {@link SpotPoolDriver}.
 */
public enum AlertTopics {
    /**
     * Topic for {@link Alert}s related to cancellation of spot instance
     * requests.
     */
    SPOT_REQUEST_CANCELLATION,
}
