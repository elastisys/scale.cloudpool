package com.elastisys.scale.cloudadapters.aws.commons.client;

import org.apache.commons.codec.binary.Base64;

/**
 * Commonly applicable convenience methods for working against the Amazon AWS
 * client APIs.
 * 
 * 
 */
public class AmazonApiUtils {

	private AmazonApiUtils() {
		throw new IllegalStateException("Not instantiable.");
	}

	/**
	 * Encode a string in <a href="http://en.wikipedia.org/wiki/Base64">base
	 * 64</a>.
	 * 
	 * @param input
	 *            The input string.
	 * @return The input string encoded in <a
	 *         href="http://en.wikipedia.org/wiki/Base64">base 64</a>.
	 */
	public static String base64Encode(String input) {
		return new String(Base64.encodeBase64(input.getBytes()));
	}
}
