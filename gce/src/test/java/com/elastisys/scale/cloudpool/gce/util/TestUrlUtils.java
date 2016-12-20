package com.elastisys.scale.cloudpool.gce.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercise {@link UrlUtils}.
 */
public class TestUrlUtils {

    @Test
    public void onUrl() {
        String url = "https://www.googleapis.com/compute/v1/projects/elastisys-cloud/zones/europe-west1-b";
        assertThat(UrlUtils.basename(url), is("europe-west1-b"));
    }

    @Test(expected = NullPointerException.class)
    public void onNull() {
        UrlUtils.basename(null);
    }

    @Test
    public void onEmptyString() {
        assertThat(UrlUtils.basename(""), is(""));
    }

    @Test
    public void onPathLessUrl() {
        assertThat(UrlUtils.basename("https://www.googleapis.com"), is("www.googleapis.com"));
    }

    @Test
    public void onPathLessUrl2() {
        assertThat(UrlUtils.basename("https://www.googleapis.com/"), is(""));
    }

    @Test
    public void onAbsolutePath() {
        assertThat(UrlUtils.basename("/home/foo/bar"), is("bar"));
    }

    @Test
    public void onEmptyPath() {
        assertThat(UrlUtils.basename(""), is(""));
    }

    @Test
    public void onRootPath() {
        assertThat(UrlUtils.basename("/"), is(""));
    }

}
