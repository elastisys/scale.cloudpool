package com.elastisys.scale.cloudpool.commons.util.cli;

import static com.elastisys.scale.commons.json.JsonUtils.toPrettyString;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * A utility class that can be used to exercise a given {@link CloudPool} from
 * the command-line. When {@link #start()} is called, the program will start to
 * read commands from {@code stdin} and forward them to the appropriate methods
 * on the {@link CloudPool}.
 */
public class CloudPoolCommandLineDriver {

	/** The {@link CloudPool} to exercise. */
	private final CloudPool cloudPool;

	/**
	 * Creates a {@link CloudPoolCommandLineDriver} that will exercise a given
	 * {@link CloudPool} instance with user commands read from {@code stdin}.
	 *
	 * @param cloudPool
	 *            The {@link CloudPool} to exercise.
	 */
	public CloudPoolCommandLineDriver(CloudPool cloudPool) {
		this.cloudPool = cloudPool;
	}

	/**
	 * Prompts the user for input, displaying the available commands.
	 */
	private void prompt() {
		List<String> commands = asList(
				//
				"config                                          -- get config",
				"setconfig <path>                                -- set config",
				"size                                            -- get pool size",
				"setsize <num>                                   -- set desired size",
				"pool [verbose?]                                 -- get pool members",
				"attach <id>                                     -- attach vm to pool",
				"detach <id> <size--?>                           -- detach vm from pool",
				"setmemberstatus <id> <active?> <evictable?>     -- set vm membership status",
				"setstate <id> <state>                           -- set vm service state",
				"terminate <id> <size--?>                        -- terminate a pool member",
				"exit                                            -- quit");
		System.err.println("Commands:");
		System.err.println(Joiner.on("\n").join(commands));
		System.err.print(">> ");
	}

	/**
	 * Starts the main command input loop.
	 */
	public void start() {
		prompt();
		try (Scanner scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.isEmpty()) {
					continue;
				}
				List<String> tokens = Arrays.asList(line.split("\\s+"));
				String command = tokens.get(0);
				List<String> args = tokens.subList(1, tokens.size());
				try {
					runCommand(command, args);
				} catch (Exception e) {
					System.err.printf("failed to execute '%s':\n%s\n", command,
							e);
					e.printStackTrace();
				}
				prompt();
			}
		}
	}

	/**
	 * Runs the given command against the {@link CloudPool}.
	 *
	 * @param command
	 * @param args
	 */
	private void runCommand(String command, List<String> args) {
		switch (command) {
		case "config": {
			System.out.println(toPrettyString(this.cloudPool.getConfiguration()
					.get()));
			break;
		}
		case "setconfig": {
			checkArgument(args.size() > 0,
					"error: setconfig requires an argument");
			File configFile = new File(args.get(0));
			checkArgument(configFile.isFile(),
					"error: given path is not a file");
			this.cloudPool.configure(JsonUtils.parseJsonFile(configFile)
					.getAsJsonObject());
			break;
		}
		case "size": {
			System.out.println(this.cloudPool.getPoolSize());
			break;
		}
		case "setsize": {
			checkArgument(args.size() > 0,
					"error: setsize requires an argument");
			this.cloudPool.setDesiredSize(Integer.valueOf(args.get(0)));
			break;
		}
		case "pool": {
			boolean verbose = !args.isEmpty() && Boolean.valueOf(args.get(0));
			List<Machine> machines = this.cloudPool.getMachinePool()
					.getMachines();
			if (verbose) {
				System.out.println(Joiner.on("\n").join(machines));
			} else {
				// exclude metadata
				System.out.println(Joiner.on("\n").join(
						Lists.transform(machines, Machine.toShortString())));
			}
			break;
		}
		case "attach": {
			checkArgument(args.size() > 0, "error: attach requires an argument");
			this.cloudPool.attachMachine(args.get(0));
			break;
		}
		case "detach": {
			checkArgument(args.size() > 1,
					"error: attach requires two arguments: "
							+ "<instance-id> <size--?: true|false>");
			this.cloudPool.detachMachine(args.get(0),
					Boolean.valueOf(args.get(1)));
			break;
		}
		case "setmemberstatus": {
			checkArgument(args.size() > 2,
					"error: setmemberstatus requires three arguments: "
							+ "<instance-id> <active?> <evictable?>");
			this.cloudPool.setMembershipStatus(
					args.get(0),
					new MembershipStatus(Boolean.valueOf(args.get(1)), Boolean
							.valueOf(args.get(2))));
			break;
		}
		case "setstate": {
			checkArgument(args.size() > 1,
					"error: setstate requires two arguments: "
							+ "<instance-id> <state>");
			this.cloudPool.setServiceState(args.get(0),
					ServiceState.valueOf(args.get(1)));
			break;
		}
		case "terminate": {
			checkArgument(args.size() > 1,
					"error: terminate requires two arguments: "
							+ "<instance-id> <size--? true|false>");
			this.cloudPool.terminateMachine(args.get(0),
					Boolean.valueOf(args.get(1)));
			break;
		}
		case "exit": {
			System.out.println("Exiting ...");
			System.exit(0);
			break;
		}
		default: {
			System.err.println(String.format(
					"error: unrecognized command '%s'", command));
		}
		}
	}
}
