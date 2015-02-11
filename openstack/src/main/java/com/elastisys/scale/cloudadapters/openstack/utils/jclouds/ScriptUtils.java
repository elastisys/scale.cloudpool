package com.elastisys.scale.cloudadapters.openstack.utils.jclouds;

import java.util.Scanner;

import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statements;

public class ScriptUtils {

	private ScriptUtils() {
		throw new IllegalStateException(ScriptUtils.class.getName()
				+ " is not intended to be instantiated.");
	}

	/**
	 * Renders a script of commands to be run on a particular operating system.
	 * <p/>
	 * The passed in script can be given in a pretty "raw form" as the method
	 * takes care of adding a shell declaration line and some basic environment
	 * (such as {@code PATH} variable) declarations to the script.
	 *
	 * @param script
	 *            A (possibly multi-line) script of commands. Each line should
	 *            be separated by a newline character.
	 * @param forOs
	 *            The operating system for which to render the script.
	 * @return The script, rendered for a particular operating system.
	 */
	public static String renderScript(String script, OsFamily forOs) {
		ScriptBuilder builder = new ScriptBuilder();
		Scanner scriptScanner = new Scanner(script);
		while (scriptScanner.hasNextLine()) {
			builder.addStatement(Statements.exec(scriptScanner.nextLine()));
		}
		scriptScanner.close();
		return builder.render(forOs);
	}
}
