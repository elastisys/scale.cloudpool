package com.elastisys.scale.cloudpool.google.container.testutils;

import java.text.MessageFormat;

public class ZoneUrl {

    //

    private final String url;

    private ZoneUrl(String project, String zoneName) {
        this.url = MessageFormat.format("https://www.googleapis.com/compute/v1/projects/{0}/zones/{1}", //
                project, zoneName);
    }

    public static ZoneUrl from(String project, String zoneName) {
        return new ZoneUrl(project, zoneName);
    }

    public String getUrl() {
        return this.url;
    }
}
