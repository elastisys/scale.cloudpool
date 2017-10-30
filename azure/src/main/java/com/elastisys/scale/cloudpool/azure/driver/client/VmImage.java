package com.elastisys.scale.cloudpool.azure.driver.client;

import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.azure.management.compute.ImageReference;

/**
 * Represents an Azure VM image, which can either be specified as an image
 * reference ({@code <publisher>:<offer>:<sku>[:<version>]}) to use an existing
 * image from the market place or as the id of a custom image
 * ({@code /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.Compute/images/<imageName>}).
 */
public class VmImage {

    /**
     * Image reference URN pattern:
     * {@code <publisher>:<offer>:<sku>[:<version>]}
     */
    private static Pattern IMAGE_REF_PATTERN = Pattern.compile("([^:]+):([^:]+):([^:]+)(:([^:]+))?");
    private static final String DEFAULT_IMAGE_REF_VERSION = "latest";

    /** The original image spec that the {@link VmImage} was created from. */
    private final String imageSpec;

    /**
     * <code>true</code> if this {@link VmImage} is a image reference
     * ({@code <publisher>:<offer>:<sku>[:<version>]}).
     */
    private boolean isImageReference = false;
    /** will be set if {@link #isImageReference} is <code>true</code>. */
    private ImageReference imageReference = null;

    /** <code>true</code> if this {@link VmImage} is an image id. */
    private boolean isImageId = false;
    /** will be set if {@link #isImageId} is <code>true</code>. */
    private String imageId = null;

    /**
     * Creates a {@link VmImage} representation.
     *
     * @param imageSpec
     *            Either an image reference URN
     *            ({@code <publisher>:<offer>:<sku>[:<version>]}) to use an
     *            existing image from the market place or an id of a custom
     *            image
     *            ({@code /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.Compute/images/<imageName>}).
     */
    public VmImage(String imageSpec) {
        this.imageSpec = imageSpec;
        try {
            this.imageReference = tryParseImageRef(imageSpec);
            this.isImageReference = true;
        } catch (IllegalArgumentException e) {
            // not an image reference, assume that this is an image id path
            this.imageId = imageSpec;
            this.isImageId = true;
        }
    }

    /**
     * Returns the image reference (of form PUBLISHER:OFFER:SKU[:VERSION])
     * represented by this {@link VmImage}, or throws an
     * {@link IllegalStateException} if this {@link VmImage} does not represent
     * an image reference. See {@link #isImageReference()}.
     *
     * @return
     *
     */
    public ImageReference getImageReference() {
        checkState(this.isImageReference, "specified image is NOT an image reference");
        return this.imageReference;
    }

    /**
     * Returns the image id represented by this {@link VmImage}, or throws an
     * {@link IllegalStateException} if this {@link VmImage} does not represent
     * an image id. See {@link #isImageId()}.
     *
     * @return
     */
    public String getImageId() {
        checkState(this.isImageId, "specified image is NOT an image id");
        return this.imageId;
    }

    /**
     * Returns <code>true</code> if this {@link VmImage} represents an image URN
     * (of form PUBLISHER:OFFER:SKU[:VERSION]). If so,
     * {@link VmImage#getImageReference()} will return the URN.
     *
     * @return
     */
    public boolean isImageReference() {
        return this.isImageReference;
    }

    /**
     * Returns <code>true</code> if this {@link VmImage} represents an image id.
     * If so, {@link #getImageId()} will return the image id.
     *
     * @return
     */
    public boolean isImageId() {
        return this.isImageId;
    }

    static ImageReference tryParseImageRef(String image) {
        Matcher urnMatcher = IMAGE_REF_PATTERN.matcher(image);

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

    @Override
    public int hashCode() {
        return Objects.hash(this.imageSpec);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VmImage) {
            VmImage that = (VmImage) obj;
            return Objects.equals(this.imageSpec, that.imageSpec);
        }
        return false;
    }
}
