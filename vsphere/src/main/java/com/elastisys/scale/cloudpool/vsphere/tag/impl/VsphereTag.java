package com.elastisys.scale.cloudpool.vsphere.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class VsphereTag implements Tag {

    private final ScalingTag key;
    private final String value;

    public VsphereTag(ScalingTag key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key.value;
    }

    public String getValue() {
        return value;
    }

    public enum ScalingTag {

        CLOUD_POOL("elastisys:cloudPool"),
        SERVICE_STATE("elastisys:serviceState"),
        MEMBERSHIP_STATUS("elastisys:membershipStatus"),
        INSTANCE_NAME("Name");

        public final String value;

        ScalingTag(String value){
            this.value = value;
        }

        public static Set<String> getValues(){
            return Arrays.stream(ScalingTag.values()).map(ScalingTag::scalingTagToString).collect(Collectors.toSet());
        }

        private static String scalingTagToString(ScalingTag tag) {
            return tag.value;
        }

    }

}
