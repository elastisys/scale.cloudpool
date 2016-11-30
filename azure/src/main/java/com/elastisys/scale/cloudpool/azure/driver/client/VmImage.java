package com.elastisys.scale.cloudpool.azure.driver.client;

import static com.google.common.base.Preconditions.checkState;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.azure.management.compute.ImageReference;

/**
 * Represents an Azure VM image, which can either be specified as a URN
 * {@code <publisher>:<offer>:<sku>[:<version>]} or as an absolute URL.
 */
public class VmImage {

    /**
     * Image reference URN pattern:
     * {@code <publisher>:<offer>:<sku>[:<version>]}
     */
    private static Pattern IMAGE_URN_PATTERN = Pattern.compile("([^:]+):([^:]+):([^:]+)(:([^:]+))?");
    private static final String DEFAULT_IMAGE_REF_VERSION = "latest";

    /** <code>true</code> if this {@link VmImage} is a image reference URN. */
    private boolean isImageReference = false;
    /** will be set if {@link #isImageReference} is <code>true</code>. */
    private ImageReference imageReference = null;

    /** <code>true</code> if this {@link VmImage} is a URL. */
    private boolean isImageUrl = false;
    /** will be set if {@link #isImageUrl} is <code>true</code>. */
    private String imageUrl = null;

    /**
     * Creates a {@link VmImage} representation.
     *
     * @param image
     *            Either a VM image reference (URN:
     *            {@code<publisher>:<offer>:<sku>[:<version>]} or a direct image
     *            URL.
     */
    public VmImage(String image) {
        try {
            this.imageReference = tryParseImageRef(image);
            this.isImageReference = true;
        } catch (IllegalArgumentException e) {
            // not an image reference, try parsing as URL
            try {
                this.imageUrl = new URL(image).toString();
                this.isImageUrl = true;
            } catch (MalformedURLException e1) {
                throw new IllegalArgumentException(String.format(
                        "image %s is neither an image reference URN (of form PUBLISHER:OFFER:SKU[:VERSION]) nor an image URL",
                        image));
            }
        }
    }

    public ImageReference getImageReference() {
        checkState(this.isImageReference, "specified image is NOT an image reference");
        return this.imageReference;
    }

    public String getImageURL() {
        checkState(this.isImageUrl, "specified image is NOT an image URL");
        return this.imageUrl;
    }

    public boolean isImageReference() {
        return this.isImageReference;
    }

    public boolean isImageUri() {
        return this.isImageUrl;
    }

    static ImageReference tryParseImageRef(String image) {
        Matcher urnMatcher = IMAGE_URN_PATTERN.matcher(image);

        if (!urnMatcher.matches()) {
            throw new IllegalArgumentException(String.format("image %s is not a image reference URN", image));
        }

        ImageReference imageRef = new ImageReference().withPublisher(urnMatcher.group(1)).withOffer(urnMatcher.group(2))
                .withSku(urnMatcher.group(3));
        if (urnMatcher.group(5) != null) {
            imageRef.withVersion(urnMatcher.group(5));
        } else {
            imageRef.withVersion(DEFAULT_IMAGE_REF_VERSION);
        }
        return imageRef;
    }

}
