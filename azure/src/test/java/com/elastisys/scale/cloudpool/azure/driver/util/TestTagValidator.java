package com.elastisys.scale.cloudpool.azure.driver.util;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Exercises {@link TagValidator}.
 */
public class TestTagValidator {

    @Test
    public void onValidKeys() {
        Map<String, String> tags = ImmutableMap.of(//
                "a-key", "value", //
                "AnotherKey", "value", //
                "Yet(Another)Key", "value", //
                "$key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsColon() {
        Map<String, String> tags = ImmutableMap.of("a:key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsLessThan() {
        Map<String, String> tags = ImmutableMap.of("a<key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsGreaterThan() {
        Map<String, String> tags = ImmutableMap.of("a>key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsAsterisk() {
        Map<String, String> tags = ImmutableMap.of("a*key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsPercent() {
        Map<String, String> tags = ImmutableMap.of("a%key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsAmpersand() {
        Map<String, String> tags = ImmutableMap.of("a&key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsBackslash() {
        Map<String, String> tags = ImmutableMap.of("a\\key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsQuestionMark() {
        Map<String, String> tags = ImmutableMap.of("a?key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsSlash() {
        Map<String, String> tags = ImmutableMap.of("a/key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsPlus() {
        Map<String, String> tags = ImmutableMap.of("a+key", "value");
        new TagValidator().validate(tags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainsDot() {
        Map<String, String> tags = ImmutableMap.of("a.key", "value");
        new TagValidator().validate(tags);
    }

}
