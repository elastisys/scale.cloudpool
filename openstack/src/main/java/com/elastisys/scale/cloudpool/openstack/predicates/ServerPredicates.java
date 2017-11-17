package com.elastisys.scale.cloudpool.openstack.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;

/**
 * {@link Predicate}s relating to Openstack {@link Server} instances.
 */
public class ServerPredicates {

    /**
     * Creates a {@link Predicate} that checks {@link Server} instances for the
     * existence of a given meta data tag.
     * <p/>
     * The predicate will return <code>true</code> for all {@link Server}
     * instances with a given tag value in its user meta data.
     *
     * @param tag
     *            The meta data tag.
     * @param value
     *            The expected value of the meta data tag.
     * @return A predicate that returns <code>true</code> for {@link Server}
     *         instances with the given tag value and <code>false</code> for all
     *         other {@link Server} instances.
     */
    public static Predicate<Server> withTag(final String tag, final String tagValue) {
        checkNotNull(tag, "tag cannot be null");
        checkNotNull(tagValue, "tag value cannot be null");
        return server -> {
            Map<String, String> metadata = server.getMetadata();
            return metadata.containsKey(tag) && metadata.get(tag).equals(tagValue);
        };
    }

    /**
     * Creates a {@link Predicate} function that returns <code>true</code> for
     * any {@link Server} with a status in a set of allowed values.
     *
     * @param statuses
     *            The allowed server {@link Status}es.
     * @return A predicate that returns <code>true</code> for {@link Server}
     *         instances in a certain state.
     */
    public static Predicate<? super Server> withStateIn(final Status... statuses) {
        checkNotNull(statuses, "statuses cannot be null");
        return server -> {
            Set<Status> allowedStatuses = new HashSet<>(Arrays.asList(statuses));
            return allowedStatuses.contains(server.getStatus());
        };
    }
}
