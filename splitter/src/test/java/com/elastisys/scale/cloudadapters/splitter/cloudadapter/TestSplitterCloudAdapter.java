package com.elastisys.scale.cloudadapters.splitter.cloudadapter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.config.SplitterCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.CloudAdapterServlet;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.MockPrioritizedRemoteCloudAdapter;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

public class TestSplitterCloudAdapter {

	private final SplitterCloudAdapter splitterAdapter = new SplitterCloudAdapter();

	private final List<Server> servers = new LinkedList<Server>();

	@After
	public void tearDown() throws Exception {
		for (Server server : this.servers) {
			server.stop();
			server.join();
		}
		this.servers.clear();
	}

	@Test(expected = NullPointerException.class)
	public void testUseBeforeConfiguration() {
		new SplitterCloudAdapter().getMachinePool();
	}

	@Test
	public void testGetConfiguration() {
		final SplitterCloudAdapterConfig expectedConfig = getConfigFromResource("splitteradapter/correct-config.json");
		this.splitterAdapter.configure((JsonObject) JsonUtils
				.toJson(expectedConfig));

		assertEquals(expectedConfig, JsonUtils.toObject(this.splitterAdapter
				.getConfiguration().get(), SplitterCloudAdapterConfig.class));
	}

	@Test(expected = CloudAdapterException.class)
	public void testConfigureWithIncorrectConfig() {
		SplitterCloudAdapterConfig incorrectConfig = getConfigFromResource("splitteradapter/no-adapters.json");
		this.splitterAdapter.configure((JsonObject) JsonUtils
				.toJson(incorrectConfig));
	}

	@Test
	public void testCorrectGetMachinePools() throws Exception {
		getMachinePoolsFromConfiguredBackend("splitteradapter/correct-config.json");
	}

	@Test
	public void testMachinePoolUtcTimezone() throws Exception {
		getMachinePoolsFromConfiguredBackend("splitteradapter/correct-config.json");
		MachinePool pool = this.splitterAdapter.getMachinePool();
		assertThat(pool.getTimestamp().getZone(), equalTo(DateTimeZone.UTC));
	}

	@Test(expected = CloudAdapterException.class)
	public void testUnreachableBackendGetMachinePools() throws Exception {
		getMachinePoolsFromConfiguredBackend("splitteradapter/unreachable-host-config.json");
	}

