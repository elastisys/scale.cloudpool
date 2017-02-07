package com.elastisys.scale.cloudpool.google.commons.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;

/**
 * Utilities for handling instance metadata.
 *
 */
public class MetadataUtil {
    /**
     * Converts the {@link Items} in an instance's {@link Metadata} to a
     * {@link Map}.
     *
     * @param metadata
     * @return
     */
    public static Map<String, String> toMap(Metadata metadata) {
        if (metadata == null) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (Items item : metadata.getItems()) {
            map.put(item.getKey(), item.getValue());
        }
        return map;
    }

    /**
     * Takes a {@link Map} of key-value pairs and produces a corresponding list
     * of instance {@link Metadata} {@link Items}.
     *
     * @param map
     * @return
     */
    public static List<Items> toItems(Map<String, String> map) {
        if (map == null) {
            return Collections.emptyList();
        }

        List<Items> items = new ArrayList<>();
        for (Entry<String, String> keyValuePair : map.entrySet()) {
            items.add(new Items().setKey(keyValuePair.getKey()).setValue(keyValuePair.getValue()));
        }
        return items;
    }

}
