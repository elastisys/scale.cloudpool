package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.elastisys.scale.commons.net.retryable.retryhandlers.AbstractLimitedRetryHandler;

/**
 * The Amazon Web Services documentation states that due to eventual consistency
 * and there being a limit to how many requests one is allowed to make over a
 * given time period, one should implement exponential backoff for repeated
 * requests. Additionally, the exceptions in the API inform the caller if a
 * retry is likely to make sense (via
 * {@link AmazonServiceException#isRetryable()} and
 * {@link AmazonClientException#isRetryable()}), and we honor that information.
 *
 * @param <R>
 *            The type of data returned by the call that is to be retried.
 */
public abstract class AbstractAmazonLimitedRetryHandler<R> implements
RetryHandler<R> {

	/** Maximum number of retries. */
	protected final int maxRetries;

	/** Delay (in ms) between retries. */
	protected final AtomicLong delay;

	/** Number of retries carried out so far. */
	private int retries = 0;

	/**
	 * Constructs a new {@link AbstractLimitedRetryHandler} that will attempt a
	 * limited number of retries with a given delay introduced prior to each new
	 * attempt.
	 *
	 * @param maxRetries
	 *            Maximum number of retries. A value less than {@code 0}
	 *            signifies an infinite number of retries.
	 */
	public AbstractAmazonLimitedRetryHandler(int maxRetries) {
		this.maxRetries = maxRetries < 0 ? Integer.MAX_VALUE : maxRetries;
		this.delay = new AtomicLong(1000);
	}

	@Override
	public Action<R> onResponse(R response) {
		if (isSuccessful(response)) {
			return Action.respond(response);
		}
		if (this.retries < this.maxRetries) {
			return retryAfterDelay();
		}

		// retries exceeded: ask subclass how to proceed
		return maxRetriesExceeded(response);
	}

	@Override
	public Action<R> onError(Exception error) {
		if (shouldRetry(error)) {
			return retryAfterDelay();
		}

		// retries exceeded: ask subclass how to proceed
		return maxRetriesExceeded(error);
	}

	private boolean shouldRetry(Exception error) {
		boolean shouldRetry = this.retries < this.maxRetries;

		if (error instanceof AmazonServiceException) {
			AmazonServiceException serviceException = (AmazonServiceException) error;
			shouldRetry = shouldRetry && serviceException.isRetryable();
		} else if (error instanceof AmazonClientException) {
			AmazonClientException clientException = (AmazonClientException) error;
			shouldRetry = shouldRetry && clientException.isRetryable();
		}

		return shouldRetry;
	}

	private Action<R> retryAfterDelay() {
		// introduce wait-time
		introduceDelay();
		this.retries++;
		return Action.retry();
	}

	/**
	 * Strategy method that gets called for every received response to decide if
	 * the response was a successful one, according to the success criteria of
	 * this {@link RetryHandler}.
	 *
	 * @param response
	 * @return
	 */
	public abstract boolean isSuccessful(R response);

	/**
	 * Strategy method that decides on the next action to take when
	 * {@code maxRetries} has been exceeded and the last response received was a
	 * non-successful one (according to {@link #isSuccessful(Object)}).
	 * <p/>
	 * Two sensible options are to either just return the last response (despite
	 * it being unsuccessful) or raising an error that the request failed.
	 *
	 * @param withResponse
	 *            The non-successful response that was received on final
	 *            attempt.
	 * @return The {@link Action} to proceed with.
	 */
	public abstract Action<R> maxRetriesExceeded(R withResponse);

	/**
	 * Strategy method that decides on the next action to take when
	 * {@code maxRetries} has been exceeded and the last request failed with an
	 * error.
	 *
	 * @param withError
	 *            The error that occurred on the final attempt.
	 * @return The {@link Action} to proceed with.
	 */
	public Action<R> maxRetriesExceeded(Exception withError) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last error: %s", this.maxRetries, withError.getMessage());
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message, withError);
		return Action.fail(failureReason);
	}

	private void introduceDelay() {
		try {
			Thread.sleep(this.delay.get());
		} catch (InterruptedException e) {
			throw new RuntimeException(this.getClass().getSimpleName()
					+ " interrupted: " + e.getMessage(), e);
		}
		// exponential backoff
		this.delay.set(this.delay.get() * 2);
	}
}
