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
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Core.CallbackQueue;
import yuan.plugins.serverDo.bukkit.MESSAGE;
import yuan.plugins.serverDo.bukkit.Main;
import yuan.plugins.serverDo.bukkit.cmds.CommandManager.CmdInfo;

/**
 * Cmd
 * 
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
		list.forEach(x -> {
			if (x != null && x.startsWith(prefix)) list.add(x);
		});
		return list;
	}

	/**
	 * 获取cmd名称
	 * 
	 * @param cmd 命令
	 * @return 命令名称
	 */
	public static final String getCmdName(Class<? extends Cmd> cmd) {
		String cname = cmd.getSimpleName();
		if (cname.length() >= 3 && cname.substring(0, 3).equalsIgnoreCase("cmd")) cname = cname.substring(3);
		cname = Tool.humpTrans(cname, "-");
		return cname;
	}

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param cmd  命令
	 * @param type 消息类型
	 * @return 消息
	 */
	protected static final Msg msg(Class<? extends Cmd> cmd, String type) {
		val	key	= getCmdName(cmd) + "." + type;
		Msg	msg	= MSGS.get(key);
		if (msg != null) return msg;
		MSGS.put(key, msg = Main.getMain().mes("cmd." + key));
		return msg;
	}

	/** 此命令名称 */
	protected final String cmdName;

	/**
	 * 构造一个命令
	 * 
	 * @param name 名称
	 */
	Cmd(String name) {
		super(name);
		val info = getCmdInfo();
		setDescription(info.getDescription());
		setUsage(info.getUsageMessage());
		setPermission(info.getPermission());

		cmdName = getCmdName(getClass());
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
		bstatsExecute(this);
		val cq = new CallbackQueue();
		cq.task(() -> checkPlayer(sender, cq), //
				() -> Core.permissionCheck(sender, getPermission(), cq), //
				() -> execute0(sender, args));
		return true;
	}

	/**
	 * bstats统计: 执行命令
	 * 
	 * @param cmd 命令
	 */
	protected static void bstatsExecute(Cmd cmd) {
		EXECUTE_COUNT.computeIfAbsent(cmd.getClass(), x -> new AtomicInteger()).getAndIncrement();
	}

	/** 执行计数 */
	public static final HashMap<Class<? extends Cmd>, AtomicInteger> EXECUTE_COUNT = new HashMap<>();

	/**
	 * 实际执行
	 * 
	 * @param sender Source object which is executing this command
	 * @param args   All arguments passed to the command, split via ' '
	 * @return true if the command was successful, otherwise false
	 */
	protected abstract boolean execute0(CommandSender sender, String[] args);

	/** @return Info */
	protected CmdInfo getCmdInfo() {
		return CommandManager.INFOS.get(getName());
	}

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type 消息类型
	 * @return 消息
	 */
	protected final Msg msg(String type) {
		return msg(type, 0);
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

	/**
	 * 返回此命令的某个消息
	 * 
	 * @param type 消息类型
	 * @param code 消息格式
	 * @return 消息
	 */
	protected final Msg msg(String type, int code) {
		val	key	= cmdName + "." + type;
		Msg	msg	= MSGS.get(key + code);
		if (msg != null) return msg;
		MSGS.put(key + code, msg = Main.getMain().mes("cmd." + key, code));
		return msg;
	}
}
