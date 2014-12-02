package com.elastisys.scale.cloudadapers.api.types;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a machine that is a member of a {@link MachinePool} managed by a
 * {@link CloudAdapter}.
 *
 *
 * @see MachinePool
 *
 *
 */
public class Machine {

	/** The identifier of the {@link Machine} . */
	private final String id;
	/** The execution state of the {@link Machine}. */
	private MachineState state;
	/**
	 * Additional state information about the operational status of the machine,
	 * for machines in an <i>active machine state</i> (either
	 * {@link MachineState#PENDING} or {@link MachineState#RUNNING}). This is an
	 * optional field that can be included for cloud adapters that perform
	 * liveness tests on machine pool members. See the table below for the range
	 * of possible values. If not specified, this field is set to
	 * {@link LivenessState#UNKNOWN}.
	 */
	private LivenessState liveness;

	/**
	 * The launch time of the {@link Machine} if it has been launched. This
	 * attribute may be <code>null</code>, depending on the state of the
	 * {@link Machine}.
	 */
	private final DateTime launchtime;
	/**
	 * The list of public IP addresses associated with this {@link Machine}.
	 * Depending on the state of the {@link Machine}, this list may be empty.
	 */
	private final List<String> publicIps;
	/**
	 * The list of private IP addresses associated with this {@link Machine}.
	 * Depending on the state of the {@link Machine}, this list may be empty.
	 */
	private final List<String> privateIps;
	/**
	 * Additional meta-data about the {@link Machine} (as a JSON document).
	 */
	private final JsonElement metadata;

	/**
	 * Constructs a new {@link Machine} with {@link LivenessState#UNKNOWN}
	 * liveness state.
	 *
	 * @param id
	 *            The identifier of the {@link Machine} .
	 * @param state
	 *            The state of the {@link Machine} .
	 * @param launchtime
	 *            The launch time of the {@link Machine} if it has been
	 *            launched. This attribute may be <code>null</code>, depending
	 *            on the state of the {@link Machine}.
	 * @param publicIps
	 *            The list of public IP addresses associated with this
	 *            {@link Machine}. If the machine hasn't (yet) been assigned any
	 *            IP addresses, this attribute can be set to <code>null</code>
	 *            or an empty list.
	 * @param privateIps
	 *            The list of private IP addresses associated with this
	 *            {@link Machine}. If the machine hasn't (yet) been assigned any
	 *            IP addresses, this attribute can be set to <code>null</code>
	 *            or an empty list.
	 * @param metadata
	 *            Additional meta-data about the {@link Machine} (as a JSON
	 *            document). Can be <code>null</code>, in which case it is set
	 *            to an empty {@link JsonObject}.
	 */
	public Machine(String id, MachineState state, DateTime launchtime,
			List<String> publicIps, List<String> privateIps,
			JsonElement metadata) {
		this(id, state, LivenessState.UNKNOWN, launchtime, publicIps,
				privateIps, metadata);
	}

	/**
	 * Constructs a new {@link Machine}.
	 *
	 * @param id
	 *            The identifier of the {@link Machine}.
	 * @param state
	 *            The state of the {@link Machine}.
	 * @param liveness
	 *            Additional state information about the operational status of
	 *            the machine, for machines in an <i>active machine state</i>
	 *            (either {@link MachineState#PENDING} or
	 *            {@link MachineState#RUNNING}).
	 * @param launchtime
	 *            The launch time of the {@link Machine} if it has been
	 *            launched. This attribute may be <code>null</code>, depending
	 *            on the state of the {@link Machine}.
	 * @param publicIps
	 *            The list of public IP addresses associated with this
	 *            {@link Machine}. If the machine hasn't (yet) been assigned any
	 *            IP addresses, this attribute can be set to <code>null</code>
	 *            or an empty list.
	 * @param privateIps
	 *            The list of private IP addresses associated with this
	 *            {@link Machine}. If the machine hasn't (yet) been assigned any
	 *            IP addresses, this attribute can be set to <code>null</code>
	 *            or an empty list.
	 * @param metadata
	 *            Additional meta-data about the {@link Machine} (as a JSON
	 *            document). Can be <code>null</code>, in which case it is set
	 *            to an empty {@link JsonObject}.
	 */
	public Machine(String id, MachineState state, LivenessState liveness,
			DateTime launchtime, List<String> publicIps,
			List<String> privateIps, JsonElement metadata) {
		checkNotNull(id, "missing id");
		checkNotNull(state, "missing state");
		checkNotNull(liveness, "missing liveness");

		this.id = id;
		this.state = state;
		this.liveness = liveness;
		this.launchtime = launchtime;
		this.publicIps = Optional.fromNullable(publicIps).or(
				new ArrayList<String>());
		this.privateIps = Optional.fromNullable(privateIps).or(
				new ArrayList<String>());
		this.metadata = Optional.fromNullable(metadata).or(new JsonObject());
	}

