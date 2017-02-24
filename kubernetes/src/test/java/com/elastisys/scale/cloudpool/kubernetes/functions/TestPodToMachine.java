package com.elastisys.scale.cloudpool.kubernetes.functions;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.kubernetes.types.ObjectMeta;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.cloudpool.kubernetes.types.PodStatus;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercise {@link PodToMachine}.
 */
public class TestPodToMachine {

    /**
     * At a minimum, the {@link Pod} needs to have fields {@code metadata.name},
     * {@code status.phase} set.
     */
    @Test
    public void minimal() {
        Pod minimal = minimal("nginx-123", "Pending");
        Machine machine = new PodToMachine().apply(minimal);

        assertThat(machine.getId(), is("nginx-123"));
        assertThat(machine.getMachineState(), is(MachineState.PENDING));

        assertThat(machine.getCloudProvider(), is(PodToMachine.CLOUD_PROVIDER));
        assertThat(machine.getRegion(), is(PodToMachine.REGION));
        assertThat(machine.getMachineSize(), is(PodToMachine.MACHINE_SIZE));
        assertThat(machine.getLaunchTime(), is(nullValue()));
        assertThat(machine.getRequestTime(), is(nullValue()));
        assertThat(machine.getPrivateIps(), is(Collections.emptyList()));
        assertThat(machine.getPublicIps(), is(Collections.emptyList()));
        assertThat(machine.getMembershipStatus(), is(MembershipStatus.defaultStatus()));
        assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
    }

    /**
     * Make sure that fields {@code status.startTime} (launchTime),
     * {@code status.hostIP} (public IP), {@code status.podIP} (private IP),
     * {@code metadata.creationTimestamp} (requestTime) get properly converted.
     */
    @Test
    public void complete() {
        Pod pod = minimal("nginx-123", "Running");
        pod.metadata.creationTimestamp = UtcTime.parse("2017-01-01T11:59:45.000Z");
        pod.status.startTime = UtcTime.parse("2017-01-01T12:00:00.000Z");
        pod.status.hostIP = "192.168.99.100";
        pod.status.podIP = "10.0.0.2";

        Machine machine = new PodToMachine().apply(pod);

        assertThat(machine.getId(), is("nginx-123"));
        assertThat(machine.getMachineState(), is(MachineState.RUNNING));

        assertThat(machine.getCloudProvider(), is(PodToMachine.CLOUD_PROVIDER));
        assertThat(machine.getRegion(), is(PodToMachine.REGION));
        assertThat(machine.getMachineSize(), is(PodToMachine.MACHINE_SIZE));
        assertThat(machine.getLaunchTime(), is(pod.status.startTime));
        assertThat(machine.getRequestTime(), is(pod.metadata.creationTimestamp));
        assertThat(machine.getPrivateIps(), is(asList(pod.status.podIP)));
        assertThat(machine.getPublicIps(), is(asList(pod.status.hostIP)));
        assertThat(machine.getMembershipStatus(), is(MembershipStatus.defaultStatus()));
        assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
    }

    /**
     * Creates a {@link Pod} metadata object with the minimal amount of fields
     * for the {@link PodToMachine} function to be able to convert it to a
     * {@link Machine}.
     *
     * @param podName
     * @param phase
     * @return
     */
    private Pod minimal(String podName, String phase) {
        Pod minimal = new Pod();
        minimal.metadata = new ObjectMeta();
        minimal.metadata.name = podName;
        minimal.status = new PodStatus();
        minimal.status.phase = phase;
        return minimal;
    }
}
