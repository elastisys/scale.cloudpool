package com.elastisys.scale.cloudpool.commons.basepool.driver;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An error thrown to indicate that some (or all) machines, in a call to
 * {@link CloudPoolDriver#terminateMachines(java.util.List)} were not
 * successfully terminated. The exception includes both the ids of all machine
 * terminations that were successful and, for each failed terminaton, the error
 * that caused the operation to fail.
 */
public class TerminateMachinesException extends CloudPoolDriverException {
    private static final long serialVersionUID = 1L;

    /** The collection of machine (IDs) that were successfully terminated. */
    private final Collection<String> terminatedMachines;

    /**
     * The machine terminations that failed. Keys are machine identifiers,
     * values are the error that caused termination to fail.
     */
    private final Map<String, Throwable> terminationErrors;

    /**
     * Creates a {@link TerminateMachinesException} representing the result of a
     * (partially) failed terminate machines call.
     *
     * @param terminatedMachines
     *            The collection of machine (IDs) that were successfully
     *            terminated.
     * @param terminationErrors
     *            The machine terminations that failed. Keys are machine
     *            identifiers, values are the error that caused termination to
     *            fail.
     */
    public TerminateMachinesException(Collection<String> terminatedMachines, Map<String, Throwable> terminationErrors) {
        super(defaultErrorMessage(terminatedMachines, terminationErrors));
        this.terminatedMachines = terminatedMachines;
        this.terminationErrors = terminationErrors;
    }

    /**
     * The collection of machine (IDs) that were successfully terminated.
     *
     * @return
     */
    public Collection<String> getTerminatedMachines() {
        return this.terminatedMachines;
    }

    /**
     * The machine terminations that failed. Keys are machine identifiers,
     * values are the error that caused termination to fail.
     *
     * @return
     */
    public Map<String, Throwable> getTerminationErrors() {
        return this.terminationErrors;
    }

    /**
     * Returns the error message for each machine failed machine termination.
     * Keys are machine identifiers, values are the termination error message.
     *
     * @return
     */
    public Map<String, String> getTerminationErrorMessages() {
        return this.terminationErrors.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getMessage()));
    }

    private static String defaultErrorMessage(Collection<String> terminatedMachines,
            Map<String, Throwable> terminationErrors) {
        int numRequestedTerminations = terminatedMachines.size() + terminationErrors.size();

        StringWriter errorCauses = new StringWriter();
        for (String machineId : terminationErrors.keySet()) {
            errorCauses.append(String.format("  %s: %s\n", machineId, terminationErrors.get(machineId).getMessage()));
        }

        String message = String.format(
                "only %d out of %d machine terminations completed successfully: unable to terminate %d machines:\n%s",
                terminatedMachines.size(), numRequestedTerminations, terminationErrors.size(), errorCauses.toString());
        return message;
    }

}
