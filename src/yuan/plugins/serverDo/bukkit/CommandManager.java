/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import yuan.plugins.serverDo.util.command.AbstractCommand;
import yuan.plugins.serverDo.util.command.CheckSenderCommand;
import yuan.plugins.serverDo.util.command.FilterCommand;
import yuan.plugins.serverDo.util.command.RootCommand;
import yuan.plugins.serverDo.util.command.SwitchCommand;

/**
 * 命令管理器<br>
 * 包含了工具版本, 命令方法<br>
 * 命令请在 {@link #init()}方法中实现
 * 
 * @version 1.1.2<br>
 *          number: 4
 * @author yuanlu
 *
 */
@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class CommandManager implements MESSAGE {

	/**
	 * 版本
	 */
	public static final String VERSION        = "1.2.0";

	/**
	 * 版本号<br>
	 * 1: 完成初代<br>
	 * 2: 优化 {@link SwitchCommand} tab提示, 完善用户体验 <br>
	 * 3: 修正 {@link CheckSenderCommand}的提示信息BUG, 添加日志doc<br>
	 * 4: 修改 {@link FilterCommand}(及其子类) 处理结构, 检测tab<br>
	 * 5: 修改消息类, 将 {@link String}改为 {@link yuan.plugins.serverDo.bukkit.MESSAGE.Msg}
	 */
	public static final long   VERSION_NUMBER = 5;

	/**
	 * 从list中挑选合适的文字返回<br>
	 * 不会修改原列表
	 * 
	 * @param prefix   前缀
	 * @param nullList 当前缀内容为空时返回的列表(做默认值用)
	 * @param dataList 数据列表
	 * @return 新列表
	 */
	protected static List<String> get(String prefix, List<String> nullList, List<String> dataList) {
		if (prefix == null || prefix.isEmpty()){
			ArrayList<String> list = new ArrayList<>(dataList.size() + nullList.size());
			list.addAll(nullList);
			list.addAll(dataList);
			return list;
		}
		ArrayList<String> list = new ArrayList<>(dataList.size());
		dataList.forEach((x) -> {
			if (x != null && x.startsWith(prefix)) list.add(x);
		});
		return list;
	}

	/**
	 * 获取版本字符串
	 * 
	 * @return 版本
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * 获取版本号
	 * 
	 * @return 版本号
	 */
	public static long getVersionNumber() {
		return VERSION_NUMBER;
	}

	/**
	 * 初始化命令
	 */
	public static final void init() {
		final String name = "ymc";
		final String description = "这是由模板自动生成的命令, 未进行修改替换";
		final String usageMessage = "这是由模板自动生成的命令, 未进行修改替换";
		final List<String> aliases = Arrays.asList();
		// TODO: 制作你的命令执行器
		HashMap<String, AbstractCommand> m = new HashMap<>();

		m.put("test", new AbstractCommand() {

			@Override
			public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
				sender.sendMessage("This is test command");
				return true;
			}

			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args,
					int argsIndex) {
				return EMPTY_TAB_LIST;
			}
		});

		AbstractCommand COMMAND = new SwitchCommand(CMD_HELP, null, true, m);

		RootCommand root = new RootCommand(name, description, usageMessage, aliases, COMMAND);
		root.register();
		// TODO: 制作你的命令执行器
	}

	/**
	 * 从配置文件获取信息<br>
	 * 简化方法
	 * 
	 * @param node 节点
	 * @return 获取到的字符串
	 * @see #mes(String, int)
	 */
	protected static Msg mes(String node) {
		return mes(node, 0);
	}

	/**
	 * 从配置文件获取信息<br>
	 * 简化方法
	 * 
	 * @param node 节点
	 * @param type 类型
	 * @return 获取到的字符串
	 */
	protected static Msg mes(String node, int type) {
		return Main.getMain().mes(node, type);
	}
}
