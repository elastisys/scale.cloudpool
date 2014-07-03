package com.elastisys.scale.cloudadapters.openstack.faults;

import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;

/**
 * An exception indicating a problem with allocating a floating IP address to a
 * server instance.
 *
 * 
 *
 */
public class FloatingIpAddressException extends ScalingGroupException {

	/**
	 * Constructs a new {@link FloatingIpAddressException}.
	 */
	public FloatingIpAddressException() {
		super();
	}

	/**
	 * Constructs a new {@link FloatingIpAddressException}.
	 *
	 * @param message
	 * @param cause
	 */
	public FloatingIpAddressException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link FloatingIpAddressException}.
	 *
	 * @param message
	 */
	public FloatingIpAddressException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link FloatingIpAddressException}.
	 *
	 * @param cause
	 */
	public FloatingIpAddressException(Throwable cause) {
		super(cause);
	}
}
