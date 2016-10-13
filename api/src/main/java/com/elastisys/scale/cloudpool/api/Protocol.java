package com.elastisys.scale.cloudpool.api;

import com.google.gson.annotations.SerializedName;

/**
 * Supported cloud pool protocols.
 */
public enum Protocol {
    /** HTTP protocol. */
    @SerializedName("http")
    HTTP("http"),
    /** HTTPS protocol. */
    @SerializedName("https")
    HTTPS("https");

    /** The protocol name to use in URIs. */
    private final String protocolName;

    Protocol(String protocolName) {
        this.protocolName = protocolName;
    }

    /**
     * The protocol name to use in URIs. For example, "http".
     *
     * @return
     */
    public String getProtocolName() {
        return this.protocolName;
    }
}
