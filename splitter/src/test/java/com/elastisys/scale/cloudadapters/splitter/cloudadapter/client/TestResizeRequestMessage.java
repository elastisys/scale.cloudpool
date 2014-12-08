package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TestResizeRequestMessage {

	@Test(expected = IllegalArgumentException.class)
	public void testParameters() {
		new ResizeRequestMessage(0);

		new ResizeRequestMessage(1);

		new ResizeRequestMessage(Long.MAX_VALUE);

		new ResizeRequestMessage(-1); // should throw
	}

	@Test
	public void testEquality() {
		ResizeRequestMessage a = new ResizeRequestMessage(1);
		ResizeRequestMessage b = new ResizeRequestMessage(1);
		ResizeRequestMessage c = new ResizeRequestMessage(2);
		assertEquals(a, b);
		assertFalse(a.equals(c));
	}

	@Test
	public void testHashcode() {
		ResizeRequestMessage a = new ResizeRequestMessage(1);
		ResizeRequestMessage b = new ResizeRequestMessage(1);
		ResizeRequestMessage c = new ResizeRequestMessage(2);

		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.hashCode() == c.hashCode());
	}

	@Test
	public void testEqualsWithUnrelatedObject() {
		ResizeRequestMessage a = new ResizeRequestMessage(0);
		assertFalse(a.equals("fail"));
		assertFalse(a.equals(null));
	}

}
