package com.elastisys.scale.cloudpool.google.container.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Specifies which container cluster to manage.
 *
 * @see GoogleContainerEngineCloudPoolConfig
 */
public class ContainerCluster {

    /**
     * The name of the container cluster. For example, {@code my-cluster}.
     * Required.
     */
    private final String name;
    /**
     * The name of the Google Cloud project under which the container cluster
     * has been created. For example, {@code my-project}. Required.
     */
    private final String project;
    /**
     * The Google Cloud zone where the container cluster is located. For
     * example, {@code europe-west1-c}. Required.
     */
    private final String zone;

    /**
     * Creates a {@link ContainerCluster} config.
     *
     * @param name
     *            The name of the container cluster. For example,
     *            {@code my-cluster}. Required.
     * @param project
     *            The name of the Google Cloud project under which the container
     *            cluster has been created. For example, {@code my-project}.
     *            Required.
     * @param zone
     *            The Google Cloud zone where the container cluster is located.
     *            For example, {@code europe-west1-c}. Required.
     */
    public ContainerCluster(String name, String project, String zone) {
        this.name = name;
        this.project = project;
        this.zone = zone;
    }

    /**
     * The name of the container cluster. For example, {@code my-cluster}.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * The name of the Google Cloud project under which the container cluster
     * has been created. For example, {@code my-project}.
     *
     * @return
     */
    public String getProject() {
        return this.project;
    }

    /**
     * The Google Cloud zone where the container cluster is located. For
     * example, {@code europe-west1-c}.
     *
     * @return
     */
    public String getZone() {
        return this.zone;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.name != null, "cluster: missing name");
        checkArgument(this.project != null, "cluster: missing project");
        checkArgument(this.zone != null, "cluster: missing zone");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.project, this.zone);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContainerCluster) {
            ContainerCluster that = (ContainerCluster) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.project, that.project) //
                    && Objects.equals(this.zone, that.zone);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
