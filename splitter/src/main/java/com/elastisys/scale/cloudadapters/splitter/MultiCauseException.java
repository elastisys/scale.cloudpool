package com.elastisys.scale.cloudadapters.splitter;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * An exception that can be used when an exceptional condition is a result of
 * multiple errors.
 */
public class MultiCauseException extends Throwable {

	private static final long serialVersionUID = 1L;
	/** The exceptions that caused this one. */
	private final List<Throwable> causes;

	/**
	 * Create a {@link MultiCauseException} with a collection of causes.
	 * 
	 * @param causes
	 *            The error causes.
	 */
	public MultiCauseException(Collection<? extends Throwable> causes) {
		super();
		this.causes = ImmutableList.copyOf(causes);
	}

	@Override
	public String getMessage() {
		StringBuilder builder = new StringBuilder();
		// builder.append(getClass().getName() + ": ");
		builder.append("[");
		for (int i = 0; i < this.causes.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append("\"");
			builder.append(this.causes.get(i).getMessage());
			builder.append("\"");
		}
		builder.append("]");
		return builder.toString();
	}
}
