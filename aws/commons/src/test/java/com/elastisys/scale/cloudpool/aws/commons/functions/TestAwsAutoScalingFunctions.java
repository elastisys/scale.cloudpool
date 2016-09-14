package com.elastisys.scale.cloudpool.aws.commons.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.aws.commons.functions.AwsAutoScalingFunctions;

/**
 * Exercises the {@link AwsAutoScalingFunctions}.
 *
 * 
 */
public class TestAwsAutoScalingFunctions {

    @Test
    public void testToAutoScalingInstanceId() {
        assertThat(AwsAutoScalingFunctions.toAutoScalingInstanceId().apply(awsAsInstance("i-123456")), is("i-123456"));
    }

    private com.amazonaws.services.autoscaling.model.Instance awsAsInstance(String withInstanceId) {
        return new com.amazonaws.services.autoscaling.model.Instance().withInstanceId(withInstanceId);
    }

}
