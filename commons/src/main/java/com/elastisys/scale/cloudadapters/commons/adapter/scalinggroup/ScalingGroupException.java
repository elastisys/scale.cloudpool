package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;

/**
 * An exception raised by a {@link ScalingGroup} to indicate a failure to
 * complete an operation.
 * 
 * 
 * 
 */
public class ScalingGroupException extends CloudAdapterException {

	/** Default serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a {@link ScalingGroupException}.
	 */
	public ScalingGroupException() {
		super();
	}

	/**
	 * Creates a {@link ScalingGroupException}.
	 * 
	 * @param message
	 * @param cause
	 */
	public ScalingGroupException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a {@link ScalingGroupException}.
	 * 
	 * @param message
	 */
	public ScalingGroupException(String message) {
		super(message);
	}

	/**
	 * Creates a {@link ScalingGroupException}.
	 * 
	 * @param cause
	 */
	public ScalingGroupException(Throwable cause) {
		super(cause);
	}

}
