/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.util.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import lombok.NonNull;

/**
 * 过滤命令执行器<br>
 * 用于筛选掉一些不合适的命令
 * 
 * @author yuanlu
 *
 */
public abstract class FilterCommand extends AbstractCommand {

	/**
	 * 被套接的命令执行器<br>
	 * 下一步执行的命令
	 */
	protected final @NonNull AbstractCommand next;

	/**
	 * 构造一个过滤命令执行器
	 * 
	 * @param next 被套接的命令执行
	 */
	public FilterCommand(@NonNull AbstractCommand next) {
		this.next = next;
	}

	/**
	 * 检测参数是否合格<br>
	 * 过滤掉不合适的参数<br>
	 * <b>如果无特殊需要, 请不要修改参数(约定)</b>
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 * @return 是否成功执行<br>
	 *         即 是否可以继续执行<br>
	 *         若返回false则直接中断命令执行, 需自行设置信息提示
	 */
	protected abstract boolean check(CommandSender sender, Command cmd, String label, String[] args, int argsIndex);

	/**
	 * 执行命令<br>
	 * 若执行的命令通过过滤则将参数传递给{@link #next}执行器(参数可能会被check修改)<br>
	 * 若未通过过滤, 则在执行
	 * {@link #onNoPermissionToRunCommand(CommandSender, Command, String, String[], int)}后返回true
	 * 
	 */
	@Override
	public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		if (check(sender, cmd, label, args, argsIndex))
			return next.onCommand(sender, cmd, label, args, argsIndex);
		onNoPermissionToRunCommand(sender, cmd, label, args, argsIndex);
		return true;
	}

	/**
	 * 当用户执行命令是 <b>如果无特殊需要, 请不要修改参数(约定)</b>
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 */
	protected abstract void onNoPermissionToRunCommand(CommandSender sender, Command cmd, String label, String[] args,
			int argsIndex);

	/**
	 * 用命令传递的参数请求可能的补全项的list.<br>
	 * 直接传递给{@link #next}执行器, 不做任何补全修改
	 */
	@Override
	public final List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args,
			int argsIndex) {
		if (check(sender, command, label, args, argsIndex))
			return next.onTabComplete(sender, command, label, args, argsIndex);
		return EMPTY_TAB_LIST;
	}
}
