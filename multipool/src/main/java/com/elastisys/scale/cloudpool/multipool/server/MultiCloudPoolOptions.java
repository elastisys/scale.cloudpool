package com.elastisys.scale.cloudpool.multipool.server;

import java.nio.charset.StandardCharsets;

import org.kohsuke.args4j.Option;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.cli.server.BaseServerCliOptions;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Captures (command-line) options accepted by a {@link MultiCloudPoolServer}.
 */
public class MultiCloudPoolOptions extends BaseServerCliOptions {

    /**
     * The default directory where the {@link CloudPool} instances will store
     * their runtime state.
     */
    public static final String DEFAULT_STORAGE_DIR = "/var/lib/elastisys/multicloudpool";

    @Option(name = "--storage-dir", metaVar = "DIR", usage = "Directory under "
            + "which state will be stored for cloud pool instances. Needs to be writable.")
    public String storageDir = DEFAULT_STORAGE_DIR;

    @Override
    public String getVersion() {
        return IoUtils.toString("VERSION.txt", StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
    }
}