	/**
	 * Returns the identifier of the {@link Machine}.
	 *
	 * @return
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Returns the state of the {@link Machine}.
	 *
	 * @return
	 */
	public MachineState getState() {
		return this.state;
	}

	/**
	 * Sets the state of the {@link Machine}.
	 *
	 * @return
	 */
	public void setState(MachineState state) {
		this.state = state;
	}

	/**
	 * Returns the liveness state of the {@link Machine}.
	 *
	 * @return
	 */
	public LivenessState getLiveness() {
		return this.liveness;
	}

	/**
	 * Sets the liveness state of the {@link Machine}.
	 *
	 * @param liveness
	 */
	public void setLiveness(LivenessState liveness) {
		this.liveness = liveness;
	}

	/**
	 * Returns the launch time of the {@link Machine} if it has been launched.
	 * This attribute may be <code>null</code>, depending on the state of the
	 * {@link Machine}.
	 *
	 * @return
	 */
	public DateTime getLaunchtime() {
		return this.launchtime;
	}

	/**
	 * Returns the list of public IP addresses associated with this
	 * {@link Machine}. Depending on the state of the {@link Machine}, this list
	 * may be empty.
	 *
	 * @return
	 */
	public List<String> getPublicIps() {
		return this.publicIps;
	}

	/**
	 * Returns the list of private IP addresses associated with this
	 * {@link Machine}. Depending on the state of the {@link Machine}, this list
	 * may be empty.
	 *
	 * @return
	 */
	public List<String> getPrivateIps() {
		return this.privateIps;
	}

	/**
	 * Returns additional meta-data about the {@link Machine} (as a JSON
	 * document).
	 *
	 * @return
	 */
	public JsonElement getMetadata() {
		return this.metadata;
	}

	@Override
	public int hashCode() {
		return Objects
				.hashCode(this.id, this.state, this.liveness, this.launchtime,
						this.publicIps, this.privateIps, this.metadata);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Machine) {
			Machine that = (Machine) obj;
			final boolean launchtimesEqual;
			if (this.launchtime != null && that.launchtime != null) {
				launchtimesEqual = this.launchtime.getMillis() == that.launchtime
						.getMillis();
			} else if (this.launchtime == null && that.launchtime == null) {
				launchtimesEqual = true;
			} else {
				launchtimesEqual = false;
			}
			return Objects.equal(this.id, that.id)
					&& Objects.equal(this.state, that.state)
					&& Objects.equal(this.liveness, that.liveness)
					&& launchtimesEqual
					&& Objects.equal(this.publicIps, that.publicIps)
					&& Objects.equal(this.privateIps, that.privateIps)
					&& Objects.equal(this.metadata, that.metadata);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("id", this.id)
				.add("state", this.state).add("liveness", this.liveness)
				.add("launchtime", this.launchtime)
				.add("publicIps", this.publicIps)
				.add("privateIps", this.privateIps)
				.add("metadata", this.metadata).toString();
	}

	/**
	 * Returns a transformation {@link Function} that given a {@link Machine}
	 * extracts its {@link MachineState}.
	 *
	 * @return
	 */
	public static Function<? super Machine, MachineState> toState() {
		return new MachineStateExtractor();
	}

	/**
	 * Returns a transformation {@link Function} that given a {@link Machine}
	 * extracts its identifier.
	 *
	 * @return
	 */
	public static Function<? super Machine, String> toId() {
		return new MachineIdExtractor();
	}

