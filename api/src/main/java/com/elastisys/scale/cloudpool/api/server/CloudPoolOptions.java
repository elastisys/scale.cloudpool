package com.elastisys.scale.cloudpool.api.server;

import java.nio.charset.StandardCharsets;

import org.kohsuke.args4j.Option;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.cli.server.BaseServerCliOptions;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Captures (command-line) options accepted by a {@link CloudPoolServer}.
 */
public class CloudPoolOptions extends BaseServerCliOptions {

    /**
     * The default directory where the {@link CloudPool} will store its runtime
     * state.
     */
    public final static String DEFAULT_STORAGE_DIR = "/var/lib/elastisys/cloudpool";

    @Option(name = "--config", metaVar = "FILE", usage = "Initial "
            + "(JSON-formatted) configuration to set for cloud pool. "
            + "If no configuration is set, an attempt will be made to "
            + "load the last set configuration from the storage "
            + "directory (see --storage-dir). If none has been set, "
            + "the cloud pool is started without configuration.")
    public String config = null;

    @Option(name = "--storage-dir", metaVar = "DIR", usage = "Directory where "
            + "the cloud pool will store its runtime state. Needs to be "
            + "writable by the user running the cloud pool.")
    public String storageDir = DEFAULT_STORAGE_DIR;

    @Option(name = "--stopped", usage = "Puts the cloud pool in a stopped "
            + "state. By default, the cloud pool is started if an explicit "
            + "configuration is provided (--config) or if the cloud pool can "
            + "recover an old configration from the storage directory.")
    public boolean stopped = false; // default

    @Override
    public String getVersion() {
        return IoUtils.toString("VERSION.txt", StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
    }
}
