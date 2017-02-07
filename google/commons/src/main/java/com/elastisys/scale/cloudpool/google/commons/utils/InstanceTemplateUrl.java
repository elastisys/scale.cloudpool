package com.elastisys.scale.cloudpool.google.commons.utils;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Convenience class for parsing out the constituent parts of an instance
 * template URL such as
 * {@code https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/my-template}.
 *
 */
public class InstanceTemplateUrl {

    private static final String INSTANCE_TEMPLATE_URL_TEMPLATE = "https://www.googleapis.com/compute/v1/projects/{0}/global/instanceTemplates/{1}";
    private static final Pattern INSTANCE_TEMPLATE_URL_PATTERN = Pattern
            .compile("^https://www.googleapis.com/compute/v1/projects/([^/]+)/global/instanceTemplates/([^/]+)$");

    /**
     * The full instance template URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/my-template
     */
    private final String instanceTemplateUrl;

    /** The project under which the instance template exists. */
    private final String project;
    /** The short name of the instance template. */
    private final String name;

    /**
     * Creates an {@link InstanceTemplateUrl}.
     *
     * @param instanceTemplateUrl
     *            A full instance template URL, such as
     *            https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/my-template
     */
    public InstanceTemplateUrl(String instanceTemplateUrl) {
        Matcher matcher = INSTANCE_TEMPLATE_URL_PATTERN.matcher(instanceTemplateUrl);
        checkArgument(matcher.matches(), "illegal instance template URL '%s' does not match expected pattern: %s",
                instanceTemplateUrl, INSTANCE_TEMPLATE_URL_PATTERN);

        this.instanceTemplateUrl = instanceTemplateUrl;
        this.project = matcher.group(1);
        this.name = matcher.group(2);
    }

    /**
     * Creates an {@link InstanceTemplateUrl} from a project, and a name.
     *
     * @param project
     *            A project, such as {@code my-project}.
     * @param name
     *            A name, such as {@code my-template}.
     * @return
     */
    public static InstanceTemplateUrl from(String project, String name) {
        checkArgument(project != null, "project cannot be null");
        checkArgument(name != null, "name cannot be null");
        return new InstanceTemplateUrl(MessageFormat.format(INSTANCE_TEMPLATE_URL_TEMPLATE, project, name));
    }

    /**
     * The project under which the instance template exists.
     *
     * @return
     */
    public String getProject() {
        return this.project;
    }

    /**
     * The short name of the instance template.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * The full instance template URL. For example,
     * https://www.googleapis.com/compute/v1/projects/my-project/global/instanceTemplates/my-template.
     *
     * @return
     */
    public String getUrl() {
        return this.instanceTemplateUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.instanceTemplateUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstanceTemplateUrl) {
            InstanceTemplateUrl that = (InstanceTemplateUrl) obj;
            return Objects.equals(this.instanceTemplateUrl, that.instanceTemplateUrl);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
