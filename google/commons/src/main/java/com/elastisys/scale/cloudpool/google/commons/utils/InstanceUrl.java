package com.elastisys.scale.cloudpool.google.commons.utils;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Convenience class for parsing out the constituent parts of an instance URL
 * such as
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0}.
 *
 */
public class InstanceUrl {

    private static final String INSTANCE_URL_TEMPLATE = "https://www.googleapis.com/compute/v1/projects/{0}/zones/{1}/instances/{2}";
    private static final Pattern INSTANCE_URL_PATTERN = Pattern
            .compile("^https://www.googleapis.com/compute/v1/projects/([^/]+)/zones/([^/]+)/instances/([^/]+)$");

    /**
     * The full instance URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0
     */
    private final String instanceUrl;

    /** The project under which the instance exists. */
    private final String project;
    /** The zone in which the instance is located. */
    private final String zone;
    /** The short name of the instance. */
    private final String name;

    /**
     * Creates an {@link InstanceUrl}.
     *
     * @param instanceUrl
     *            A full instance URL, such as
     *            https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0.
     */
    public InstanceUrl(String instanceUrl) {
        Matcher matcher = INSTANCE_URL_PATTERN.matcher(instanceUrl);
        checkArgument(matcher.matches(), "illegal instance  URL '%s' does not match expected pattern: %s", instanceUrl,
                INSTANCE_URL_PATTERN);

        this.instanceUrl = instanceUrl;
        this.project = matcher.group(1);
        this.zone = matcher.group(2);
        this.name = matcher.group(3);
    }

    /**
     * Creates an {@link InstanceUrl} from a project, a zone and a name.
     *
     * @param project
     * @param zone
     * @param name
     * @return
     */
    public static InstanceUrl from(String project, String zone, String name) {
        checkArgument(project != null, "project cannot be null");
        checkArgument(zone != null, "zone cannot be null");
        checkArgument(name != null, "name cannot be null");
        return new InstanceUrl(MessageFormat.format(INSTANCE_URL_TEMPLATE, project, zone, name));
    }

    /**
     * The project under which the instance exists.
     *
     * @return
     */
    public String getProject() {
        return this.project;
    }

    /**
     * The zone in which the instance is located.
     *
     * @return
     */
    public String getZone() {
        return this.zone;
    }

    /**
     * The short name of the instance.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * The full instance URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-d/instances/webserver-s4s0
     *
     * @return
     */
    public String getUrl() {
        return this.instanceUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.instanceUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstanceUrl) {
            InstanceUrl that = (InstanceUrl) obj;
            return Objects.equals(this.instanceUrl, that.instanceUrl);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
