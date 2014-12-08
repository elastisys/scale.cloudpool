package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestErrorResponseMessage {

	@Test
	public void testParameters() {
		final String message = "message";
		final String detail = "detail";
		ErrorResponseMessage a = new ErrorResponseMessage(message, detail);

		assertEquals(message, a.getMessage());
		assertEquals(detail, a.getDetail());
	}

	@Test(expected = NullPointerException.class)
	public void testParametersMissingMessage() {
		new ErrorResponseMessage(null, "detail");
	}

	@Test
	public void testParametersMissingDetail() {
		new ErrorResponseMessage("message", null);
	}

	@Test
	public void testEquality() {
		ErrorResponseMessage a = new ErrorResponseMessage("message1", "detail1");
		ErrorResponseMessage b = new ErrorResponseMessage("message1", "detail1");
		ErrorResponseMessage c = new ErrorResponseMessage("message2", "detail1");
		ErrorResponseMessage d = new ErrorResponseMessage("message2", "detail2");
		ErrorResponseMessage e = new ErrorResponseMessage("message1", "detail2");

		assertEquals(a, a);
		assertEquals(a, b);
		assertFalse(a.equals(c));
		assertFalse(a.equals(d));
		assertFalse(a.equals(e));

		assertTrue(a.hashCode() == b.hashCode());

		assertFalse(a.equals(null));
	}

	@Test
	public void testEqualsWithUnrelatedObject() {
		ErrorResponseMessage a = new ErrorResponseMessage("message1", "detail1");
		assertFalse(a.equals("fail"));
	}

}
