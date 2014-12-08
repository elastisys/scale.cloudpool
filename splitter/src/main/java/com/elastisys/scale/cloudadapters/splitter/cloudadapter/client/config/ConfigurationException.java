package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config;

/**
 * The configuration is incorrect in some way (missing or unacceptable values).
 * A descriptive message must be provided.
 */
public class ConfigurationException extends Exception {
	private static final long serialVersionUID = 0xC0DEC0DE;

	/**
	 * Creates a new instance, setting the message and cause parameters.
	 *
	 * @param message
	 *            Description of what went wrong.
	 * @param cause
	 *            The cause of the exception.
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new instance with the given message.
	 *
	 * @param message
	 *            Description of what went wrong.
	 */
	public ConfigurationException(String message) {
		super(message);
	}

}
