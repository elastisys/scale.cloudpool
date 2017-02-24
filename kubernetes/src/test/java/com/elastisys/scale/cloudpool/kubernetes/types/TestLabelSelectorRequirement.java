package com.elastisys.scale.cloudpool.kubernetes.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

/**
 * Exercises {@link LabelSelectorRequirement}.
 */
public class TestLabelSelectorRequirement {

    /**
     * Verify the conversion of a {@link LabelSelectorRequirement} declaration
     * to a corresponding <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a>.
     */
    @Test
    public void toLabelSelector() {
        assertThat(LabelSelectorRequirement.exists("version").toLabelSelector(), is("version"));
        assertThat(LabelSelectorRequirement.notexists("version").toLabelSelector(), is("!version"));
        assertThat(LabelSelectorRequirement.in("version", "1.0").toLabelSelector(), is("version in (1.0)"));
        assertThat(LabelSelectorRequirement.in("version", "1.0", "1.1").toLabelSelector(), is("version in (1.0,1.1)"));
        assertThat(LabelSelectorRequirement.notin("version", "1.0").toLabelSelector(), is("version notin (1.0)"));
        assertThat(LabelSelectorRequirement.notin("version", "1.0", "1.1").toLabelSelector(),
                is("version notin (1.0,1.1)"));
    }

    /**
     * Test the {@link LabelSelectorRequirement#exists(String)} factory method.
     */
    @Test
    public void existsFactoryMethod() {
        LabelSelectorRequirement actual = LabelSelectorRequirement.exists("version");
        LabelSelectorRequirement expected = new LabelSelectorRequirement();
        expected.key = "version";
        expected.operator = Operator.Exists;
        expected.values = null;
        assertThat(actual, is(expected));
    }

    /**
     * Test the {@link LabelSelectorRequirement#notexists(String)} factory
     * method.
     */
    @Test
    public void notexistsFactoryMethod() {
        LabelSelectorRequirement actual = LabelSelectorRequirement.notexists("version");
        LabelSelectorRequirement expected = new LabelSelectorRequirement();
        expected.key = "version";
        expected.operator = Operator.DoesNotExist;
        expected.values = null;
        assertThat(actual, is(expected));
    }

    /**
     * Test the {@link LabelSelectorRequirement#in(String, String...)} factory
     * method.
     */
    @Test
    public void inFactoryMethod() {
        LabelSelectorRequirement actual = LabelSelectorRequirement.in("app", "nginx");
        LabelSelectorRequirement expected = new LabelSelectorRequirement();
        expected.key = "app";
        expected.operator = Operator.In;
        expected.values = Arrays.asList("nginx");
        assertThat(actual, is(expected));

        // multiple values
        actual = LabelSelectorRequirement.in("version", "1.0", "1.1");
        expected = new LabelSelectorRequirement();
        expected.key = "version";
        expected.operator = Operator.In;
        expected.values = Arrays.asList("1.0", "1.1");
        assertThat(actual, is(expected));
    }

    /**
     * The {@link Operator#In} operator requires a non-empty list of arguments.
     */
    @Test(expected = IllegalArgumentException.class)
    public void inWithNullValues() {
        LabelSelectorRequirement.in("label", null);
    }

    /**
     * The {@link Operator#In} operator requires a non-empty list of arguments.
     */
    @Test(expected = IllegalArgumentException.class)
    public void inWithEmptyValues() {
        LabelSelectorRequirement.in("label", new String[] {});
    }

    /**
     * Test the {@link LabelSelectorRequirement#notin(String, String...)}
     * factory method.
     */
    @Test
    public void notinFactoryMethod() {
        LabelSelectorRequirement actual = LabelSelectorRequirement.notin("app", "nginx");
        LabelSelectorRequirement expected = new LabelSelectorRequirement();
        expected.key = "app";
        expected.operator = Operator.NotIn;
        expected.values = Arrays.asList("nginx");
        assertThat(actual, is(expected));

        // multiple values
        actual = LabelSelectorRequirement.notin("version", "1.0", "1.1");
        expected = new LabelSelectorRequirement();
        expected.key = "version";
        expected.operator = Operator.NotIn;
        expected.values = Arrays.asList("1.0", "1.1");
        assertThat(actual, is(expected));
    }

    /**
     * The {@link Operator#NotIn} operator requires a non-empty list of
     * arguments.
     */
    @Test(expected = IllegalArgumentException.class)
    public void notinWithNullValues() {
        LabelSelectorRequirement.notin("label", null);
    }

    /**
     * The {@link Operator#NotIn} operator requires a non-empty list of
     * arguments.
     */
    @Test(expected = IllegalArgumentException.class)
    public void notinWithEmptyValues() {
        LabelSelectorRequirement.notin("label", new String[] {});
    }

}
