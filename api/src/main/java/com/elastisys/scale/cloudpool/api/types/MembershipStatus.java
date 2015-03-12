package com.elastisys.scale.cloudpool.api.types;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The pool membership status indicates if a {@link Machine} needs to be given
 * special treatment.
 * <p/>
 * The membership status for a machine can, for example, be set to protect the
 * machine from being terminated (by settings its evictability status) or to
 * mark a machine as being in need of replacement by flagging it as an inactive
 * pool member.
 *
 * @see Machine
 * @see MachinePool
 * @see CloudPool
 */
public class MembershipStatus {

	/**
	 * Indicates if this is an active (working) {@link MachinePool} member. A
	 * {@code false} value, indicates to the {@link CloudPool} that a
	 * replacement machine needs to be launched for this pool member (at least
	 * until this machine has been troubleshooted and had its {@link #active}
	 * flag set back to <code>true</code>).
	 * <p/>
	 * The default value is <code>true</code>.
	 */
	private final boolean active;

	/**
	 * Indicates if this {@link Machine} is a blessed member of the
	 * {@link MachinePool}. If {@code true}, the {@link CloudPool} may not
	 * terminate this machine.
	 * <p/>
	 * The default value is <code>true</code>.
	 */
	private final boolean evictable;

	/**
	 * Creates a default {@link MembershipStatus} that marks a machine both
	 * active and evictable.
	 */
	public MembershipStatus() {
		this(true, true);
	}

	/**
	 * Creates a {@link MembershipStatus} for a machine.
	 *
	 * @param active
	 *            Indicates if this is an active (working) {@link MachinePool}
	 *            member. A {@code false} value, indicates to the
	 *            {@link CloudPool} that a replacement machine needs to be
	 *            launched for this pool member (at least until this machine has
	 *            been troubleshooted and had its {@link #active} flag set back
	 *            to <code>true</code>).
	 * @param evictable
	 *            Indicates if this {@link Machine} is a blessed member of the
	 *            {@link MachinePool}. If {@code true}, the {@link CloudPool}
	 *            may not terminate this machine.
	 */
	public MembershipStatus(boolean active, boolean evictable) {
		this.active = active;
		this.evictable = evictable;
	}

	/**
	 * Creates a default {@link MembershipStatus} that marks a machine both
	 * active and evictable.
	 *
	 * @return
	 */
	public static MembershipStatus defaultStatus() {
		return new MembershipStatus(true, true);
	}

	/**
	 * Creates a {@link MembershipStatus} that marks a machine as blessed (or a
	 * permanent pool member that cannot be evicted).
	 *
	 * @return
	 */
	public static MembershipStatus blessed() {
		return new MembershipStatus(true, false);
	}

	/**
	 * Creates a {@link MembershipStatus} that marks a machine as being
	 * non-functional (i.e. inactive) and in need of service. The
	 * {@link Machine} should be replaced and should not be terminated (it is
	 * kept alive for troubleshooting).
	 *
	 * @return
	 */
	public static MembershipStatus awaitingService() {
		return new MembershipStatus(false, false);
	}

	/**
	 * Creates a {@link MembershipStatus} that marks a machine as being
	 * non-functional (i.e. inactive) and in need of replacement. The
	 * {@link Machine} should be replaced and can be terminated.
	 *
	 * @return
	 */
	public static MembershipStatus disposable() {
		return new MembershipStatus(false, true);
	}

	/**
	 * Indicates if this is an active (working) {@link MachinePool} member. A
	 * {@code false} value, indicates to the {@link CloudPool} that a
	 * replacement machine needs to be launched for this pool member (at least
	 * until this machine has been troubleshooted and had its {@link #active}
	 * flag set back to <code>true</code>).
	 *
	 * @return
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * Indicates if this {@link Machine} is a blessed member of the
	 * {@link MachinePool}. If {@code true}, the {@link CloudPool} may not
	 * terminate this machine.
	 *
	 * @return
	 */
	public boolean isEvictable() {
		return this.evictable;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.active, this.evictable);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MembershipStatus) {
			MembershipStatus that = (MembershipStatus) obj;
			return Objects.equal(this.active, that.active)
					&& Objects.equal(this.evictable, that.evictable);

		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("").add("active", this.active)
				.add("evictable", this.evictable).toString();
	}
}
