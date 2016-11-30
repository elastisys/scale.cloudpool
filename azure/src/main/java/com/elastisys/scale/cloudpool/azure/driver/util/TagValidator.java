package com.elastisys.scale.cloudpool.azure.driver.util;

import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * An Azure tag validator which checks that a given {@link Map} of tags use keys
 * that follow the Azure tag naming conventions.
 */
public class TagValidator {

    /**
     * Checks that a given {@link Map} of tags use keys that follow the Azure
     * tag naming conventions.
     *
     * @param tags
     * @throws IllegalArgumentException
     */
    public void validate(Map<String, String> tags) throws IllegalArgumentException {
        Preconditions.checkArgument(tags.size() <= 15, "extensions: a maximum of 15 tags may be set");
        for (String tagKey : tags.keySet()) {
            // note: dots are allowed in portal, but not when running through
            // Azure Java SDK
            Preconditions.checkArgument(tagKey.matches("[^:<>\\*%&\\\\\\?/\\+\\.]+"),
                    "extensions: illegal tag key '%s': the following characters are not supported: <>*%&:\\?/+.",
                    tagKey);
        }
    }

}
