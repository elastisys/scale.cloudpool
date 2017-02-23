package com.elastisys.scale.cloudpool.kubernetes.lab;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.commons.util.cli.CloudPoolCommandLineDriver;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.StandardKubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.http.impl.AuthenticatingHttpApiClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Simple lab program that exercises the {@link CloudPool} via commands read
 * from {@code stdin}.
 */
public class RunPool {
    static Logger LOG = LoggerFactory.getLogger(RunPool.class);

    private static final Path configFile = Paths.get(System.getenv("HOME"), ".elastisys", "kubernetes", "config.json");

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    public static void main(String[] args) throws Exception {
        CloudPool pool = new KubernetesCloudPool(new StandardKubernetesClient(new AuthenticatingHttpApiClient()),
                executor);

        JsonObject config = JsonUtils.parseJsonFile(configFile.toFile()).getAsJsonObject();
        pool.configure(config);

        new CloudPoolCommandLineDriver(pool).start();

        executor.shutdownNow();
    }
}
