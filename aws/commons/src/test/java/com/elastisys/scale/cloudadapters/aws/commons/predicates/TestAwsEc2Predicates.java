package com.elastisys.scale.cloudadapters.aws.commons.predicates;

import static com.elastisys.scale.cloudadapters.aws.commons.predicates.AwsEc2Predicates.ec2InstanceIdEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.google.common.base.Predicate;

/**
 * Verifies the behavior of the {@link Predicate}s in {@link AwsEc2Predicates}.
 *
 * 
 *
 */
public class TestAwsEc2Predicates {

	@Test
	public void testEc2InstanceIdEquals() {
		// test on illegal arguments
		assertThat(ec2InstanceIdEquals("i-123456").apply(null), is(false));
		assertThat(ec2InstanceIdEquals("i-123456").apply(new Instance()),
				is(false));

		// test on valid arguments
		Instance instance = new Instance().withInstanceId("i-123456");
		assertThat(ec2InstanceIdEquals("i-123456").apply(instance), is(true));
		assertThat(ec2InstanceIdEquals("i-654321").apply(instance), is(false));
	}

	@Test(expected = NullPointerException.class)
	public void testEc2InstanceIdEqualsWithBadInput() {
		AwsEc2Predicates.ec2InstanceIdEquals(null);
	}

}
