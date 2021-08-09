/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.util.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * 使用lambda表达式执行的命令执行器<br>
 * 本意为简化构建命令时简化代码<br>
 * 但因Eclipse的代码补全不支持lambda表达式的补全, 所以并没有使用
 * 
 * @author yuanlu
 * @deprecated 不实用
 */
@Deprecated
public final class LambdaCommand extends AbstractCommand {

	/**
	 * 执行命令的接口<br>
	 * 可使用lambda
	 *
	 * @author yuanlu
	 *
	 */
	@FunctionalInterface
	public static interface OnCommand {

		/**
		 * 执行命令
		 * 
		 * @param sender    命令执行的来源
		 * @param cmd       被执行的命令
		 * @param label     使用的命令别名
		 * @param args      传递的命令参数
		 * @param argsIndex 命令参数读取起点位置<br>
		 *                  根命令从0开始
		 * @return 是否成功执行
		 */
		boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, int argsIndex);
	}

	/**
	 * 执行tab的接口<br>
	 * 可使用lambda
	 *
	 * @author yuanlu
	 *
	 */
	@FunctionalInterface
	public static interface OnTabComplete {

		/**
		 * 执行命令
		 * 
		 * @param sender    命令执行的来源
		 * @param command   被执行的命令
		 * @param label     使用的命令别名
		 * @param args      传递的命令参数
		 * @param argsIndex 命令参数读取起点位置<br>
		 *                  根命令从0开始
		 * @return 是否成功执行
		 */
		List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args, int argsIndex);
	}

	/**
	 * command
	 */
	private final OnCommand     command;

	/**
	 * tab
	 */
	private final OnTabComplete tab;

	/**
	 * 构造一个使用lambda表达式的命令执行器
	 * 
	 * @param command 执行命令方法
	 * @param tab     执行tab方法
	 */
	public LambdaCommand(OnCommand command, OnTabComplete tab) {
		if (command == null) throw new NullPointerException("command can not be null.");
		if (tab == null) throw new NullPointerException("tab can not be null.");
		this.command = command;
		this.tab = tab;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		return command.onCommand(sender, cmd, label, args, argsIndex);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args,
			int argsIndex) {
		return tab.onTabComplete(sender, command, label, args, argsIndex);
	}
}
