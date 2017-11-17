package com.elastisys.scale.cloudpool.azure.driver;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;

/**
 * Hamcrest matcher that will match that any collection of {@link Machine}s that
 * contain a given collection of identifiers.
 */
public class MachinesMatcher extends TypeSafeMatcher<List<Machine>> {
    private static Logger LOG = LoggerFactory.getLogger(MachinesMatcher.class);

    private List<String> expectedMachineNames;

    /**
     * Constructs an {@link MachinesMatcher} that will match any list of
     * {@link Machine}s containing the specified names. Note that the machine
     * ids are likely to be paths of form
     * {@code /subscriptions/.../virtualMachines/worker-1}, but the name is a
     * short-form of the id, like {@code worker-1}.
     *
     * @param expectedMachineId
     */
    public MachinesMatcher(List<String> expectedMachineIds) {
        this.expectedMachineNames = expectedMachineIds;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a list of machine ids containing names " + this.expectedMachineNames);
    }

    @Override
    public boolean matchesSafely(List<Machine> machines) {
        if (this.expectedMachineNames.size() != machines.size()) {
            LOG.warn("expected {} machines, was: {}", this.expectedMachineNames.size(), machines.size());
            return false;
        }
        for (String expectedName : this.expectedMachineNames) {
            boolean nameFound = false;
            for (Machine machine : machines) {
                if (machine.getId().endsWith(expectedName)) {
                    nameFound = true;
                    break;
                }
            }
            if (!nameFound) {
                LOG.warn("no machine was found with an id ending in expected VM name {}", expectedName);
                return false;
            }
        }
        return true;
    }

    /**
     * Constructs an {@link MachinesMatcher} that will match any list of
     * {@link Machine}s containing the specified names. Note that the machine
     * ids are likely to be paths of form
     * {@code /subscriptions/.../virtualMachines/worker-1}, but the name is a
     * short-form of the id, like {@code worker-1}.
     *
     * @param topic
     * @param severity
     * @return
     */
    @Factory
    public static <T> Matcher<List<Machine>> machines(String... expectedVmNames) {
        return new MachinesMatcher(Arrays.asList(expectedVmNames));
    }
}
