package com.elastisys.scale.cloudadapters.splitter.server;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapters.splitter.Splitter;

/**
 * Run a REST endpoint server for the {@link Splitter} cloud adapter.
 *
 * @see CloudAdapterServer
 */
public class Main {

	/**
	 * Starts a {@link SplitterCloudAdapter} accessible over REST.
	 *
	 * @see CloudAdapterServer#main(CloudAdapter, String[])
	 */
	public static void main(String[] args) throws Exception {
		CloudAdapter scalingGroup = new Splitter();
		CloudAdapterServer.main(scalingGroup, args);
	}
}