	@Test
	public void testResizePools() throws Exception {
		SplitterCloudAdapterConfig config = getConfigFromResource("splitteradapter/correct-config.json");

		assertEquals(40, config.getAdapterConfigurations().get(0).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(1).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(2).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(3).getPriority());

		List<CloudAdapterServlet> servlets = createAndStartServlets(config);
		this.splitterAdapter.configure((JsonObject) JsonUtils.toJson(config));

		final int individualMachinePoolSize = 5;

		for (CloudAdapterServlet servlet : servlets) {
			MockPrioritizedRemoteCloudAdapter mockedAdapter = (MockPrioritizedRemoteCloudAdapter) servlet
					.getCloudAdapter();
			mockedAdapter.setMachinePool(MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(individualMachinePoolSize));
		}

		/*
		 * Alright, now actually resize according to the priorities!
		 */

		this.splitterAdapter.resizeMachinePool(0);
		assertEquals(0, servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(1).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(2).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(3).getCloudAdapter().getMachinePool()
				.getMachines().size());

		this.splitterAdapter.resizeMachinePool(1);
		assertEquals(1, servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(1).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(2).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(3).getCloudAdapter().getMachinePool()
				.getMachines().size());

		this.splitterAdapter.resizeMachinePool(2);
		assertEquals(1, servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(1, servlets.get(1).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(2).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(0, servlets.get(3).getCloudAdapter().getMachinePool()
				.getMachines().size());

		this.splitterAdapter.resizeMachinePool(10);
		assertEquals(4, servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(2, servlets.get(1).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(2, servlets.get(2).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(2, servlets.get(3).getCloudAdapter().getMachinePool()
				.getMachines().size());

		this.splitterAdapter.resizeMachinePool(13);
		assertEquals(6, servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(3, servlets.get(1).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(3, servlets.get(2).getCloudAdapter().getMachinePool()
				.getMachines().size());
		assertEquals(1, servlets.get(3).getCloudAdapter().getMachinePool()
				.getMachines().size());
	}

	@Test(expected = CloudAdapterException.class)
	public void testResizeFailingHost() throws Exception {
		SplitterCloudAdapterConfig config = getConfigFromResource("splitteradapter/correct-config.json");

		assertEquals(40, config.getAdapterConfigurations().get(0).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(1).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(2).getPriority());
		assertEquals(20, config.getAdapterConfigurations().get(3).getPriority());

		List<CloudAdapterServlet> servlets = createAndStartServlets(config);
		this.splitterAdapter.configure((JsonObject) JsonUtils.toJson(config));

		final int individualMachinePoolSize = 5;

		for (CloudAdapterServlet servlet : servlets) {
			MockPrioritizedRemoteCloudAdapter mockedAdapter = (MockPrioritizedRemoteCloudAdapter) servlet
					.getCloudAdapter();
			mockedAdapter.setMachinePool(MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(individualMachinePoolSize));
		}

		/*
		 * The last adapter should fail...
		 */
		servlets.get(3).setNextPostResponse(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		this.splitterAdapter.resizeMachinePool(0);
	}

	private void getMachinePoolsFromConfiguredBackend(
			String configurationResource) throws Exception {
		SplitterCloudAdapterConfig config = getConfigFromResource(configurationResource);
		List<CloudAdapterServlet> servlets = createAndStartServlets(config);

		this.splitterAdapter.configure((JsonObject) JsonUtils.toJson(config));

		final int individualMachinePoolSize = 5;

		for (CloudAdapterServlet servlet : servlets) {
			MockPrioritizedRemoteCloudAdapter mockedAdapter = (MockPrioritizedRemoteCloudAdapter) servlet
					.getCloudAdapter();
			mockedAdapter.setMachinePool(MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(individualMachinePoolSize));
		}

		assertEquals(servlets.get(0).getCloudAdapter().getMachinePool()
				.getMachines().size(), individualMachinePoolSize);

		assertEquals(individualMachinePoolSize * servlets.size(),
				this.splitterAdapter.getMachinePool().getMachines().size());
	}

	@Test
	public void testGetConfigurationSchema() {
		assertEquals(
				JsonUtils
						.parseJsonResource("splitter-adapter-config-schema.json"),
				this.splitterAdapter.getConfigurationSchema().get());
	}

	/**
	 * Creates a number of servlets and servers that contain them, and starts
	 * the servers. The servers are added to the field {@link #servers}, and are
	 * therefore stopped and removed in the {@link #tearDown()} method.
	 *
	 * @param portNumbers
	 *            The port numbers that the servers should have, each running a
	 *            single servlet.
	 * @return A list of servlets.
	 * @throws Exception
	 *             Thrown if the server fails to start.
	 */
	private List<CloudAdapterServlet> createAndStartServlets(
			List<PrioritizedRemoteCloudAdapterConfig> configs) throws Exception {
		List<CloudAdapterServlet> servlets = new LinkedList<CloudAdapterServlet>();
		for (PrioritizedRemoteCloudAdapterConfig config : configs) {
			CloudAdapterServlet servlet = new CloudAdapterServlet(
					new MockPrioritizedRemoteCloudAdapter());
			servlet.getCloudAdapter().configure(config);
			servlets.add(servlet);
			final Server server = CloudAdapterServlet.getServer(servlet,
					config.getCloudAdapterPort());
			this.servers.add(server);
			server.start();
		}
		return servlets;
	}

	private List<CloudAdapterServlet> createAndStartServlets(
			SplitterCloudAdapterConfig splitterCloudAdapterConfig)
					throws Exception {
		return createAndStartServlets(splitterCloudAdapterConfig
				.getAdapterConfigurations());
	}

	private SplitterCloudAdapterConfig getConfigFromResource(String resource) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resource),
				SplitterCloudAdapterConfig.class);
	}
}
