package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands.GetMachinePoolCommand;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands.PoolResizeCommand;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.CloudAdapterServlet;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.MockPrioritizedRemoteCloudAdapter;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.google.common.collect.ImmutableList;

public class TestStandardPrioritizedRemoteCloudAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(TestStandardPrioritizedRemoteCloudAdapter.class);

	private final StandardPrioritizedRemoteCloudAdapter cloudAdapter = new StandardPrioritizedRemoteCloudAdapter();

	private final int serverPort = 9876;
	private final String serverHostname = "localhost";
	private final BasicCredentials realmCredentials = new BasicCredentials(
			"admin", "adminpassword");
	private final CertificateCredentials correctCertificateCredentials = new CertificateCredentials(
			KeyStoreType.PKCS12, CloudAdapterServlet.KEYSTORE_PATH,
			CloudAdapterServlet.KEYSTORE_PASSWORD);

	private final PrioritizedRemoteCloudAdapterConfig correctConfig = createCorrectConfig();
	private final MockPrioritizedRemoteCloudAdapter mockedCloudAdapter = new MockPrioritizedRemoteCloudAdapter();
	private final CloudAdapterServlet cloudAdapterServlet = new CloudAdapterServlet(
			this.mockedCloudAdapter);
	private Server server;

	@Before
	public void setUp() throws Exception {
		this.server = CloudAdapterServlet.getServer(this.cloudAdapterServlet,
				this.serverPort);
		this.server.start();

		this.cloudAdapterServlet.setNextGetResponse(HttpServletResponse.SC_OK);
		this.cloudAdapterServlet.setNextPostResponse(HttpServletResponse.SC_OK);
		this.cloudAdapterServlet.setReturnBrokenMachinePool(false);
	}

	@After
	public void tearDown() throws Exception {
		if (this.server != null) {
			this.server.stop();
			this.server.join();
		}
	}

	@Test
	public void testGetPool() throws ConfigurationException {
		for (int i = 0; i < 10; i++) {
			final MachinePool expected = MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(i);

			this.mockedCloudAdapter.setMachinePool(expected);
			this.cloudAdapter.configure(this.correctConfig);

			MachinePool actual = this.cloudAdapter.getMachinePool();

			assertEquals(expected, actual);
		}
	}

	@Test(expected = CloudAdapterException.class)
	public void handleGetPoolInternalServerError()
			throws ConfigurationException {
		this.cloudAdapterServlet
		.setNextGetResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		this.cloudAdapter.configure(this.correctConfig);
		this.cloudAdapter.getMachinePool();
	}

	@Test(expected = CloudAdapterException.class)
	public void handleGetPoolBadRequest() throws ConfigurationException {
		this.cloudAdapterServlet
		.setNextGetResponse(HttpServletResponse.SC_BAD_REQUEST);
		this.cloudAdapter.configure(this.correctConfig);
		this.cloudAdapter.getMachinePool();
	}

	@Test
	public void testUpdateMachinePool() throws ConfigurationException {
		// need a pool to support GET:ing it
		this.mockedCloudAdapter
		.setMachinePool(MockPrioritizedRemoteCloudAdapter
				.generateMachinePool(1));

		this.cloudAdapter.configure(this.correctConfig);

		final int correctSize = 10;

		this.cloudAdapter.resizeMachinePool(correctSize);

		assertEquals(correctSize, this.cloudAdapterServlet.getRequestedSize());
		assertEquals(correctSize, this.mockedCloudAdapter.getMachinePool()
				.getMachines().size());
	}

	@Test(expected = CloudAdapterException.class)
	public void testUpdateMachinePoolInternalServerError()
			throws ConfigurationException {
		this.mockedCloudAdapter
		.setMachinePool(MockPrioritizedRemoteCloudAdapter
				.generateMachinePool(1));

		this.cloudAdapterServlet
		.setNextPostResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		this.cloudAdapter.configure(this.correctConfig);
		this.cloudAdapter.resizeMachinePool(0);
	}

	@Test(expected = CloudAdapterException.class)
	public void testUpdateMachinePoolBadRequest() throws ConfigurationException {
		this.mockedCloudAdapter
		.setMachinePool(MockPrioritizedRemoteCloudAdapter
				.generateMachinePool(1));

		this.cloudAdapterServlet
		.setNextPostResponse(HttpServletResponse.SC_BAD_REQUEST);
		this.cloudAdapter.configure(this.correctConfig);
		this.cloudAdapter.resizeMachinePool(0);
	}

	@Test(expected = IllegalStateException.class)
	public void testResizeBeforeConfiguration() {
		StandardPrioritizedRemoteCloudAdapter adapter = new StandardPrioritizedRemoteCloudAdapter();
		adapter.resizeMachinePool(0);
	}

	@Test(expected = IllegalStateException.class)
	public void testGetPoolBeforeConfiguration() {
		StandardPrioritizedRemoteCloudAdapter adapter = new StandardPrioritizedRemoteCloudAdapter();
		adapter.getMachinePool();
	}

	@Test(expected = IllegalStateException.class)
	public void testGetPriorityBeforeConfiguration() {
		StandardPrioritizedRemoteCloudAdapter adapter = new StandardPrioritizedRemoteCloudAdapter();
		adapter.getPriority();
	}

	@Test
	public void testConfigurationParsing() throws ConfigurationException,
	RuntimeException {
		StandardPrioritizedRemoteCloudAdapter adapter = new StandardPrioritizedRemoteCloudAdapter();

		PrioritizedRemoteCloudAdapterConfig config = JsonUtils
				.toObject(JsonUtils
						.parseJsonResource("cloudadapter/correct-config.json"),
						PrioritizedRemoteCloudAdapterConfig.class);

		assertEquals("localhost0", config.getCloudAdapterHost());
		assertEquals(40, config.getPriority());
		assertEquals(10, config.getCloudAdapterPort());
		assertEquals("testuser0", config.getBasicCredentials().get()
				.getUsername());
		assertEquals("testpassword0", config.getBasicCredentials().get()
				.getPassword());

		adapter.validate(config);
		adapter.configure(config);
	}

	@Test(expected = ConfigurationException.class)
	public void testIncorrectConfigurationParsing()
			throws ConfigurationException {
		StandardPrioritizedRemoteCloudAdapter adapter = new StandardPrioritizedRemoteCloudAdapter();

		PrioritizedRemoteCloudAdapterConfig config = JsonUtils
				.toObject(
						JsonUtils
						.parseJsonResource("cloudadapter/missing-username.json"),
						PrioritizedRemoteCloudAdapterConfig.class);
		adapter.configure(config);
	}

	@Test
	public void testSortOrder() throws ConfigurationException {
		StandardPrioritizedRemoteCloudAdapter a = new StandardPrioritizedRemoteCloudAdapter();
		StandardPrioritizedRemoteCloudAdapter b = new StandardPrioritizedRemoteCloudAdapter();
		StandardPrioritizedRemoteCloudAdapter c = new StandardPrioritizedRemoteCloudAdapter();

		a.configure(createCorrectConfig(20));
		b.configure(createCorrectConfig(40));
		c.configure(createCorrectConfig(40));

		ImmutableList<StandardPrioritizedRemoteCloudAdapter> unsortedAdapters = ImmutableList
				.of(a, b, c);

		/*
		 * As specified in the interface, the sort must be descending with
		 * regard to priority.
		 */
		ImmutableList<StandardPrioritizedRemoteCloudAdapter> correctlySortedAdapters = ImmutableList
				.of(b, c, a);

		List<StandardPrioritizedRemoteCloudAdapter> sortedAdapters = new LinkedList<StandardPrioritizedRemoteCloudAdapter>(
				unsortedAdapters);
		Collections.sort(sortedAdapters);

		assertEquals(correctlySortedAdapters, sortedAdapters);
	}

	@Test(expected = CloudAdapterException.class)
	public void testUnparseableMachinePool() throws ConfigurationException {
		this.cloudAdapterServlet.setReturnBrokenMachinePool(true);
		testGetPool();
	}

	@Test(expected = ConfigurationException.class)
	public void testNullConfig() throws ConfigurationException {
		this.cloudAdapter.validate(null);
	}

	@Test(expected = CloudAdapterException.class)
	public void testUnavailableHost() throws ConfigurationException {
		PrioritizedRemoteCloudAdapterConfig incorrectConfig = new PrioritizedRemoteCloudAdapterConfig(
				"localhost", this.serverPort - 1, 100, this.realmCredentials,
				null);

		this.cloudAdapter.configure(incorrectConfig);

		this.cloudAdapter.getMachinePool();
	}

	@Test(expected = CloudAdapterException.class)
	public void testIncorrectCertificateConfiguration()
			throws ConfigurationException {
		final CertificateCredentials incorrectCertificateCredentials = new CertificateCredentials(
				KeyStoreType.PKCS12, "/proc/cpuinfo", "somepassword");

		this.cloudAdapter.configure(new PrioritizedRemoteCloudAdapterConfig(
				this.serverHostname, this.serverPort, 100, null,
				incorrectCertificateCredentials));

		this.cloudAdapter.getMachinePool();
	}

	@Test(expected = CloudAdapterException.class)
	public void testCorrectCertificateCredentialsButIncorrectConnection()
			throws ConfigurationException {
		this.cloudAdapter.configure(new PrioritizedRemoteCloudAdapterConfig(
				this.serverHostname, this.serverPort, 100, null,
				this.correctCertificateCredentials));

		this.cloudAdapter.getMachinePool();
	}

	@Test
	public void testGetMachinePoolViaCommand() throws Exception {
		GetMachinePoolCommand command = new GetMachinePoolCommand(
				this.cloudAdapter);

		final MachinePool expected = MockPrioritizedRemoteCloudAdapter
				.generateMachinePool(10);

		this.mockedCloudAdapter.setMachinePool(expected);
		this.cloudAdapter.configure(this.correctConfig);

		MachinePool actual = command.call();
		assertEquals(expected, actual);
	}

	@Test(expected = Exception.class)
	public void testGetMachinePoolErrorsViaCommand() throws Exception {
		this.cloudAdapterServlet
		.setNextGetResponse(HttpServletResponse.SC_BAD_REQUEST);
		testGetMachinePoolViaCommand();
	}

	@Test
	public void testUpdateMachinePoolViaCommand() throws Exception {
		final int correctSize = 5;
		PoolResizeCommand command = new PoolResizeCommand(this.cloudAdapter,
				correctSize);

		final MachinePool machinePool = MockPrioritizedRemoteCloudAdapter
				.generateMachinePool(10);
		this.mockedCloudAdapter.setMachinePool(machinePool);
		this.cloudAdapter.configure(this.correctConfig);

		command.call();

		assertEquals(correctSize, this.cloudAdapterServlet.getRequestedSize());
		assertEquals(correctSize, this.cloudAdapter.getMachinePool()
				.getMachines().size());
	}

	@Test(expected = Exception.class)
	public void testUpdateMachinePoolErrorsViaCommand() throws Exception {
		this.cloudAdapterServlet
		.setNextPostResponse(HttpServletResponse.SC_BAD_REQUEST);
		testUpdateMachinePoolViaCommand();
	}

	private PrioritizedRemoteCloudAdapterConfig createCorrectConfig() {
		return createCorrectConfig(100);
	}

	private PrioritizedRemoteCloudAdapterConfig createCorrectConfig(int priority) {
		return new PrioritizedRemoteCloudAdapterConfig(this.serverHostname,
				this.serverPort, priority, this.realmCredentials, null);
	}

}
