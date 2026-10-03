package de.optools.commands;

/**
 * A client-side alias. {@code command} is sent to the server (without leading slash); arguments typed after the
 * alias are appended, or inserted at {@code {args}} if present.
 */
public final class CommandShortcut {
	public String alias;
	public String command;
	public String description = "";
	public boolean enabled = true;

	public CommandShortcut() {
	}

	public CommandShortcut(String alias, String command, String description) {
		this.alias = alias;
		this.command = command;
		this.description = description;
	}

	public String resolve(String args) {
		String cmd = command.startsWith("/") ? command.substring(1) : command;
		String a = args == null ? "" : args.strip();
		if (cmd.contains("{args}")) return cmd.replace("{args}", a).strip();
		return a.isEmpty() ? cmd : cmd + " " + a;
	}
}
