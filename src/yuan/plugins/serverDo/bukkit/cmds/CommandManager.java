/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bukkit.MESSAGE;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * 命令管理器
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandManager implements MESSAGE {
	/** 命令信息 */
	@Value
	static final class CmdInfo {
		/**
		 * 获取命令信息
		 * 
		 * @param conf 配置节点
		 * @return 命令信息
		 */
		private static CmdInfo getCmdInfo(ConfigurationSection conf) {
			val	names			= conf.getStringList("names");
			val	permission		= conf.getString("permission", null);
			val	usageMessage	= conf.getString("usageMessage", "");
			val	description		= conf.getString("description", "");
			return new CmdInfo(description, usageMessage, names, permission);
		}

		/** 描述 */
		String			description;
		/** 使用方法 */
		String			usageMessage;
		/** 所有名称 */
		List<String>	names;

		/** 权限 */
		String			permission;
	}

	/**
	 * 所有的命令<br>
	 * {@code 命令名->命令信息}
	 */
	static final HashMap<String, CmdInfo> INFOS = new HashMap<>();

	/**
	 * 初始化所有命令
	 * 
	 * @param conf 配置文件
	 */
	public static final void init(ConfigurationSection conf) {
		if (conf != null) {
			init(conf, "tp", CmdTp.class);
			init(conf, "tpa", CmdTpa.class);
			init(conf, "tphere", CmdTphere.class);
			init(conf, "tpahere", CmdTpahere.class);
			init(conf, "tpaccept", CmdTpaccept.class);
			init(conf, "tpdeny", CmdTpdeny.class);
		}
	}

	/**
	 * 初始化一条命令
	 * 
	 * @param conf 配置文件
	 * @param cmd  命令名
	 * @param c    目标类
	 */
	private static void init(ConfigurationSection conf, String cmd, Class<? extends Cmd> c) {
		try {
			conf = conf.getConfigurationSection(cmd);
			if (conf == null) {
				ShareData.getLogger().warning("未注册命令 " + cmd);
				return;
			}
			val	constructor	= c.getDeclaredConstructor(String.class);
			val	info		= CmdInfo.getCmdInfo(conf);

			for (val cmdName : info.getNames()) {
				INFOS.put(cmdName, info);
				register(constructor.newInstance(cmdName));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 注册命令<br>
	 * 将命令注册到Bukkit上
	 * 
	 * @param cmd 命令
	 */
	public static final void register(Command cmd) {
		try {
			Method		method	= Bukkit.getServer().getClass().getMethod("getCommandMap");
			CommandMap	cmdm	= (CommandMap) method.invoke(Bukkit.getServer());
			val			b		= cmdm.register(Main.getMain().getName(), cmd);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[D] cmdm: " + cmdm);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[D] register cmd: " + b + ", fbn:" + Main.getMain().getName());
		} catch (Exception e2) {
			System.err.println("CAN NOT REGISTER COMMAND: " + e2.toString());
			Main.getMain().getLogger().warning("CAN NOT REGISTER COMMAND: " + e2.toString());
		}
	}
}
