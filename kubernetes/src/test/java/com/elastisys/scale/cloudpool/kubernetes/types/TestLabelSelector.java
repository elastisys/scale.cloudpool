package com.elastisys.scale.cloudpool.kubernetes.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.Test;

/**
 * Exercises {@link LabelSelector}.
 */
public class TestLabelSelector {

    /**
     * Converts a {@link LabelSelector} with <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#set-based-requirement">set-based
     * selectors</a> in {@link LabelSelector#matchExpressions}.
     */
    @Test
    public void convertWithMatchExpressions() {
        LabelSelector selector = new LabelSelector();
        selector.matchExpressions = new ArrayList<>();
        selector.matchExpressions.add(LabelSelectorRequirement.in("app", "nginx"));
        selector.matchExpressions.add(LabelSelectorRequirement.notin("env", "testing", "staging"));
        selector.matchExpressions.add(LabelSelectorRequirement.exists("version"));
        selector.matchExpressions.add(LabelSelectorRequirement.notexists("temporary"));

        assertThat(selector.toLabelSelectorExpression(),
                is("app in (nginx),env notin (testing,staging),version,!temporary"));
    }

    /**
     * Converts a {@link LabelSelector} with <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#equality-based-requirement">equality-based
     * selectors</a> in {@link LabelSelector#matchLabels}.
     */
    @Test
    public void convertWithMatchLabels() {
        LabelSelector selector = new LabelSelector();
        selector.matchLabels = new TreeMap<>();
        selector.matchLabels.put("app", "nginx");
        selector.matchLabels.put("env", "production");

        assertThat(selector.toLabelSelectorExpression(), is("app=nginx,env=production"));
    }

    /**
     * Converts a {@link LabelSelector} with <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#set-based-requirement">set-based
     * selectors</a> in {@link LabelSelector#matchExpressions} AND <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#equality-based-requirement">equality-based
     * selectors</a> in {@link LabelSelector#matchLabels}.
     *
     */
    @Test
    public void convertWithMatchExpressionsAndMatchLabels() {
        LabelSelector selector = new LabelSelector();
        selector.matchExpressions = new ArrayList<>();
        selector.matchExpressions.add(LabelSelectorRequirement.notin("env", "testing", "staging"));
        selector.matchExpressions.add(LabelSelectorRequirement.exists("version"));
        selector.matchLabels = new TreeMap<>();
        selector.matchLabels.put("app", "nginx");

        assertThat(selector.toLabelSelectorExpression(), is("env notin (testing,staging),version,app=nginx"));
    }

}
