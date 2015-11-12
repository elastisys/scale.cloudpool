package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.google.common.collect.Lists;

/**
 * Hamcrest matcher that will match that any collection of {@link Machine}s that
 * contain a given collection of identifiers.
 */
public class MachinesMatcher extends TypeSafeMatcher<List<Machine>> {
	private static Logger LOG = LoggerFactory.getLogger(MachinesMatcher.class);

	private List<String> expectedMachineIds;

	/**
	 * Constructs an {@link MachinesMatcher} that will match any {@link Machine}
	 * s with the specified id.
	 *
	 * @param expectedMachineId
	 */
	public MachinesMatcher(List<String> expectedMachineIds) {
		this.expectedMachineIds = expectedMachineIds;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(
				"a list of machines with ids " + this.expectedMachineIds);
	}

	@Override
	public boolean matchesSafely(List<Machine> machines) {
		if (this.expectedMachineIds.size() != machines.size()) {
			LOG.warn("expected {} machines, was: {}",
					this.expectedMachineIds.size(), machines.size());
			return false;
		}
		for (Machine machine : machines) {
			if (!this.expectedMachineIds.contains(machine.getId())) {
				LOG.warn("machine id {} was not among expected ones: {}",
						machine.getId(), this.expectedMachineIds.size());
				return false;
			}
		}
		return true;
	}

	/**
	 * Constructs an {@link MachinesMatcher} that will match any {@link Machine}
	 * s with the specified id.
	 *
	 * @param topic
	 * @param severity
	 * @return
	 */
	@Factory
	public static <T> Matcher<List<Machine>> machines(String... expectedIds) {
		return new MachinesMatcher(Lists.newArrayList(expectedIds));
	}
}
