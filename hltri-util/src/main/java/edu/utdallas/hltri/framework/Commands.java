package edu.utdallas.hltri.framework;

import edu.utdallas.hltri.util.Unsafe;
import edu.utdallas.hltri.util.Unsafe.CheckedRunnable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import picocli.CommandLine;

public class Commands {

  public static void run(Runnable command, String... args) {
    final CommandLine commandLine = new CommandLine(command);
    parseCommand(commandLine, args);
    command.run();
  }

  protected static void parseCommand(CommandLine commandLine, String[] args) {
    commandLine.registerConverter(Path.class, Paths::get);
    try {
      commandLine.parse(args);
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      commandLine.usage(System.err);
      System.exit(1);
    }
  }

  public static void run(CheckedRunnable command, String... args) throws Exception {
    final CommandLine commandLine = new CommandLine(command);
    parseCommand(commandLine, args);
    command.run();
  }

  public static <A> A createArguments(Supplier<A> argsConstructor, String[] args) {
    A arguments;
    try {
      arguments = argsConstructor.get();
      final CommandLine commandLine = new CommandLine(arguments)
          .registerConverter(Path.class, Paths::get);
      commandLine.parse(args);
      return arguments;
    } catch (Exception e) {
      CommandLine.usage(argsConstructor.get(), System.err);
      throw new RuntimeException(e);
    }
  }
}
