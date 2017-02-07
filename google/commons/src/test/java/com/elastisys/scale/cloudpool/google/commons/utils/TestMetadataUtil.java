package com.elastisys.scale.cloudpool.google.commons.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises {@link MetadataUtil}.
 */
public class TestMetadataUtil {

    /**
     * Verify conversion of {@link Metadata} to {@link Map}.
     */
    @Test
    public void toMap() {
        // sample instance metadata
        Metadata metadata = new Metadata().setFingerprint("Z-nUqUWbL1c=").setKind("compute#metadata").setItems(//
                items(//
                        item("startup-script", "#!/bin/bash\napt update -qy && apt install -qy apache2"), //
                        item("ssh-keys", "foo:ssh-rsa XXXXXXXX foo@bar")));

        // convert
        Map<String, String> map = MetadataUtil.toMap(metadata);

        // verify
        assertThat(map.size(), is(2));
        assertThat(map.get("startup-script"), is("#!/bin/bash\napt update -qy && apt install -qy apache2"));
        assertThat(map.get("ssh-keys"), is("foo:ssh-rsa XXXXXXXX foo@bar"));
    }

    @Test
    public void toMapOnEmptyMetadata() {
        // sample instance metadata
        Metadata metadata = new Metadata().setFingerprint("Z-nUqUWbL1c=").setKind("compute#metadata")
                .setItems(Collections.emptyList());

        // convert
        Map<String, String> map = MetadataUtil.toMap(metadata);

        // verify
        assertThat(map.size(), is(0));
    }

    @Test
    public void toMapOnNullMetadata() {
        // convert
        Map<String, String> map = MetadataUtil.toMap(null);

        // verify
        assertThat(map.size(), is(0));
    }

    /**
     * Verify conversion of a {@link Map} to a list of {@link Items}.
     */
    @Test
    public void toItems() {
        Map<String, String> map = ImmutableMap.of(//
                "startup-script", "#!/bin/bash\napt update -qy && apt install -qy apache2", //
                "ssh-keys", "foo:ssh-rsa XXXXXXXX foo@bar");

        // convert
        List<Items> items = MetadataUtil.toItems(map);

        // verify
        assertThat(items,
                is(items(//
                        item("startup-script", "#!/bin/bash\napt update -qy && apt install -qy apache2"), //
                        item("ssh-keys", "foo:ssh-rsa XXXXXXXX foo@bar"))));
    }

    @Test
    public void toItemsOnEmptyMap() {
        Map<String, String> map = new HashMap<>();

        // convert
        List<Items> items = MetadataUtil.toItems(map);

        // verify
        assertThat(items, is(items()));
    }

    @Test
    public void toItemsOnNullMap() {
        // convert
        List<Items> items = MetadataUtil.toItems(null);

        // verify
        assertThat(items, is(items()));
    }

    private List<Items> items(Items... items) {
        return Arrays.asList(items);
    }

    private Items item(String key, String value) {
        return new Items().setKey(key).setValue(value);
    }
}
