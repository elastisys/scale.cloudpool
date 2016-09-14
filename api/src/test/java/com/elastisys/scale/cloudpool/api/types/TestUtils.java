package com.elastisys.scale.cloudpool.api.types;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.google.common.collect.Lists;

/**
 * Convenience methods used by {@link MachinePool}-related tests.
 */
public class TestUtils {

    /**
     * Convenience method for creating a {@link MachinePool}.
     *
     * @param timestamp
     * @param machines
     * @return
     */
    public static MachinePool pool(DateTime timestamp, Machine... machines) {
        List<Machine> machineList = Lists.newArrayList(machines);
        return new MachinePool(machineList, timestamp);
    }

    /**
     * Convenience method for creating a {@link Machine} without IP addresses.
     *
     * @param id
     * @param state
     * @param launchtime
     * @return
     */
    public static Machine machineNoIp(String id, MachineState state, DateTime requesttime, DateTime launchtime) {
        return Machine.builder().id(id).machineState(state).cloudProvider("AWS-EC2").region("us-east-1")
                .machineSize("m1.small").requestTime(requesttime).launchTime(launchtime).build();
    }

    /**
     * Convenience method for creating a {@link Machine} without IP addresses
     * and with identical request and launch times.
     *
     * @param id
     * @param state
     * @param launchtime
     * @return
     */
    public static Machine machineNoIp(String id, MachineState state, DateTime launchtime) {
        return machineNoIp(id, state, launchtime, launchtime);
    }

    /**
     * Convenience method for creating a {@link Machine} with a given request
     * and launch time.
     *
     * @param id
     * @param launchtime
     * @return
     */
    public static Machine machine(String id, DateTime requesttime, DateTime launchtime) {
        return Machine.builder().id(id).machineState(MachineState.RUNNING).cloudProvider("AWS-EC2").region("us-east-1")
                .machineSize("m1.small").requestTime(requesttime).launchTime(launchtime).build();
    }

    /**
     * Convenience method for creating a {@link Machine} with given identical
     * request and launch times.
     *
     * @param id
     * @param launchtime
     * @return
     */
    public static Machine machine(String id, DateTime launchtime) {
        return machine(id, launchtime, launchtime);
    }

    /**
     * Convenience method for creating a {@link Machine} with public and/or
     * private IP address(es).
     *
     * @param id
     * @param state
     * @param launchtime
     * @param publicIps
     * @param privateIps
     * @return
     */
    public static Machine machine(String id, MachineState state, DateTime requesttime, DateTime launchtime,
            List<String> publicIps, List<String> privateIps) {
        return Machine.builder().id(id).machineState(state).cloudProvider("AWS-EC2").region("us-east-1")
                .machineSize("m1.small").requestTime(requesttime).launchTime(launchtime).publicIps(publicIps)
                .privateIps(privateIps).build();
    }

    /**
     * Convenience method for creating a {@link Machine} with public and/or
     * private IP address(es) and identical request and launch times.
     *
     * @param id
     * @param state
     * @param launchtime
     * @param publicIps
     * @param privateIps
     * @return
     */
    public static Machine machine(String id, MachineState state, DateTime launchtime, List<String> publicIps,
            List<String> privateIps) {
        return machine(id, state, launchtime, launchtime, publicIps, privateIps);
    }

    public static List<String> ips(String... ipAddresses) {
        return Arrays.asList(ipAddresses);
    }

    public static long secondsBetween(DateTime start, DateTime end) {
        return Seconds.secondsBetween(start, end).getSeconds();
    }
}
