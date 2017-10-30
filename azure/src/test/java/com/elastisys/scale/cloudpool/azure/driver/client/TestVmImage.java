package com.elastisys.scale.cloudpool.azure.driver.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Exercise {@link VmImage}.
 */
public class TestVmImage {

    /**
     * It should be possible to specify an image as a reference to a
     * market-place image.
     */
    @Test
    public void imageFromImageRef() {
        VmImage image = new VmImage("Canonical:UbuntuServer:16.04.0-LTS:2016-01-01");
        assertThat(image.isImageReference(), is(true));
        assertThat(image.isImageId(), is(false));
        assertThat(image.getImageReference().publisher(), is("Canonical"));
        assertThat(image.getImageReference().offer(), is("UbuntuServer"));
        assertThat(image.getImageReference().sku(), is("16.04.0-LTS"));
        assertThat(image.getImageReference().version(), is("2016-01-01"));

        // should not be possible to get image URL
        try {
            image.getImageId();
            fail("should fail");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * If left out, image reference version should be set to "latest".
     */
    @Test
    public void imageFromImageRefWithoutVersion() {
        VmImage image = new VmImage("Canonical:UbuntuServer:16.04.0-LTS");
        assertThat(image.isImageReference(), is(true));
        assertThat(image.isImageId(), is(false));
        assertThat(image.getImageReference().version(), is("latest"));
    }

    @Test
    public void imageFromId() {
        VmImage image = new VmImage(
                "/subscriptions/123/resourceGroups/rg/providers/Microsoft.Compute/images/ubuntu-apache");
        assertThat(image.isImageReference(), is(false));
        assertThat(image.isImageId(), is(true));
        assertThat(image.getImageId(),
                is("/subscriptions/123/resourceGroups/rg/providers/Microsoft.Compute/images/ubuntu-apache"));
    }

}
