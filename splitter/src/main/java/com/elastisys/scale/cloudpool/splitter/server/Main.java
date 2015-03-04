package com.elastisys.scale.cloudpool.splitter.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.splitter.Splitter;

/**
 * Run a REST endpoint server for the {@link Splitter} cloud pool.
 *
 * @see CloudPoolServer
 */
public class Main {

	/**
	 * Starts a {@link Splitter} accessible over REST.
	 *
	 * @see CloudPoolServer#main(CloudPool, String[])
	 */
	public static void main(String[] args) throws Exception {
		CloudPool cloudPool = new Splitter();
		CloudPoolServer.main(cloudPool, args);
	}
}
