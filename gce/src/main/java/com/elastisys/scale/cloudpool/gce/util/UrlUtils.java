package com.elastisys.scale.cloudpool.gce.util;

public class UrlUtils {

    /**
     * Extracts the "basename" of an URL or path -- the string following that
     * last {@code /} in the path/URL. For example, {@code europe-west1-b} from
     * a URL such as
     * {@code https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-b}.
     *
     * @param zoneUrl
     * @return
     */
    public static String basename(String urlOrPath) {
        int lastSlashIndex = urlOrPath.lastIndexOf("/");
        if (lastSlashIndex < 0) {
            return urlOrPath;
        }
        return urlOrPath.substring(lastSlashIndex + 1);
    }
}
