package com.elastisys.scale.cloudpool.gce.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.util.Preconditions;

/**
 * Utility class for parsing out zone/region from zone URLs.
 */
public class ZoneUtils {
    /**
     * A zone has form: {@code <continent>-<region>-<zone>}. For example,
     * {@code us-east1-b}.
     */
    private static final Pattern ZONE_PATTERN = Pattern.compile("\\w+\\-\\w+\\-\\w+");

    /**
     * Extracts the zone name (for example, {@code europe-west1-b} from a zone
     * URL such as
     * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-b}.
     *
     * @param zoneUrl
     * @return
     */
    public static String zoneName(String zoneUrl) {
        Pattern zoneUrlPattern = Pattern.compile("^http.*/([a-zA-Z_0-9\\-]+)$");
        Matcher matcher = zoneUrlPattern.matcher(zoneUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("%s does not appear to be a valid zone URL", zoneUrl));
        }
        return UrlUtils.basename(zoneUrl);
    }

    /**
     * Extracts the region name from a zone name. For example, zone
     * {@code europe-west1-b} is in region {@code europe-west1}.
     *
     * @param zoneUrl
     * @return
     */
    public static String regionName(String zoneName) {
        Preconditions.checkArgument(ZONE_PATTERN.matcher(zoneName).matches(), "not a valid zone name: %s", zoneName);
        return zoneName.substring(0, zoneName.length() - 2);
    }
}