	/**
	 * Returns a {@link Function} that given a {@link Machine} returns the
	 * remaining time (in seconds) to the start of its next hour.
	 *
	 * @return
	 */
	public static Function<? super Machine, Long> remainingInstanceHourTime() {
		return new RemainingInstanceHourTime();
	}

	/**
	 * Returns a {@link Function} that given a {@link Machine} (with a launch
	 * time set) returns the starting point of the {@link Machine}'s most
	 * recently started hour.
	 *
	 * @return
	 */
	public static Function<? super Machine, DateTime> instanceHourStart() {
		return new InstanceHourStart();
	}

	/**
	 * Returns a {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} in a given {@link MachineState}.
	 *
	 * @param state
	 *            The {@link MachineState} for which this {@link Predicate} will
	 *            return <code>true</code>.
	 * @return
	 */
	public static Predicate<? super Machine> withState(MachineState state) {
		return new MachineWithState(state);
	}

	/**
	 * Returns a {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} in a given {@link LivenessState}.
	 *
	 * @param livenessState
	 *            The {@link LivenessState} for which this {@link Predicate}
	 *            will return <code>true</code>.
	 * @return
	 */
	public static Predicate<? super Machine> withLivenessState(
			LivenessState livenessState) {
		return new MachineWithLivenessState(livenessState);
	}

	/**
	 * A {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} in a given {@link MachineState}.
	 *
	 *
	 *
	 */
	public static class MachineWithState implements Predicate<Machine> {
		private final MachineState state;

		public MachineWithState(MachineState state) {
			checkNotNull(state);
			this.state = state;
		}

		@Override
		public boolean apply(Machine machine) {
			return machine.getState() == this.state;
		}
	}

	/**
	 * A {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} in a given {@link MachineState}.
	 *
	 *
	 *
	 */
	public static class MachineWithLivenessState implements Predicate<Machine> {
		private final LivenessState livenessState;

		public MachineWithLivenessState(LivenessState livenessState) {
			checkNotNull(livenessState);
			this.livenessState = livenessState;
		}

		@Override
		public boolean apply(Machine machine) {
			return machine.getLiveness() == this.livenessState;
		}
	}

	/**
	 * A transformation {@link Function} that when applied to a {@link Machine}
	 * extracts the {@link Machine}'s state.
	 * <p/>
	 * Can be used to transform a collection of {@link Machine}s to a collection
	 * of {@link MachineState}. See
	 * {@link Iterables#transform(Iterable, Function)}.
	 *
	 * @see http://code.google.com/p/guava-libraries/wiki/FunctionalExplained
	 *
	 *
	 */
	public static class MachineStateExtractor implements
			Function<Machine, MachineState> {
		/**
		 * Extracts the state of a {@link Machine}.
		 *
		 * @see Function#apply(Object)
		 */
		@Override
		public MachineState apply(Machine machine) {
			return machine.getState();
		}
	}

	/**
	 * A transformation {@link Function} that when applied to a {@link Machine}
	 * extracts the {@link Machine}'s id.
	 * <p/>
	 * Can be used to transform a collection of {@link Machine}s to a collection
	 * of {@link String}. See {@link Iterables#transform(Iterable, Function)}.
	 *
	 * @see http://code.google.com/p/guava-libraries/wiki/FunctionalExplained
	 *
	 *
	 */
	public static class MachineIdExtractor implements Function<Machine, String> {
		/**
		 * Extracts the id of a {@link Machine}.
		 *
		 * @see Function#apply(Object)
		 */
		@Override
		public String apply(Machine machine) {
			return machine.getId();
		}
	}

	/**
	 * Returns a {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} that has been allocated from the underlying
	 * infrastructure and is in a non-terminal state, as indicated by it being
	 * in one of the states: {@link MachineState#REQUESTED},
	 * {@link MachineState#PENDING} or {@link MachineState#RUNNING}.
	 *
	 * @return
	 */
	public static Predicate<? super Machine> isAllocated() {
		return new MachineAllocatedPredicate();
	}

	/**
	 * Returns a {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} that is active, as indicated by it being in one of the
	 * states: {@link MachineState#PENDING} or {@link MachineState#RUNNING}.
	 *
	 * @return
	 */
	public static Predicate<? super Machine> isActive() {
		return new MachineActivePredicate();
	}

