package com.elastisys.scale.cloudpool.aws.commons.predicates;

import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.instanceStateIn;
import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.instancesPresent;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jersey.repackaged.com.google.common.collect.Lists;

import org.junit.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises the {@link InstancePredicates} class.
 */
public class TestInstancePredicates {

	@Test
	public void testHasTag() {
		Predicate<Instance> hasTag = InstancePredicates.hasTag(new Tag()
				.withKey("expectedKey").withValue("expectedValue"));

		Map<String, String> noTags = ImmutableMap.of();
		Instance i0 = makeInstance("i-0", "running", noTags);
		Instance i1 = makeInstance("i-1", "running",
				ImmutableMap.of("k1", "v1"));
		Instance i2 = makeInstance("i-2", "running",
				ImmutableMap.of("expectedKey", "expectedValue"));
		Instance i3 = makeInstance("i-3", "running",
				ImmutableMap.of("k1", "v1", //
						"expectedKey", "expectedValue"));

		assertFalse(hasTag.apply(i0));
		assertFalse(hasTag.apply(i1));
		assertTrue(hasTag.apply(i2));
		assertTrue(hasTag.apply(i3));
	}

	@Test
	public void testInstanceStateIn() {
		List<String> empty = Arrays.asList();
		Predicate<Instance> noAcceptableStates = instanceStateIn(empty);
		Predicate<Instance> pendingOnly = instanceStateIn(asList("pending"));
		Predicate<Instance> active = instanceStateIn(asList("pending",
				"running"));
		Predicate<Instance> terminal = instanceStateIn(asList("terminating",
				"terminated"));

		Map<String, String> noTags = ImmutableMap.of();
		Instance pending = makeInstance("i-0", "pending", noTags);
		Instance running = makeInstance("i-1", "running", noTags);
		Instance stopping = makeInstance("i-2", "stopping", noTags);
		Instance terminating = makeInstance("i-2", "terminating", noTags);
		Instance terminated = makeInstance("i-3", "terminated", noTags);

		assertFalse(noAcceptableStates.apply(pending));
		assertFalse(noAcceptableStates.apply(running));
		assertFalse(noAcceptableStates.apply(stopping));
		assertFalse(noAcceptableStates.apply(terminating));
		assertFalse(noAcceptableStates.apply(terminated));

		assertTrue(pendingOnly.apply(pending));
		assertFalse(pendingOnly.apply(running));
		assertFalse(pendingOnly.apply(stopping));
		assertFalse(pendingOnly.apply(terminating));
		assertFalse(pendingOnly.apply(terminated));

		assertTrue(active.apply(pending));
		assertTrue(active.apply(running));
		assertFalse(active.apply(stopping));
		assertFalse(active.apply(terminating));
		assertFalse(active.apply(terminated));

		assertFalse(terminal.apply(pending));
		assertFalse(terminal.apply(running));
		assertFalse(terminal.apply(stopping));
		assertTrue(terminal.apply(terminating));
		assertTrue(terminal.apply(terminated));
	}

	@Test
	public void testInstancesPresent() {

		Map<String, String> noTags = ImmutableMap.of();
		Instance i0 = makeInstance("i-0", "pending", noTags);
		Instance i1 = makeInstance("i-1", "running", noTags);
		Instance i2 = makeInstance("i-2", "running", noTags);
		Instance i3 = makeInstance("i-3", "terminating", noTags);

		List<Instance> noInstances = Arrays.asList();

		List<String> expectedIds = Arrays.asList();
		assertTrue(instancesPresent(expectedIds).apply(noInstances));
		expectedIds = asList("i-0");
		assertFalse(instancesPresent(expectedIds).apply(noInstances));
		assertFalse(instancesPresent(expectedIds).apply(asList(i1)));
		assertTrue(instancesPresent(expectedIds).apply(asList(i0)));
		assertTrue(instancesPresent(expectedIds).apply(asList(i0, i1)));
		assertFalse(instancesPresent(expectedIds).apply(asList(i1, i2)));

		expectedIds = asList("i-0", "i-3");
		assertFalse(instancesPresent(expectedIds).apply(noInstances));
		assertFalse(instancesPresent(expectedIds).apply(asList(i1)));
		assertFalse(instancesPresent(expectedIds).apply(asList(i0)));
		assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1)));
		assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1)));
		assertTrue(instancesPresent(expectedIds).apply(asList(i0, i3)));
		assertFalse(instancesPresent(expectedIds).apply(asList(i0, i1, i2)));
		assertTrue(instancesPresent(expectedIds).apply(asList(i0, i1, i2, i3)));

	}

	private Instance makeInstance(String id, String state,
			Map<String, String> tags) {
		List<Tag> tagList = Lists.newArrayList();
		for (Entry<String, String> tagEntry : tags.entrySet()) {
			tagList.add(new Tag().withKey(tagEntry.getKey()).withValue(
					tagEntry.getValue()));
		}

		return new Instance().withInstanceId(id)
				.withState(new InstanceState().withName(state))
				.withTags(tagList);
	}
}
