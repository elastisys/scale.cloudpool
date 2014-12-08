package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * A message that encapsulates information about an error that has occurred. See
 * the <a href=
 * "http://cloudadapterapi.readthedocs.org/en/latest/api.html#error-response-message"
 * >API</a>.
 *
 * <p>
 * This class is thread-safe by being immutable.
 * </p>
 */
public final class ErrorResponseMessage {
	private final String message;
	private final String detail;

	/**
	 * Creates a new instance with the given message and detailed explanation of
	 * the error message.
	 *
	 * @param message
	 *            The error message. Cannot be null.
	 * @param detail
	 *            A detailed explanation of the error message. Can be null.
	 */
	public ErrorResponseMessage(String message, String detail) {
		Preconditions.checkNotNull(message, "Message cannot be null");
		this.message = message;
		this.detail = detail == null ? "" : detail;
	}

	/**
	 * @return The error message.
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @return The error detail, typically, a stack trace or similar.
	 */
	public String getDetail() {
		return this.detail;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.message, this.detail);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ErrorResponseMessage that = (ErrorResponseMessage) obj;

		return Objects.equals(this.message, that.message)
				&& Objects.equals(this.detail, that.detail);
	}

}
