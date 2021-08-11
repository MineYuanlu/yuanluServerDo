/**
 * yuanlu
 * date: 2021年8月10日
 * file: Cmds.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Core.CallbackQueue;
import yuan.plugins.serverDo.bukkit.MESSAGE;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * @author yuanlu
 *
 */
public abstract class Cmd extends Command implements MESSAGE {
	/** 所有的信息 */
	private static final HashMap<String, Msg> MSGS = new HashMap<>();

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
		if (prefix == null || prefix.isEmpty()) {
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

	/** 此命令名称 */
	protected final String cmdName;

	/**
	 * 构造一个命令
	 * 
	 * @param name 名称
	 */
	protected Cmd(String name) {
		super(name);
		val info = CommandManager.INFOS.get(name);
		setDescription(info.getDescription());
		setUsage(info.getUsageMessage());
		setPermission(info.getPermission());

		String cname = getClass().getSimpleName().toLowerCase();
		if (cname.startsWith("cmd")) cname = cname.substring(3);
		cmdName = cname;
	}

	/**
	 * 检查CommandSender状态
	 * 
	 * @param sender CommandSender
	 * @param next   回调
	 */
	protected void checkPlayer(CommandSender sender, Runnable next) {
		if (sender instanceof Player) next.run();
		else NOT_PLAYER.send(sender);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		val cq = new CallbackQueue();
		cq.task(() -> checkPlayer(sender, cq), //
				() -> Core.permissionCheck(sender, getPermission(), cq), //
				() -> execute0(sender, args));
		return true;
	}

	/**
	 * 实际执行
	 * 
	 * @param sender Source object which is executing this command
	 * @param args   All arguments passed to the command, split via ' '
	 * @return true if the command was successful, otherwise false
	 */
	protected abstract boolean execute0(CommandSender sender, String[] args);

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type 消息类型
	 * @return 消息
	 */
	protected final Msg msg(String type) {
		val	key	= cmdName + "." + type;
		Msg	msg	= MSGS.get(key);
		if (msg != null) return msg;
		MSGS.put(key, msg = Main.getMain().mes("cmd." + key));
		return msg;
	}

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type   消息类型
	 * @param sender 目标
	 * @return true
	 */
	protected final boolean msg(String type, CommandSender sender) {
		msg(type).send(sender);
		return true;
	}

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type   消息类型
	 * @param sender 目标
	 * @param args   参数
	 * @return true
	 */
	protected final boolean msg(String type, CommandSender sender, Map<String, Object> args) {
		msg(type).send(sender, args);
		return true;
	}

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type   消息类型
	 * @param sender 目标
	 * @param args   参数
	 * @return true
	 */
	protected final boolean msg(String type, CommandSender sender, Object... args) {
		msg(type).send(sender, args);
		return true;
	}
}