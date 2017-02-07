package com.elastisys.scale.cloudpool.google.container.testutils;

import java.text.MessageFormat;

public class NodePoolUrl {

    private final String url;

    private NodePoolUrl(String project, String zone, String clusterName, String nodePoolName) {
        this.url = MessageFormat.format(
                "https://container.googleapis.com/v1/projects/{0}/zones/{1}/clusters/{2}/nodePools/{3}", //
                project, zone, clusterName, nodePoolName);
    }

    public static NodePoolUrl from(String project, String zone, String clusterName, String nodePoolName) {
        return new NodePoolUrl(project, zone, clusterName, nodePoolName);
    }

    public String getUrl() {
        return this.url;
    }
}
