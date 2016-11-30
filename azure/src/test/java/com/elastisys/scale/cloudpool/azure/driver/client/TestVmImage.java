package com.elastisys.scale.cloudpool.azure.driver.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link VmImage}.
 */
public class TestVmImage {

    @Test
    public void imageFromUrn() {
        VmImage image = new VmImage("Canonical:UbuntuServer:16.04.0-LTS:2016-01-01");
        assertThat(image.isImageReference(), is(true));
        assertThat(image.isImageUri(), is(false));
        assertThat(image.getImageReference().publisher(), is("Canonical"));
        assertThat(image.getImageReference().offer(), is("UbuntuServer"));
        assertThat(image.getImageReference().sku(), is("16.04.0-LTS"));
        assertThat(image.getImageReference().version(), is("2016-01-01"));

        // should not be possible to get image URL
        try {
            image.getImageURL();
            fail("should fail");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * If left out, image reference version should be set to "latest".
     */
    @Test
    public void imageFromUrnWithoutVersion() {
        VmImage image = new VmImage("Canonical:UbuntuServer:16.04.0-LTS");
        assertThat(image.isImageReference(), is(true));
        assertThat(image.isImageUri(), is(false));
        assertThat(image.getImageReference().version(), is("latest"));
    }

    @Test
    public void imageFromUrl() {
        VmImage image = new VmImage("https://my.site/vm/image.iso");
        assertThat(image.isImageReference(), is(false));
        assertThat(image.isImageUri(), is(true));
        assertThat(image.getImageURL(), is("https://my.site/vm/image.iso"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalUrn() {
        new VmImage("Canonical:UbuntuServer");
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalUrl() {
        new VmImage("tcp://some/image");
    }

}
