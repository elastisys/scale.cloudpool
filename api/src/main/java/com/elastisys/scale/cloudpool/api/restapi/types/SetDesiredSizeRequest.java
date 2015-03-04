package com.elastisys.scale.cloudpool.api.restapi.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolHandler;
import com.google.common.base.Objects;

/**
 * REST API request type that requests a certain desired size of the machine
 * pool.
 *
 * @see CloudPoolHandler#setDesiredSize(SetDesiredSizeRequest)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SetDesiredSizeRequest {

	@XmlElement(name = "desiredSize")
	private int desiredSize;

	public SetDesiredSizeRequest() {
		// empty constructor mandated by JAXB spec
	}

	public SetDesiredSizeRequest(int desiredSize) {
		this.desiredSize = desiredSize;
	}

	public void setDesiredSize(int desiredSize) {
		this.desiredSize = desiredSize;
	}

	public int getDesiredSize() {
		return this.desiredSize;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.desiredSize);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SetDesiredSizeRequest) {
			SetDesiredSizeRequest that = (SetDesiredSizeRequest) obj;
			return Objects.equal(this.desiredSize, that.desiredSize);
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("{\"desiredSize\": %d}", this.desiredSize);
	}
}
