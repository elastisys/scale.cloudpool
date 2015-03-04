package com.elastisys.scale.cloudpool.commons.basepool.alerts;

import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;

/**
 * A list of {@link Alert} message topics that can be emitted by an
 * {@link BaseCloudPool}.
 *
 *
 *
 */
public enum AlertTopics {
	/** Topic for {@link Alert}s related to pool size changes. */
	RESIZE,
	/** Topic for {@link Alert}s related to fetching the machine pool. */
	POOL_FETCH,
	/** Topic for {@link Alert}s related to service state changes on machines. */
	SERVICE_STATE;
}
