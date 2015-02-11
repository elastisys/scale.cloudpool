package com.elastisys.scale.cloudadapers.api.restapi.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.google.common.base.Objects;

/**
 * REST API request type that requests that a machine be detached from the
 * machine pool.
 *
 * @see PoolHandler#detachMachine(String, DetachMachineRequest)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DetachMachineRequest {

	/**
	 * If {@code true}, the desired size of the group should be decremented, if
	 * {@code false} it should be left at its current value.
	 */
	@XmlElement(name = "decrementDesiredSize")
	private boolean decrementDesiredSize;

	public DetachMachineRequest() {
		// empty constructor mandated by JAXB spec
	}

	public DetachMachineRequest(boolean decrementDesiredSize) {
		this.decrementDesiredSize = decrementDesiredSize;
	}

	/**
	 * Indicates if the desired size of the group should be decremented after
	 * terminating the machine.
	 *
	 * @return
	 */
	public boolean isDecrementDesiredSize() {
		return this.decrementDesiredSize;
	}

	/**
	 * Set to <code>true</code> if the desired size of the group should be
	 * decremented after terminating the machine.
	 *
	 * @param decrementDesiredSize
	 */
	public void setDecrementDesiredSize(boolean decrementDesiredSize) {
		this.decrementDesiredSize = decrementDesiredSize;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.decrementDesiredSize);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DetachMachineRequest) {
			DetachMachineRequest that = (DetachMachineRequest) obj;
			return Objects.equal(this.decrementDesiredSize,
					that.decrementDesiredSize);
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("{\"wantReplacement\": %s}",
				this.decrementDesiredSize);
	}
}
