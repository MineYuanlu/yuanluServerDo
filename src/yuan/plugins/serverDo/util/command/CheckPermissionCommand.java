/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.util.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * 权限检查命令执行器<br>
 * 本类是 {@link FilterCommand}的一个实现<br>
 * 本方法会在
 * 
 * @author yuanlu
 *
 */
public class CheckPermissionCommand extends FilterCommand {

	/**
	 * 权限<br>
	 * 命令发送者需要拥有此权限才可不被过滤掉
	 */
	protected final String permission;

	/**
	 * 没有权限时提示的信息(默认)
	 */
	protected final Msg    MES_NO_PERMISSION;

	/**
	 * 构造一个权限检查器
	 * 
	 * @param next             被套接的命令执行
	 * @param permission       权限
	 * @param mes_noPermission 没有权限时提示的信息<br>
	 *                         在构造时将会把%permission%替换为权限字符串
	 */

	public CheckPermissionCommand(AbstractCommand next, String permission, Msg mes_noPermission) {
		super(next);
		if (permission == null) throw new NullPointerException("permission can not be null.");
		if (mes_noPermission == null) throw new NullPointerException("mes_noPermission can not be null.");
		this.permission = permission;
		MES_NO_PERMISSION = mes_noPermission.replace("%permission%", permission);
	}

	@Override
	protected final boolean check(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		return sender.hasPermission(permission);
	}

	/**
	 * 当命令执行的来源没有预设的权限时调用此方法<br>
	 * 若没有继承本类并重写本方法, 则将会把预设的{@link #MES_NO_PERMISSION}发给命令执行的来源 发送的信息没有任何替换
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 */
	@Override
	protected void onNoPermissionToRunCommand(CommandSender sender, Command cmd, String label, String[] args,
			int argsIndex) {
		MES_NO_PERMISSION.send(sender);
	}
}
