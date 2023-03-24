package cn.yuchuxi.php;

import cn.nukkit.Server;
import cn.nukkit.command.RemoteConsoleCommandSender;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunCommand {
	public static void runWorldCommand(String command, @NotNull Permission permission, CallBack callBack) {
		if (permission.canRunWorldCommand()) {
			CommandRunner.WorldCommandRunner(command, callBack);
			return;
		}
		callBack.run(false, "? permission", "denied");
	}
	
	public static void runJShellCommand(String command, @NotNull Permission permission, CallBack callBack) {
		if (permission.canRunJShellCommand()) {
			CommandRunner.JShellRunner(command, callBack);
			return;
		}
		callBack.run(false, "? permission", "denied");
	}
	
	public static void runShell(String shell, @NotNull Permission permission, CallBack callBack) {
		if (permission.canRunHostShellCommand()) {
			CommandRunner.HostShellRunner(shell, callBack);
			return;
		}
		callBack.run(false, "? permission", "denied");
	}
	
	public static class Permission {
		private final boolean canRunWorldCommand;
		private final boolean canRunJShellCommand;
		private final boolean canRunHostShellCommand;
		
		public Permission(long user) {
			Config config = PHPServer.getInstance().getConfig();
			canRunWorldCommand = config.getLongList("bot.user.admin").contains(user);
			canRunJShellCommand = config.getLong("bot.user.root") == user;
			canRunHostShellCommand = config.getLong("bot.user.root") == user;
		}
		
		public boolean canRunWorldCommand() {
			return canRunWorldCommand;
		}
		
		public boolean canRunJShellCommand() {
			return canRunJShellCommand;
		}
		
		public boolean canRunHostShellCommand() {
			return canRunHostShellCommand;
		}
		
	}
}

class CommandRunner {
	static private final Pattern timeoutMatcher = Pattern.compile("^((--timeout|-t)\\s(\\d+?)\\s)?(.+)$"); // 匹配命令超时参数
	
	static public void WorldCommandRunner(String command, CallBack callBack) {
		RemoteConsoleCommandSender commandSender = new RemoteConsoleCommandSender();
		Server server = Server.getInstance();
		server.getScheduler().scheduleTask(new Task() {
			@Override
			public void onRun(int i) {
				server.executeCommand(commandSender, command);
				callBack.run(true, "/", TextFormat.clean(commandSender.getMessages()));
			}
		});
	}
	
	static public void JShellRunner(String command, CallBack jsh) {
            /*
            callBack.run(true, "/", commandSender.getMessages());

             */
	}
	
	static public void HostShellRunner(String shell, CallBack callBack) {
		Matcher m = timeoutMatcher.matcher(shell);
		int timeout = 10;
		if (m.find() && (m.group(3) != null)) {
			timeout = Integer.parseInt(m.group(3));
			shell = m.group(4);
		}
		try {
			//System.out.println(shell);
			//System.out.println(shell.length());
			Process exec = Runtime.getRuntime().exec(shell);
			//                                   ^ to ProcessBuilder !!!!!!!!
			if (exec.waitFor(timeout, TimeUnit.SECONDS)) {
				callBack.run(true, "$", new String(exec.getInputStream().readAllBytes()));
			} else {
				callBack.run(false, "$ error", "Timeout");
				exec.destroy();
			}
		} catch (IOException | InterruptedException | ArrayIndexOutOfBoundsException e) {
			callBack.run(false, "$ error", e.getMessage());
		}
	}
}

