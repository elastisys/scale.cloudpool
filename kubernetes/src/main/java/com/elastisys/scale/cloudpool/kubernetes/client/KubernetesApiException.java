package com.elastisys.scale.cloudpool.kubernetes.client;

/**
 * Thrown by a {@link KubernetesClient} to indicate an error condition.
 *
 */
public class KubernetesApiException extends RuntimeException {

	public KubernetesApiException() {
		super();
	}

	public KubernetesApiException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public KubernetesApiException(String message, Throwable cause) {
		super(message, cause);
	}

	public KubernetesApiException(String message) {
		super(message);
	}

	public KubernetesApiException(Throwable cause) {
		super(cause);
	}

}
