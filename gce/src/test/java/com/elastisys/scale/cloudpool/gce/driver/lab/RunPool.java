package com.elastisys.scale.cloudpool.gce.driver.lab;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.cloudpool.gce.driver.GcePoolDriver;
import com.elastisys.scale.cloudpool.gce.driver.client.impl.StandardGceClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Lab program that exercises the Google Compute Engine {@link CloudPool} via
 * commands read from {@code stdin}.
 * <p/>
 * Note that an Instance Group with the specified name must already exist before
 * running the program.
 *
 */
public class RunPool {

    private static final Path configFile = Paths.get(".", "myconfig.json");

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        StateStorage stateStorage = StateStorage.builder(new File("target/state")).build();
        CloudPool pool = new BaseCloudPool(stateStorage, new GcePoolDriver(new StandardGceClient()));

        JsonObject config = JsonUtils.parseJsonFile(configFile.toFile()).getAsJsonObject();
        pool.configure(config);

        new CloudPoolCommandLineDriver(pool).start();

        executorService.shutdownNow();
    }
}