package com.elastisys.scale.cloudpool.citycloud.driver.lab;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.StandardOpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the CityCloud {@link CloudPool} via
 * commands read from {@code stdin}.
 */
public class RunPool {
    static Logger LOG = LoggerFactory.getLogger(RunPool.class);

    /**
     * TODO: set up a cloud pool configuration file for the
     * {@link OpenStackPoolDriver}. Relative paths are relative to base
     * directory of enclosing Maven project.
     */
    private static final Path cloudPoolConfig = Paths.get(".", "myconfig.json");

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    public static void main(String[] args) throws Exception {
        StateStorage stateStorage = StateStorage.builder(new File("target/state")).build();
        CloudPoolDriver cityCloudDriver = new OpenStackPoolDriver(new StandardOpenstackClient(),
                CloudProviders.CITYCLOUD);
        CloudPool pool = new BaseCloudPool(stateStorage, cityCloudDriver);

        JsonObject config = JsonUtils.parseJsonFile(cloudPoolConfig.toFile()).getAsJsonObject();
        pool.configure(config);

        new CloudPoolCommandLineDriver(pool).start();

        executorService.shutdownNow();
    }
}
