/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.util.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * 类switch-case的命令执行器<br>
 * 树形命令执行器的核心<br>
 * 通过预设的命令情况, 选择子节点执行<br>
 * 本质为HashMap的查找
 * 
 * @author yuanlu
 *
 */
public class SwitchCommand extends AbstractCommand {

	/**
	 * 缺少需要的命令参数时提示信息<br>
	 * 即提供的命令参数长度未达到判断位置
	 */
	public final Msg                               MES_NO_ARGS;

	/**
	 * 输入的参数没有匹配时提示信息
	 */
	public final Msg                               MES_NO_ARGS_CASE;

	/**
	 * 是否将输入的命令参数转换为小写进行匹配<br>
	 * 解决大小写不一致问题
	 */
	public final boolean                           TO_LOWER_CASE;

	/**
	 * 所有预设的情况集合图<br>
	 * 预设的检测字符串 - 命令执行器
	 */
	private final HashMap<String, AbstractCommand> CASES;

	/**
	 * 所有预设的检测字符串列表<br>
	 * 用于tab响应
	 */
	private final ArrayList<String>                TABS;

	/**
	 * 构建一个选择命令执行器
	 * 
	 * @param mes_noArgs     见: {@link #MES_NO_ARGS}
	 * @param mes_noArgsCase 见: {@link #MES_NO_ARGS_CASE} 若为null则指向
	 *                       {@link #MES_NO_ARGS}
	 * @param toLowerCase    见: {@link #TO_LOWER_CASE}
	 * @param cases          见: {@link #CASES}
	 */
	public SwitchCommand(Msg mes_noArgs, Msg mes_noArgsCase, boolean toLowerCase, Map<String, AbstractCommand> cases) {
		if (mes_noArgs == null) throw new NullPointerException("mes_noArgs can not be null.");
		if (mes_noArgsCase == null) mes_noArgsCase = mes_noArgs;
		if (cases == null) throw new NullPointerException("cases can not be null.");
		MES_NO_ARGS = mes_noArgs;
		MES_NO_ARGS_CASE = mes_noArgsCase;
		TO_LOWER_CASE = toLowerCase;
		CASES = new HashMap<>(cases);
		TABS = new ArrayList<>(CASES.keySet());
	}

	/**
	 * 当没有在{@link #CASES}中找到下一步操作时调用本方法<br>
	 * 若没有继承本类并重写本方法, 则将会把预设的{@link #MES_NO_ARGS_CASE}发给命令执行的来源<br>
	 * 发送的信息将会进行如下的替换:<br>
	 * <br>
	 * %args% {@code -> }被检测的参数(即args[argsIndex])
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 * @return 向上传递测命令执行状态<br>
	 *         此值将直接传给上层命令执行器, 而不会在本方法后执行其他语句
	 */
	protected boolean nextNullCase(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		MES_NO_ARGS_CASE.send(sender, args[argsIndex]);
		return true;
	}

	/**
	 * 当用户输入的命令参数长度没有达到给定的(argsIndex)判断位置时调用本方法<br>
	 * 若没有继承本类并重写本方法, 则将会把预设的{@link #MES_NO_ARGS}发给命令执行的来源<br>
	 * 发送的信息没有任何替换, 推荐是上层命令的帮助信息
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 * @return 向上传递测命令执行状态<br>
	 *         此值将直接传给上层命令执行器, 而不会在本方法后执行其他语句
	 */
	protected boolean noArgsCase(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		MES_NO_ARGS.send(sender);
		return true;
	}

	@Override
	public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		if (args.length > argsIndex){
			AbstractCommand next = CASES.get(TO_LOWER_CASE ? args[argsIndex].toLowerCase() : args[argsIndex]);
			if (next == null) return nextNullCase(sender, cmd, label, args, argsIndex);
			return next.onCommand(sender, cmd, label, args, argsIndex + 1);
		} else{
			return noArgsCase(sender, cmd, label, args, argsIndex);
		}
	}

	/**
	 * 用命令传递的参数请求可能的补全项的list.<br>
	 * 如果请求位置为本操作器处理位置, 则返回{@link #TABS}作为补全项list.<br>
	 * 如果请求位置超过本操作器处理位置, 则尝试寻找下一步操作器<br>
	 * 若寻找到则将参数传递给下一层操作器, 若未找到则返回{@link AbstractCommand#EMPTY_TAB_LIST}
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args,
			int argsIndex) {
		if (args.length > argsIndex + 1){
			AbstractCommand next = CASES.get(TO_LOWER_CASE ? args[argsIndex].toLowerCase() : args[argsIndex]);
			if (next != null) return next.onTabComplete(sender, command, label, args, argsIndex + 1);
			return EMPTY_TAB_LIST;
		} else{
			String s = args[argsIndex];
			ArrayList<String> tab = new ArrayList<>(TABS);
			tab.removeIf((x) -> !x.startsWith(s));
			return tab;
		}
	}
}
