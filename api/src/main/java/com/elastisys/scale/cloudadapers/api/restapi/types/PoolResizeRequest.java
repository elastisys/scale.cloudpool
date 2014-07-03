package com.elastisys.scale.cloudadapers.api.restapi.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

/**
 * REST API request type that requests a the machine pool to be (re)sized to a
 * certain number of machines.
 * 
 * 
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PoolResizeRequest {

	@XmlElement(name = "desiredCapacity")
	private int desiredCapacity;

	public PoolResizeRequest() {
		// empty constructor mandated by JAXB spec
	}

	public PoolResizeRequest(int desiredCapacity) {
		this.desiredCapacity = desiredCapacity;
	}

	public void setDesiredCapacity(int desiredCapacity) {
		this.desiredCapacity = desiredCapacity;
	}

	public int getDesiredCapacity() {
		return this.desiredCapacity;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.desiredCapacity);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PoolResizeRequest) {
			PoolResizeRequest that = (PoolResizeRequest) obj;
			return Objects.equal(this.desiredCapacity, that.desiredCapacity);
		}
		return false;

	}

}
