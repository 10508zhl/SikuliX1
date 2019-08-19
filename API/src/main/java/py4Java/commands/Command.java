/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */
package py4Java.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import py4Java.Gateway;
import py4Java.GatewayConnection;
import py4Java.Py4JException;
import py4Java.Py4JServerConnection;

/**
 * <p>
 * This interface must be implemented by all commands.
 * </p>
 * <p>
 * Typically, each command will define a public constant field that contains the
 * command name, i.e., a String that uniquely identifies the command.
 * </p>
 *
 * <p>
 * The command name can be a String of any length, but it must not contain an
 * end of line character and it cannot be the End of Command character,
 * {@link py4Java.Protocol#END}.
 * </p>
 *
 * <p>
 * There is a command instance per {@link GatewayConnection}: this ensures that
 * each command instance is accessed by only one thread/connection at a time.
 * </p>
 *
 * @author barthelemy
 *
 */
public interface Command {

	/**
	 *
	 * @param commandName
	 *            The command name that was extracted of the command.
	 * @param reader
	 *            The reader from which to read the command parts. Each command
	 *            part are expected to be on a separate line and readable
	 *            through {@link BufferedReader#readLine()}.
	 * @param writer
	 *            The writer to which the return value should be written.
	 * @throws Py4JException
	 *             If an error occurs while executing the command. All
	 *             exceptions except IOException caused by the reader and the
	 *             writer should be wrapper in a {@link Py4JException} instance.
	 * @throws IOException
	 *             If an error occurs while using the reader or the writer.
	 */
	public void execute(String commandName, BufferedReader reader, BufferedWriter writer)
			throws Py4JException, IOException;

	public String getCommandName();

	/**
	 * <p>
	 * Called when a command instance is created and assigned to a connection.
	 * </p>
	 *
	 * @param gateway
	 * @param connection the {@link Py4JServerConnection} this socket is assigned to
	 */
	public void init(Gateway gateway, Py4JServerConnection connection);

}