	/**
	 * A {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} that has been allocated from the underlying
	 * infrastructure and is in a non-terminal state, as indicated by it being
	 * in one of the states: {@link MachineState#REQUESTED},
	 * {@link MachineState#PENDING} or {@link MachineState#RUNNING}.
	 *
	 *
	 */
	public static class MachineAllocatedPredicate implements Predicate<Machine> {
		private static final Set<MachineState> allocatedStates = Sets
				.newHashSet(MachineState.REQUESTED, MachineState.PENDING,
						MachineState.RUNNING);

		@Override
		public boolean apply(Machine machine) {
			return allocatedStates.contains(machine.getState());
		}
	}

	/**
	 * A {@link Predicate} that returns <code>true</code> when passed a
	 * {@link Machine} that is active, as indicated by it being in one of the
	 * states: {@link MachineState#PENDING} or {@link MachineState#RUNNING}.
	 *
	 *
	 */
	public static class MachineActivePredicate implements Predicate<Machine> {
		private static final Set<MachineState> activeStates = Sets.newHashSet(
				MachineState.PENDING, MachineState.RUNNING);

		@Override
		public boolean apply(Machine machine) {
			return machine.getLaunchtime() != null
					&& activeStates.contains(machine.getState());
		}
	}

	/**
	 * A {@link Function} that for a given {@link Machine} calculates when the
	 * started its most recent hour.
	 * <p/>
	 * The {@link Machine} must have its launch time set.
	 *
	 *
	 */
	public static class InstanceHourStart implements
			Function<Machine, DateTime> {

		/**
		 * Calculates the starting point of the machine's current hour.
		 *
		 * @param machine
		 * @return
		 */
		@Override
		public DateTime apply(Machine machine) {
			checkArgument(machine != null, "null machine");
			checkArgument(machine.getLaunchtime() != null,
					"null launch time for machine");
			DateTime now = UtcTime.now();
			DateTime launchtime = machine.getLaunchtime();
			long secondsPerHour = TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
			long millisPerSecond = TimeUnit.MILLISECONDS.convert(1,
					TimeUnit.SECONDS);
			long millisPerHour = secondsPerHour * millisPerSecond;

			// millis from epoch machine was launched
			long epochMillis = launchtime.getMillis();
			// millis into wall-clock hour machine was launched
			long wallclockhourOffset = epochMillis % millisPerHour;
			// apply the wallclock hour offset to the current hour
			DateTime currentHour = now.withMinuteOfHour(0)
					.withSecondOfMinute(0).withMillisOfSecond(0);
			DateTime HourStart = currentHour.plus(wallclockhourOffset);
			// if that results in a time instant that lies in the future, the
			// start of the current billing hour was in the previous
			// wall-clock hour
			if (now.isBefore(HourStart)) {
				HourStart = HourStart.minusHours(1);
			}
			return HourStart;
		}
	}

	/**
	 * A {@link Function} that for a given {@link Machine} calculates the
	 * remaining time (in seconds) of the machine's current hour.
	 *
	 *
	 */
	public static class RemainingInstanceHourTime implements
			Function<Machine, Long> {

		/**
		 * Calculates the remaining time (in seconds) of the machine's last
		 * started billing hour.
		 *
		 * @param machine
		 * @return
		 */
		@Override
		public Long apply(Machine machine) {
			checkArgument(machine != null, "null machine");
			checkArgument(machine.getLaunchtime() != null,
					"null launch time for machine");

			DateTime HourStart = instanceHourStart().apply(machine);
			DateTime nextHourStart = HourStart.plusHours(1);

			long millisToNextHour = nextHourStart.getMillis()
					- UtcTime.now().getMillis();
			return millisToNextHour / 1000;
		}
	}

	/**
	 * Sorts a collection of {@link Machine}s according to the order prescribed
	 * by a certain {@link Comparator}.
	 *
	 * @param machines
	 * @param comparator
	 * @return
	 */
	public static List<Machine> sort(Collection<Machine> machines,
			Comparator<Machine> comparator) {
		List<Machine> list = Lists.newArrayList(machines);
		Collections.sort(list, comparator);
		return list;
	}
}
