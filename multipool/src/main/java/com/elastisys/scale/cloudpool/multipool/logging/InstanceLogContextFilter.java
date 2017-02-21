package com.elastisys.scale.cloudpool.multipool.logging;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.elastisys.scale.cloudpool.api.CloudPool;

/**
 * A JAX-RS {@link ContainerRequestContext} filter which, for requests targeted
 * to a certain {@link CloudPool} instance, scopes log entries for the request
 * serving thread to the targeted instance.
 * <p/>
 * It does this by checking if a request is intended for a certain
 * {@link CloudPool} instance (that is, the request path is of form
 * {@code /cloudpools/<cloudPoolName>/...}. If the request is targeted for a
 * specific {@link CloudPool}, it sets a {@code cloudpool}
 * <a href="https://logback.qos.ch/manual/mdc.html">MDC property</a> which, for
 * example, can be used in a layout pattern via <code>%X{cloudpool}</code> to
 * show which {@link CloudPool} instance served a given request.
 */
public class InstanceLogContextFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceLogContextFilter.class);

    private static final Pattern CLOUDPOOL_TARGETED_PATH = Pattern.compile("^cloudpools/([^/]+)/.*");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestedPath = requestContext.getUriInfo().getPath();
        Optional<String> targetedCloudPool = getTargetedCloudPool(requestedPath);
        if (targetedCloudPool.isPresent()) {
            LOG.debug("setting log MDC property for thread: {}={}", LogConstants.POOL_INSTANCE_MDC_PROPERTY,
                    targetedCloudPool.get());
            MDC.put(LogConstants.POOL_INSTANCE_MDC_PROPERTY, targetedCloudPool.get());
        } else {
            MDC.remove(LogConstants.POOL_INSTANCE_MDC_PROPERTY);
        }
    }

    /**
     * Extracts the targeted {@link CloudPool} instance name from a request
     * path, if the request is destined for a certain {@link CloudPool}
     * instance.
     *
     * @param requestedPath
     *            A request path, such as {@code /cloudpools/my-pool/config}.
     * @return
     */
    private static Optional<String> getTargetedCloudPool(String requestedPath) {
        Matcher matcher = CLOUDPOOL_TARGETED_PATH.matcher(requestedPath);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
